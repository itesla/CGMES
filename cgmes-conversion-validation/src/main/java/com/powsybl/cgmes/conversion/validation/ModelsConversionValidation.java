package com.powsybl.cgmes.conversion.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.catalog.CatalogLocation;
import com.powsybl.cgmes.catalog.CatalogReview;
import com.powsybl.cgmes.conversion.validation.ConversionValidationResult.VerificationData;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.cgmes.model.CgmesOnDataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletion;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletionParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.triplestore.api.TripleStore;
import com.powsybl.triplestore.api.TripleStoreException;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class ModelsConversionValidation extends CatalogReview {

    public ModelsConversionValidation(CatalogLocation location) {
        super(location);
    }

    public Map<String, ConversionValidationResult> reviewAll(String pattern, ValidationConfig config)
            throws IOException {
        Map<String, ConversionValidationResult> conversionValidationResult = new HashMap<>();
        reviewAll(pattern, p -> {
            LOG.info("case {}", modelName(p));
            CgmesModelConversion m = cgmesModelConversion(p);
            m.z0Nodes();
            CgmesModelForInterpretation interpretedModel = InterpretationForConversionValidation
                    .getCgmesModelForInterpretation(modelName(p), m);
            List<InterpretationAlternative> mappingConfigs = InterpretationForConversionValidation.getConfigList();

            verificationDataForAllModelMapping = new HashMap<>();

            for (InterpretationAlternative mappingConfig : mappingConfigs) {
                CgmesConversion conversion = new CgmesConversion(m, mappingConfig);
                Network network = conversion.convert();
                VerificationData verificationData = validateModel(interpretedModel, mappingConfig, network, config);
                verificationDataForAllModelMapping.put(mappingConfig, verificationData);
            }

            conversionValidationResult.put(modelName(p), getConversionValidationresult());
        });
        return conversionValidationResult;
    }

    public CgmesModelConversion cgmesModelConversion(Path rpath) {
        String impl = TripleStoreFactory.defaultImplementation();
        if (location.boundary() == null) {
            return createCgmesModel(dataSource(location.dataRoot().resolve(rpath)), impl);
        } else {
            return createCgmesModel(dataSource(location.dataRoot().resolve(rpath)), dataSource(location.boundary()),
                    impl);
        }
    }

    private CgmesModelConversion createCgmesModel(ReadOnlyDataSource ds, String tripleStoreImpl) {
        return createCgmesModel(ds, null, tripleStoreImpl);
    }

    private CgmesModelConversion createCgmesModel(ReadOnlyDataSource ds, ReadOnlyDataSource dsBoundary,
            String tripleStoreImpl) {
        CgmesOnDataSource cds = new CgmesOnDataSource(ds);
        TripleStore tripleStore = TripleStoreFactory.create(tripleStoreImpl);
        CgmesModelConversion cgmes = new CgmesModelConversion(cds.cimNamespace(), tripleStore);
        readCgmesModel(cgmes, cds, cds.baseName());
        // Only try to read boundary data from additional sources if the main data
        // source does not contain boundary info
        if (!cgmes.hasBoundary() && dsBoundary != null) {
            // Read boundary using same baseName of the main data
            readCgmesModel(cgmes, new CgmesOnDataSource(dsBoundary), cds.baseName());
        }
        return cgmes;
    }

    private void readCgmesModel(CgmesModelConversion cgmes, CgmesOnDataSource cds, String base) {
        for (String name : cds.names()) {
            LOG.info("Reading [{}]", name);
            try (InputStream is = cds.dataSource().newInputStream(name)) {
                cgmes.read(base, name, is);
            } catch (IOException e) {
                String msg = String.format("Reading [%s]", name);
                LOG.warn(msg);
                throw new TripleStoreException(msg, e);
            }
        }
    }

    private ConversionValidationResult getConversionValidationresult() {
        ConversionValidationResult r = new ConversionValidationResult();
        r.failedCount = failedCount(verificationDataForAllModelMapping);
        r.verificationDataForAllModelMapping = verificationDataForAllModelMapping;
        return r;
    }

    private int failedCount(Map<InterpretationAlternative, VerificationData> verificationDataForAllModelMapping) {
        return verificationDataForAllModelMapping.values().stream().mapToInt(vd -> vd.failedCount()).sum();
    }

    private VerificationData validateModel(CgmesModelForInterpretation interpretedModel,
            InterpretationAlternative mappingConfig,
            Network network, ValidationConfig config) {
        resetFlows(network);
        computeIidmFlows(network, config.getLoadFlowParameters());

        return ModelConversionValidation.validate(interpretedModel, mappingConfig, network);
    }

    private void computeIidmFlows(Network network, LoadFlowParameters lfparams) {
        LoadFlowResultsCompletionParameters p = new LoadFlowResultsCompletionParameters(
                LoadFlowResultsCompletionParameters.EPSILON_X_DEFAULT,
                LoadFlowResultsCompletionParameters.APPLY_REACTANCE_CORRECTION_DEFAULT,
                LoadFlowResultsCompletionParameters.Z0_THRESHOLD_DIFF_VOLTAGE_ANGLE,
                true);
        LoadFlowResultsCompletion lf = new LoadFlowResultsCompletion(p, lfparams);
        try {
            lf.run(network, null);
        } catch (Exception e) {
            LOG.error("computeFlows, error {}", e.getMessage());
        }
    }

    private void resetFlows(Network network) {
        network.getBranchStream().forEach(b -> {
            b.getTerminal1().setP(Double.NaN);
            b.getTerminal2().setP(Double.NaN);
            b.getTerminal1().setQ(Double.NaN);
            b.getTerminal2().setQ(Double.NaN);
        });
        network.getDanglingLineStream().forEach(d -> {
            d.getTerminal().setP(Double.NaN);
            d.getTerminal().setQ(Double.NaN);
        });
        network.getThreeWindingsTransformerStream().forEach(tx -> {
            tx.getLeg1().getTerminal().setP(Double.NaN);
            tx.getLeg2().getTerminal().setP(Double.NaN);
            tx.getLeg3().getTerminal().setP(Double.NaN);
            tx.getLeg1().getTerminal().setQ(Double.NaN);
            tx.getLeg2().getTerminal().setQ(Double.NaN);
            tx.getLeg3().getTerminal().setQ(Double.NaN);
        });
    }

    private Map<InterpretationAlternative, VerificationData> verificationDataForAllModelMapping;
    private static final Logger                              LOG = LoggerFactory
            .getLogger(ModelsConversionValidation.class);
}
