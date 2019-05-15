package com.powsybl.cgmes.conversion.validation.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.conversion.validation.CgmesConversion;
import com.powsybl.cgmes.conversion.validation.CgmesModelConversion;
import com.powsybl.cgmes.conversion.validation.ConversionValidationResult;
import com.powsybl.cgmes.conversion.validation.ConversionValidationResult.VerificationData;
import com.powsybl.cgmes.conversion.validation.InterpretationForConversionValidation;
import com.powsybl.cgmes.conversion.validation.ModelConversionValidation;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletion;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletionParameters;
import com.powsybl.loadflow.validation.ValidationConfig;

public class CgmesConversionValidationTester {

    public CgmesConversionValidationTester(CgmesModelConversion gm) {
        this.model = gm;
    }

    public ConversionValidationResult test(String modelName, ValidationConfig config) {

        CgmesModelForInterpretation interpretedModel = InterpretationForConversionValidation
            .getCgmesModelForInterpretation(modelName, model);
        List<InterpretationAlternative> mappingConfigs = InterpretationForConversionValidation.getConfigList();

        Map<InterpretationAlternative, VerificationData> verificationDataForAllModelMapping = new HashMap<>();

        for (InterpretationAlternative mappingConfig : mappingConfigs) {
            CgmesConversion conversion = new CgmesConversion(model, mappingConfig);
            Network network = conversion.convert();
            VerificationData verificationData = validateModel(interpretedModel, mappingConfig, network, config);
            verificationDataForAllModelMapping.put(mappingConfig, verificationData);
        }
        return getConversionValidationresult(verificationDataForAllModelMapping);
    }

    private ConversionValidationResult getConversionValidationresult(Map<InterpretationAlternative, VerificationData> verificationDataForAllModelMapping) {
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

    private static CgmesModelConversion model;
    private static final Logger LOG = LoggerFactory.getLogger(CgmesConversionValidationTester.class);
}
