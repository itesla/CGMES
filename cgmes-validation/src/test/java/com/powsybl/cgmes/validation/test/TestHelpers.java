package com.powsybl.cgmes.validation.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.mock.LoadFlowFactoryMock;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletion;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletionParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.loadflow.validation.ValidationOutputWriter;
import com.powsybl.loadflow.validation.ValidationType;

public final class TestHelpers {

    private TestHelpers() {
    }

    // Limits

    public static LimitsSummary limitsSummary(Network network) {
        return new LimitsSummary(network.getExtension(CgmesModelExtension.class).getCgmesModel());
    }

    public static Network computeMissingFlows(Network network) {
        return computeMissingFlows(network, loadFlowParameters());
    }

    // Complete results

    public static Network computeMissingFlows(Network network, LoadFlowParameters lfp) {
        float epsilonX = 0;
        boolean applyXCorrection = false;
        double z0Threshold = 1e-6;
        LoadFlowResultsCompletion lf = new LoadFlowResultsCompletion(
            new LoadFlowResultsCompletionParameters(
                epsilonX,
                applyXCorrection,
                z0Threshold),
            lfp);
        lf.run(network, null);
        return network;
    }

    public static Network resetFlows(Network network) {
        network.getBranchStream().forEach(b -> {
            b.getTerminal1().setP(Double.NaN);
            b.getTerminal2().setP(Double.NaN);
        });
        network.getThreeWindingsTransformerStream().forEach(tx -> {
            tx.getLeg1().getTerminal().setP(Double.NaN);
            tx.getLeg2().getTerminal().setP(Double.NaN);
            tx.getLeg3().getTerminal().setP(Double.NaN);
        });
        return network;
    }

    // Check buses

    public static final double THR_P = 0.1;
    public static final double THR_Q = 1.0;

    public static boolean checkBuses(Network network, double thresholdp, double thresholdq) {
        return checkBuses(network, thresholdp, thresholdq, null);
    }

    public static boolean checkBuses(
        Network network,
        double thresholdp,
        double thresholdq,
        Map<String, Complex> errors) {
        LoadFlowParameters lfp = loadFlowParameters();
        computeMissingFlows(network, lfp);
        try {
            try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
                Path working = validationWorking(fs);
                ValidationType.BUSES.check(
                    network,
                    validationConfig(fs, lfp, Math.min(thresholdp, thresholdq)),
                    working);
                return validationCheckResults(working, thresholdq, thresholdq, errors);
            }
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    private static boolean validationCheckResults(
        Path working,
        double thresholdp,
        double thresholdq,
        Map<String, Complex> errors) throws IOException {
        boolean[] result = {true};
        List<String> lines = Files.readAllLines(ValidationType.BUSES.getOutputFile(working), Charset.defaultCharset());
        // Check header matches expected contents
        assertEquals("id;incomingP;incomingQ;loadP;loadQ", lines.get(1));
        // Skip title and header
        lines.stream().skip(2).forEach(d -> {
            String[] fields = d.split(";");
            String id = fields[0];
            try {
                double incomingp = asDouble(fields[1]);
                double incomingq = asDouble(fields[2]);
                double loadp = asDouble(fields[3]);
                double loadq = asDouble(fields[4]);
                // Ignore invalid values
                if (valid(incomingp, incomingq, loadp, loadq)) {
                    double errp = Math.abs(incomingp + loadp);
                    double errq = Math.abs(incomingq + loadq);
                    if (errp > thresholdp || errq > thresholdq) {
                        result[0] = false;
                        if (errors != null) {
                            errors.put(id, new Complex(errp, errq));
                        }
                    }
                }
            } catch (Exception x) {
                LOG.error("Error in checkBuses {}", x);
            }
        });
        return result[0];
    }

    public static ValidationConfig validationConfig(FileSystem fs, LoadFlowParameters lfp, double threshold) {
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fs);
        MapModuleConfig defaultConfig = platformConfig.createModuleConfig("componentDefaultConfig");
        defaultConfig.setStringProperty("LoadFlowFactory", LoadFlowFactoryMock.class.getCanonicalName());
        ValidationConfig config = ValidationConfig.load(platformConfig);
        config.setVerbose(false);
        config.setLoadFlowParameters(lfp);
        config.setValidationOutputWriter(ValidationOutputWriter.CSV);
        // Flows should be equal within a given precision
        // Max mismatch allowed in a bus is also given with this threshold parameter
        config.setThreshold(threshold);
        config.setOkMissingValues(false);
        return config;
    }

    public static Path validationWorking(FileSystem fs) throws IOException {
        return Files.createDirectories(fs.getPath("/temp-lf-validation"));
    }

    public static LoadFlowParameters loadFlowParameters() {
        boolean specificCompatibility = false;
        LoadFlowParameters lfp = new LoadFlowParameters();
        lfp.setSpecificCompatibility(specificCompatibility);
        return lfp;
    }

    private static double asDouble(String v) {
        return v.equals("inv") ? Double.NaN : Double.parseDouble(v);
    }

    private static boolean valid(double... values) {
        for (double v : values) {
            if (Double.isNaN(v)) {
                return false;
            }
        }
        return true;
    }

    private static final Logger LOG = LoggerFactory.getLogger(TestHelpers.class);
}
