package com.powsybl.cgmes.validation.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.datasource.ZipFileDataSource;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.mock.LoadFlowFactoryMock;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletion;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletionParameters;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.loadflow.validation.ValidationOutputWriter;
import com.powsybl.loadflow.validation.ValidationType;

public class TestBase {

    public TestBase(String sdata) {
        this.data = Paths.get(sdata);
    }

    public Network convert(String rpath) {
        return convert(this.data.resolve(rpath));
    }

    public Network convert(Path path) {
        Properties iparams = new Properties();
        // iparams.put("createBusbarSectionForEveryConnectivityNode", "true");
        Network network = Importers.importData("CGMES",
                dataSource(path),
                iparams,
                LocalComputationManager.getDefault());
        return network;
    }

    public DataSource dataSource(Path path) {
        if (!Files.exists(path)) {
            fail();
        }

        String spath = path.toString();
        if (Files.isDirectory(path)) {
            String basename = spath.substring(spath.lastIndexOf('/') + 1);
            return new FileDataSource(path, basename);
        } else if (Files.isRegularFile(path) && spath.endsWith(".zip")) {
            return new ZipFileDataSource(path);
        } else {
            fail();
        }
        return null;
    }

    public void reportLimits(Network network) {
        new LimitsSummary(network.getExtension(CgmesModelExtension.class).getCgmesModel()).report();
    }

    public void computeMissingFlows(Network network) {
        computeMissingFlows(network, loadFlowParameters());
    }

    public void computeMissingFlows(Network network, LoadFlowParameters lfp) {
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
    }

    public void assertCheckBuses(Network network) throws IOException {
        assertTrue(checkBuses(network));
    }

    public boolean checkBuses(Network network) throws IOException {
        return checkBuses(network, null);
    }

    public boolean checkBuses(Network network, List<String> detail) throws IOException {
        double threshold = 1.0;

        LoadFlowParameters lfp = loadFlowParameters();
        computeMissingFlows(network, lfp);
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {
            Path working = validationWorking(fs);
            boolean r = ValidationType.BUSES.check(
                    network,
                    validationConfig(fs, lfp, threshold),
                    working);
            if (detail != null) {
                detail.addAll(
                        Files.readAllLines(ValidationType.BUSES.getOutputFile(working),
                                Charset.defaultCharset()));
            }
            return r;
        }
    }

    public ValidationConfig validationConfig(FileSystem fs, LoadFlowParameters lfp, double threshold) {
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

    public Path validationWorking(FileSystem fs) throws IOException {
        return Files.createDirectories(fs.getPath("/temp-lf-validation"));
    }

    public LoadFlowParameters loadFlowParameters() {
        boolean specificCompatibility = false;

        LoadFlowParameters lfp = new LoadFlowParameters();
        lfp.setSpecificCompatibility(specificCompatibility);
        return lfp;
    }

    protected double asDouble(String v) {
        return v.equals("inv") ? Double.NaN : Double.parseDouble(v);
    }

    protected boolean valid(double... values) {
        for (double v : values) {
            if (Double.isNaN(v)) {
                return false;
            }
        }
        return true;
    }

    protected final Path data;
}
