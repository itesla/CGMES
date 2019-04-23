package com.powsybl.cgmes.conversion.validation.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
import com.powsybl.cgmes.model.interpretation.InterpretedModel;
import com.powsybl.cgmes.tools.Catalog;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.util.BranchData;
import com.powsybl.iidm.network.util.TwtData;
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

    private CgmesModel load(Path p) {
        String impl = TripleStoreFactory.defaultImplementation();
        if (boundary == null) {
            return CgmesModelFactory.create(dataSource(p), impl);
        } else {
            return CgmesModelFactory.create(dataSource(p), dataSource(boundary), impl);
        }
    }

    public String tsoName(Path p) {
        String model = modelName(p);
        return tsoName(model);
    }

    public void validate(Path p) throws IOException {
        CgmesModel cgmesModel = load(p);
        BestModelInterpretation cgmesInterpretation = new BestModelInterpretation(cgmesModel);
        List<CgmesEquipmentModelMapping> configs = new ArrayList<>();
        cgmesInterpretation.addModelMappingConfigurations(configs);
        cgmesInterpretation.interpret(configs);
        CgmesEquipmentModelMapping modelMapping = cgmesInterpretation.getModelMapping();
        CgmesConversion conversion = new CgmesConversion(cgmesModel, modelMapping);
        Network network = conversion.convert();
        assertTrue(validateModelInterpret(tsoName(p), network, cgmesInterpretation));
    }

    private boolean validateModelInterpret(String tsoName, Network network, BestModelInterpretation cgmesInterpretation)
            throws IOException {
        boolean validatedFlows = true;
        double threshold = 0.01;
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            ValidationConfig config = loadFlowValidationConfig(fs, threshold);

            computeMissingFlows(network, config.getLoadFlowParameters());

            validatedFlows = validateModelFlows(tsoName, network, cgmesInterpretation, config);
        }
        return validatedFlows;
    }

    private boolean validateModelFlows(String tsoName, Network network, BestModelInterpretation cgmesInterpretation,
            ValidationConfig config) {
        boolean lineFlowValidated = true;
        boolean xfmr2FlowValidated = true;
        boolean xfmr3FlowValidated = true;
        InterpretedModel interpretedModel = cgmesInterpretation.getInputModel();
        for (Line l : network.getLines()) {
            BranchData branch = new BranchData(l, config.getEpsilonX(), config.applyReactanceCorrection());

            PropertyBag interpretedLine = interpretedModel.getLineParameters(l.getId());
            if (interpretedLine == null) {
                continue;
            }
            PropertyBag node1 = interpretedModel.getNodeParameters(interpretedLine.get("terminal1"));
            PropertyBag node2 = interpretedModel.getNodeParameters(interpretedLine.get("terminal2"));
            FlowCalculator calcFlowEnd1 = new FlowCalculator(interpretedModel);
            calcFlowEnd1.forLine(interpretedLine.get("terminal1"), node1, node2, interpretedLine,
                    cgmesInterpretation.getModelMapping());
            FlowCalculator calcFlowEnd2 = new FlowCalculator(interpretedModel);
            calcFlowEnd2.forLine(interpretedLine.get("terminal2"), node1, node2, interpretedLine,
                    cgmesInterpretation.getModelMapping());
            lineFlowValidated = lineFlowValidated && checkFlows(tsoName, branch, calcFlowEnd1, calcFlowEnd2, config);
        }
        for (TwoWindingsTransformer xfmr2 : network.getTwoWindingsTransformers()) {
            BranchData branch = new BranchData(xfmr2, config.getEpsilonX(), config.applyReactanceCorrection(),
                    config.getLoadFlowParameters().isSpecificCompatibility());

            PropertyBag interpretedXfmr2 = interpretedModel.getTransformerParameters(xfmr2.getId());
            if (interpretedXfmr2 == null) {
                continue;
            }
            PropertyBag node1 = interpretedModel.getNodeParameters(interpretedXfmr2.get("terminal1"));
            PropertyBag node2 = interpretedModel.getNodeParameters(interpretedXfmr2.get("terminal2"));
            FlowCalculator calcFlowEnd1 = new FlowCalculator(interpretedModel);
            calcFlowEnd1.forTwoWindingTransformer(interpretedXfmr2.get("terminal1"), node1, node2, interpretedXfmr2,
                    cgmesInterpretation.getModelMapping());
            FlowCalculator calcFlowEnd2 = new FlowCalculator(interpretedModel);
            calcFlowEnd2.forTwoWindingTransformer(interpretedXfmr2.get("terminal2"), node1, node2, interpretedXfmr2,
                    cgmesInterpretation.getModelMapping());
            xfmr2FlowValidated = xfmr2FlowValidated && checkFlows(tsoName, branch, calcFlowEnd1, calcFlowEnd2, config);
        }
        for (ThreeWindingsTransformer xfmr3 : network.getThreeWindingsTransformers()) {
            TwtData branch = new TwtData(xfmr3, config.getEpsilonX(), config.applyReactanceCorrection());

            PropertyBag interpretedXfmr3 = interpretedModel.getTransformerParameters(xfmr3.getId());
            if (interpretedXfmr3 == null) {
                continue;
            }
            PropertyBag node1 = interpretedModel.getNodeParameters(interpretedXfmr3.get("terminal1"));
            PropertyBag node2 = interpretedModel.getNodeParameters(interpretedXfmr3.get("terminal2"));
            PropertyBag node3 = interpretedModel.getNodeParameters(interpretedXfmr3.get("terminal3"));
            FlowCalculator calcFlowEnd1 = new FlowCalculator(interpretedModel);
            calcFlowEnd1.forThreeWindingTransformer(interpretedXfmr3.get("terminal1"), node1, node2, node3,
                    interpretedXfmr3,
                    cgmesInterpretation.getModelMapping());
            FlowCalculator calcFlowEnd2 = new FlowCalculator(interpretedModel);
            calcFlowEnd2.forThreeWindingTransformer(interpretedXfmr3.get("terminal2"), node1, node2, node3,
                    interpretedXfmr3,
                    cgmesInterpretation.getModelMapping());
            FlowCalculator calcFlowEnd3 = new FlowCalculator(interpretedModel);
            calcFlowEnd3.forThreeWindingTransformer(interpretedXfmr3.get("terminal3"), node1, node2, node3,
                    interpretedXfmr3,
                    cgmesInterpretation.getModelMapping());
            xfmr3FlowValidated = xfmr3FlowValidated
                    && checkFlows(tsoName, branch, calcFlowEnd1, calcFlowEnd2, calcFlowEnd3, config);
        }
        return lineFlowValidated && xfmr2FlowValidated && xfmr3FlowValidated;
    }

    private boolean checkFlows(String tsoName, BranchData branch, FlowCalculator calcFlowEnd1,
            FlowCalculator calcFlowEnd2, ValidationConfig config) {
        Branch.Side sideOne = Branch.Side.ONE;
        Branch.Side sideTwo = Branch.Side.TWO;
        boolean validatedEnd1 = true;
        if (branch.isConnected1()) {
            validatedEnd1 = checkFlow(tsoName, branch.getId(), "1", branch.getComputedP(sideOne),
                    branch.getComputedQ(sideOne),
                    calcFlowEnd1.getP(), calcFlowEnd1.getQ(), config);
        }
        boolean validatedEnd2 = true;
        if (branch.isConnected2()) {
            validatedEnd2 = checkFlow(tsoName, branch.getId(), "2", branch.getComputedP(sideTwo),
                    branch.getComputedQ(sideTwo),
                    calcFlowEnd2.getP(), calcFlowEnd2.getQ(), config);
        }
        return validatedEnd1 && validatedEnd2;
    }

    private boolean checkFlows(String tsoName, TwtData branch, FlowCalculator calcFlowEnd1, FlowCalculator calcFlowEnd2,
            FlowCalculator calcFlowEnd3, ValidationConfig config) {
        ThreeWindingsTransformer.Side sideOne = ThreeWindingsTransformer.Side.ONE;
        ThreeWindingsTransformer.Side sideTwo = ThreeWindingsTransformer.Side.TWO;
        ThreeWindingsTransformer.Side sideThree = ThreeWindingsTransformer.Side.THREE;
        boolean validatedEnd1 = true;
        if (branch.isConnected(sideOne)) {
            validatedEnd1 = checkFlow(tsoName, branch.getId(), "1", branch.getComputedP(sideOne),
                    branch.getComputedQ(sideOne),
                    calcFlowEnd1.getP(), calcFlowEnd1.getQ(), config);
        }
        boolean validatedEnd2 = true;
        if (branch.isConnected(sideTwo)) {
            validatedEnd2 = checkFlow(tsoName, branch.getId(), "2", branch.getComputedP(sideTwo),
                    branch.getComputedQ(sideTwo),
                    calcFlowEnd2.getP(), calcFlowEnd2.getQ(), config);
        }
        boolean validatedEnd3 = true;
        if (branch.isConnected(sideThree)) {
            validatedEnd3 = checkFlow(tsoName, branch.getId(), "3", branch.getComputedP(sideThree),
                    branch.getComputedQ(sideThree),
                    calcFlowEnd3.getP(), calcFlowEnd3.getQ(), config);
        }
        return validatedEnd1 && validatedEnd2 && validatedEnd3;
    }

    private boolean checkFlow(String tsoName, String id, String terminalNumber, double iidmP, double iidmQ,
            double cgmesP, double cgmesQ, ValidationConfig config) {
        boolean validatedP = Math.abs(iidmP - cgmesP) < config.getThreshold();
        if (!validatedP) {
            LOG.warn("{} {} {}: {} P{} {} {}", tsoName, ValidationType.FLOWS, ValidationUtils.VALIDATION_ERROR, id,
                    terminalNumber, iidmP, cgmesP);
        }
        boolean validatedQ = Math.abs(iidmQ - cgmesQ) < config.getThreshold();
        if (!validatedQ) {
            LOG.warn("{} {} {}: {} Q{} {} {}", tsoName, ValidationType.FLOWS, ValidationUtils.VALIDATION_ERROR, id,
                    terminalNumber, iidmQ, cgmesQ);
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
