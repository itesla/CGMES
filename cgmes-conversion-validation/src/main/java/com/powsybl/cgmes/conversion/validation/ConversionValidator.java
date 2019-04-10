package com.powsybl.cgmes.conversion.validation;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping;
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
                validate(load(p));
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

    private void validate(CgmesModel model) throws IOException {
        BestModelInterpretation cgmesInterpretation = new BestModelInterpretation(model);
        cgmesInterpretation.interpret();
        CgmesEquipmentModelMapping modelMapping = cgmesInterpretation.getModelMapping();
        CgmesConversion conversion = new CgmesConversion(model, modelMapping);
        Network network = conversion.convert();
        validateModelInterpret(network, cgmesInterpretation);
    }

    private void validateModelInterpret(Network network, BestModelInterpretation cgmesInterpretation) throws IOException {
        double threshold = 0.01;
        InterpretedModel interpretedModel = cgmesInterpretation.getInputModel();
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            ValidationConfig config = loadFlowValidationConfig(fs, threshold);

            computeMissingFlows(network, config.getLoadFlowParameters());
            network.getLineStream().forEach(l -> {
                PropertyBag interpretedLine = interpretedModel.getLineParameters(l.getId());
                BranchData branch = new BranchData(l, config.getEpsilonX(), config.applyReactanceCorrection());
                checkFlows(branch, config);
            });
        }
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

    public boolean checkFlows(BranchData branch, ValidationConfig config) {
        Objects.requireNonNull(branch);
        Objects.requireNonNull(branch.getId());
        boolean validated = true;

        if (!branch.isConnected1()) {
            validated &= checkDisconnectedTerminal(branch.getId(), "1", branch.getP1(), branch.getComputedP1(), branch.getQ1(), branch.getComputedQ1(), config);
        }
        if (!branch.isConnected2()) {
            validated &= checkDisconnectedTerminal(branch.getId(), "2", branch.getP2(), branch.getComputedP2(), branch.getQ2(), branch.getComputedQ2(), config);
        }
        if (branch.isConnected1() && ValidationUtils.isMainComponent(config, branch.isMainComponent1())) {
            validated &= checkConnectedTerminal(branch.getId(), "1", branch.getP1(), branch.getComputedP1(), branch.getQ1(), branch.getComputedQ1(), config);
        }
        if (branch.isConnected2() && ValidationUtils.isMainComponent(config, branch.isMainComponent2())) {
            validated &= checkConnectedTerminal(branch.getId(), "2", branch.getP2(), branch.getComputedP2(), branch.getQ2(), branch.getComputedQ2(), config);
        }
        return validated;
    }

    private static boolean checkDisconnectedTerminal(String id, String terminalNumber, double p, double pCalc, double q, double qCalc, ValidationConfig config) {
        boolean validated = true;
        if (!Double.isNaN(p) && Math.abs(p) > config.getThreshold()) {
            LOG.warn("{} {}: {} disconnected P{} {} {}", ValidationType.FLOWS, ValidationUtils.VALIDATION_ERROR, id, terminalNumber, p, pCalc);
            validated = false;
        }
        if (!Double.isNaN(q) && Math.abs(q) > config.getThreshold()) {
            LOG.warn("{} {}: {} disconnected Q{} {} {}", ValidationType.FLOWS, ValidationUtils.VALIDATION_ERROR, id, terminalNumber, q, qCalc);
            validated = false;
        }
        return validated;
    }

    private static boolean checkConnectedTerminal(String id, String terminalNumber, double p, double pCalc, double q, double qCalc, ValidationConfig config) {
        boolean validated = true;
        if (ValidationUtils.areNaN(config, pCalc) || Math.abs(p - pCalc) > config.getThreshold()) {
            LOG.warn("{} {}: {} P{} {} {}", ValidationType.FLOWS, ValidationUtils.VALIDATION_ERROR, id, terminalNumber, p, pCalc);
            validated = false;
        }
        if (ValidationUtils.areNaN(config, qCalc) || Math.abs(q - qCalc) > config.getThreshold()) {
            LOG.warn("{} {}: {} Q{} {} {}", ValidationType.FLOWS, ValidationUtils.VALIDATION_ERROR, id, terminalNumber, q, qCalc);
            validated = false;
        }
        return validated;
    }

    private final Path          boundary;
    private static final Logger LOG = LoggerFactory.getLogger(ConversionValidator.class);
}
