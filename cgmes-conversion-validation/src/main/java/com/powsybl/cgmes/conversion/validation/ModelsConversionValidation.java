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

    public Map<String, ConversionValidationResult> reviewAll(String pattern, ValidationConfig config) throws IOException {
        Map<String, ConversionValidationResult> conversionValidationResult = new HashMap<>();
        reviewAll(pattern, p -> {
            try {
                LOG.info("case {}", modelName(p));
                CgmesModel m = load(p);
                InterpretedModel interpretedModel = InterpretationForConversionValidation.getInterpretedModel(m);
                List<CgmesEquipmentModelMapping> mappingConfigs = InterpretationForConversionValidation
                        .getConfigList(m);

                verificationDataForAllModelMapping = new HashMap<>();

                mappingConfigs.forEach(mappingConfig -> {
                    CgmesConversion conversion = new CgmesConversion(m, mappingConfig);
                    Network network = conversion.convert();
                    try {
                        VerificationData verificationData = validateModel(interpretedModel, mappingConfig, network, config);
                        verificationDataForAllModelMapping.put(mappingConfig, verificationData);
                    } catch (Exception x) {
                        LOG.warn(x.getMessage());
                    }
                });

                conversionValidationResult.put(modelName(p), getConversionValidationresult());

            } catch (Exception x) {
                exceptions.put(modelName(p), x);
                LOG.warn(x.getMessage());
            }
        });
        return conversionValidationResult;
    }

    public boolean getFailed(Map<String, ConversionValidationResult> conversionValidationResult) {
        return conversionValidationResult.values().stream().filter(cvr -> cvr.failed).limit(1).count() > 0;
    }

    private ConversionValidationResult getConversionValidationresult() {
        ConversionValidationResult r = new ConversionValidationResult();
        r.failed = failed(verificationDataForAllModelMapping);
        r.verificationDataForAllModelMapping = verificationDataForAllModelMapping;
        return r;
    }

    private boolean failed(Map<CgmesEquipmentModelMapping, VerificationData> verificationDataForAllModelMapping) {

        return verificationDataForAllModelMapping.values().stream().filter(vd -> vd.failed()).limit(1).count() > 0;
    }

    private VerificationData validateModel(InterpretedModel interpretedModel, CgmesEquipmentModelMapping mappingConfig,
            Network network, ValidationConfig config) throws IOException {
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
