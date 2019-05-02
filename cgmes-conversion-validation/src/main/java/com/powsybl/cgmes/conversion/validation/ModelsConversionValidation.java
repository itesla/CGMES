package com.powsybl.cgmes.conversion.validation;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.conversion.validation.ConversionValidationResult.VerificationData;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping;
import com.powsybl.cgmes.model.interpretation.InterpretedModel;
import com.powsybl.cgmes.tools.Catalog;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletion;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletionParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class ModelsConversionValidation extends Catalog {

    public ModelsConversionValidation(String sdata) {
        this(sdata, null);
    }

    public ModelsConversionValidation(String sdata, String sboundary) {
        super(sdata);
        boundary = sboundary == null ? null : Paths.get(sboundary);
        exceptions = new HashMap<>();
    }

    public Map<String, ConversionValidationResult> reviewAll(String pattern, ValidationConfig config)
            throws IOException {
        Map<String, ConversionValidationResult> conversionValidationResult = new HashMap<>();
        reviewAll(pattern, p -> {
            LOG.info("case {}", modelName(p));
            CgmesModel m = load(p);
            InterpretedModel interpretedModel = InterpretationForConversionValidation.getInterpretedModel(m);
            List<CgmesEquipmentModelMapping> mappingConfigs = InterpretationForConversionValidation
                    .getConfigList(m);

            verificationDataForAllModelMapping = new HashMap<>();

            for (CgmesEquipmentModelMapping mappingConfig : mappingConfigs) {
                CgmesConversion conversion = new CgmesConversion(m, mappingConfig);
                Network network = conversion.convert();
                VerificationData verificationData = validateModel(interpretedModel, mappingConfig, network, config);
                verificationDataForAllModelMapping.put(mappingConfig, verificationData);
            }

            conversionValidationResult.put(modelName(p), getConversionValidationresult());
        });
        return conversionValidationResult;
    }

    private ConversionValidationResult getConversionValidationresult() {
        ConversionValidationResult r = new ConversionValidationResult();
        r.failedCount = failedCount(verificationDataForAllModelMapping);
        r.verificationDataForAllModelMapping = verificationDataForAllModelMapping;
        return r;
    }

    private int failedCount(Map<CgmesEquipmentModelMapping, VerificationData> verificationDataForAllModelMapping) {
        return verificationDataForAllModelMapping.values().stream().mapToInt(vd -> vd.failedCount()).sum();
    }

    private VerificationData validateModel(InterpretedModel interpretedModel, CgmesEquipmentModelMapping mappingConfig,
            Network network, ValidationConfig config) {
        resetFlows(network);
        computeIidmFlows(network, config.getLoadFlowParameters());

        return ModelConversionValidation.validate(interpretedModel, mappingConfig, network);
    }

    private void computeIidmFlows(Network network, LoadFlowParameters lfparams) {
        LoadFlowResultsCompletionParameters p = new LoadFlowResultsCompletionParameters(
                LoadFlowResultsCompletionParameters.EPSILON_X_DEFAULT,
                LoadFlowResultsCompletionParameters.APPLY_REACTANCE_CORRECTION_DEFAULT,
                LoadFlowResultsCompletionParameters.Z0_THRESHOLD_DIFF_VOLTAGE_ANGLE);
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

    public Map<String, Exception> getExceptions() {
        return exceptions;
    }

    protected CgmesModel load(Path p) {
        String impl = TripleStoreFactory.defaultImplementation();
        if (boundary == null) {
            return CgmesModelFactory.create(dataSource(p), impl);
        } else {
            return CgmesModelFactory.create(dataSource(p), dataSource(boundary), impl);
        }
    }

    private final Path                                        boundary;
    private Map<String, Exception>                            exceptions;
    private Map<CgmesEquipmentModelMapping, VerificationData> verificationDataForAllModelMapping;
    private static final Logger                               LOG = LoggerFactory
            .getLogger(ModelsConversionValidation.class);
}
