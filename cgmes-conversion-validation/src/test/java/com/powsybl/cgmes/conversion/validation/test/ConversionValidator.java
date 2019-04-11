package com.powsybl.cgmes.conversion.validation.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.conversion.validation.BestModelInterpretation;
import com.powsybl.cgmes.conversion.validation.CgmesConversion;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping;
import com.powsybl.cgmes.model.interpretation.FlowCalculator;
import com.powsybl.cgmes.model.interpretation.InterpretationResult;
import com.powsybl.cgmes.model.interpretation.InterpretedModel;
import com.powsybl.cgmes.tools.Catalog;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.util.BranchData;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.mock.LoadFlowFactoryMock;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletion;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletionParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.loadflow.validation.ValidationType;
import com.powsybl.loadflow.validation.ValidationUtils;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.TripleStoreFactory;

public class ConversionValidator extends Catalog {

    public ConversionValidator(String sdata, String sboundary) {
        super(sdata);
        boundary = sboundary == null ? null : Paths.get(sboundary);
    }

    public Map<String, InterpretationResult> reviewAll(String pattern) throws IOException {
        Map<String, InterpretationResult> interpretations = new HashMap<>();
        reviewAll(pattern, p -> {
            try {
                validate(modelName(p), load(p));
            } catch (Exception x) {
                LOG.warn(x.getMessage());
            }
        });
        return interpretations;
    }

    public CgmesModel load(Path p) {
        String impl = TripleStoreFactory.defaultImplementation();
        if (boundary == null) {
            return CgmesModelFactory.create(dataSource(p), impl);
        } else {
            return CgmesModelFactory.create(dataSource(p), dataSource(boundary), impl);
        }
    }

    private void validate(String model, CgmesModel cgmesModel) throws IOException {
        BestModelInterpretation cgmesInterpretation = new BestModelInterpretation(cgmesModel);
        cgmesInterpretation.interpret();
        CgmesEquipmentModelMapping modelMapping = cgmesInterpretation.getModelMapping();
        CgmesConversion conversion = new CgmesConversion(cgmesModel, modelMapping);
        Network network = conversion.convert();
        validateModelInterpret(tsoName(model), network, cgmesInterpretation);
    }

    private void validateModelInterpret(String tsoName, Network network, BestModelInterpretation cgmesInterpretation) throws IOException {
        double threshold = 0.01;
        InterpretedModel interpretedModel = cgmesInterpretation.getInputModel();
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            ValidationConfig config = loadFlowValidationConfig(fs, threshold);

            computeMissingFlows(network, config.getLoadFlowParameters());

            network.getLineStream().forEach(l -> {
                BranchData branch = new BranchData(l, config.getEpsilonX(), config.applyReactanceCorrection());

                PropertyBag interpretedLine = interpretedModel.getLineParameters(l.getId());
                if (interpretedLine == null) {
                    return;
                }
                PropertyBag node1 = interpretedModel.getNodeParameters(interpretedLine.get("terminal1"));
                PropertyBag node2 = interpretedModel.getNodeParameters(interpretedLine.get("terminal2"));
                FlowCalculator calcFlowEnd1 = new FlowCalculator(interpretedModel);
                calcFlowEnd1.forLine(interpretedLine.get("terminal1"), node1, node2, interpretedLine, cgmesInterpretation.getModelMapping());
                FlowCalculator calcFlowEnd2 = new FlowCalculator(interpretedModel);
                calcFlowEnd2.forLine(interpretedLine.get("terminal2"), node1, node2, interpretedLine, cgmesInterpretation.getModelMapping());
                assertTrue(checkFlows(tsoName, branch, calcFlowEnd1, calcFlowEnd2, config));
            });
        }
    }

    private boolean checkFlows(String tsoName, BranchData branch, FlowCalculator calcFlowEnd1, FlowCalculator calcFlowEnd2, ValidationConfig config) {
        boolean validatedEnd1 = true; 
        if (branch.isConnected1()) {
            validatedEnd1 = checkFlow(tsoName, branch.getId(), "1", branch.getComputedP1(), branch.getComputedQ1(), calcFlowEnd1.getP(), calcFlowEnd1.getQ(), config);
        }
        boolean validatedEnd2 = true; 
        if (branch.isConnected2()) {
            validatedEnd2 = checkFlow(tsoName, branch.getId(), "2", branch.getComputedP2(), branch.getComputedQ2(), calcFlowEnd2.getP(), calcFlowEnd2.getQ(), config);
        }
        return validatedEnd1 && validatedEnd2;
    }

    private boolean checkFlow(String tsoName, String id, String terminalNumber, double iidmP, double iidmQ, double cgmesP, double cgmesQ, ValidationConfig config) {
        boolean validatedP = Math.abs(iidmP - cgmesP) < config.getThreshold();
        if (!validatedP) {
            LOG.warn("{} {} {}: {} P{} {} {}", tsoName, ValidationType.FLOWS, ValidationUtils.VALIDATION_ERROR, id, terminalNumber, iidmP, cgmesP);
        }
        boolean validatedQ = Math.abs(iidmQ - cgmesQ) < config.getThreshold();
        if (!validatedQ) {
            LOG.warn("{} {} {}: {} Q{} {} {}", tsoName, ValidationType.FLOWS, ValidationUtils.VALIDATION_ERROR, id, terminalNumber, iidmQ, cgmesQ);
        }
        return validatedP && validatedQ;
    }

    private ValidationConfig loadFlowValidationConfig(FileSystem fs, double threshold) {
        InMemoryPlatformConfig pconfig = new InMemoryPlatformConfig(fs);
        pconfig
                .createModuleConfig("componentDefaultConfig")
                .setStringProperty("LoadFlowFactory", LoadFlowFactoryMock.class.getCanonicalName());
        ValidationConfig config = ValidationConfig.load(pconfig);
        config.setVerbose(true);
        config.setThreshold(threshold);
        config.setOkMissingValues(false);
        config.setLoadFlowParameters(new LoadFlowParameters());
        LOG.info("specificCompatibility is {}", config.getLoadFlowParameters().isSpecificCompatibility());
        return config;
    }

    private void computeMissingFlows(Network network, LoadFlowParameters lfparams) {
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

    private String tsoName(String model) {
        int i = model.indexOf("_1D_") + 4;
        if (model.indexOf("_1D_") == -1) {
            i = model.indexOf("_2D_") + 4;
        }
        int j = model.indexOf("_", i);
        if (model.indexOf("_", i) > model.indexOf("\\", i)) {
            j = model.indexOf("\\", i);
        }
        if (j > i) {
            return model.substring(i, j);
        } else {
            return model.substring(i);
        }
    }

    private final Path          boundary;
    private static final Logger LOG = LoggerFactory.getLogger(ConversionValidator.class);
}
