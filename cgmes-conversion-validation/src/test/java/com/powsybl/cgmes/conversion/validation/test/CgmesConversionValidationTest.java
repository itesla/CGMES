package com.powsybl.cgmes.conversion.validation.test;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.conformity.test.CgmesConformity1Catalog;
import com.powsybl.cgmes.conversion.validation.CgmesModelConversion;
import com.powsybl.cgmes.conversion.validation.ConversionValidationResult;
import com.powsybl.cgmes.model.CgmesOnDataSource;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.mock.LoadFlowFactoryMock;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.triplestore.api.TripleStore;
import com.powsybl.triplestore.api.TripleStoreException;
import com.powsybl.triplestore.api.TripleStoreFactory;

public class CgmesConversionValidationTest {

    @BeforeClass
    public static void setUp() {
        catalog = new CgmesConformity1Catalog();
    }

    @Test
    public void microGridBaseCaseAssembled() throws IOException {

        ReadOnlyDataSource ds = catalog.microGridBaseCaseAssembled().dataSource();
        String impl = TripleStoreFactory.defaultImplementation();
        double threshold = 0.01;
        try (FileSystem fs = Jimfs.newFileSystem(Configuration.unix())) {

            ValidationConfig config = loadFlowParametersConfig(fs, threshold);
            CgmesModelConversion model = createCgmesModel(ds, impl);
            model.z0Nodes();
            ConversionValidationResult conversionValidationResult = new CgmesConversionValidationTester(model)
                .test("microGridBaseCaseAssembled", config);
            assertFalse(getFailed(conversionValidationResult));
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

    private boolean getFailed(ConversionValidationResult conversionValidationResult) {
        return conversionValidationResult.failedCount > 0;
    }

    private ValidationConfig loadFlowParametersConfig(FileSystem fs, double threshold) {
        InMemoryPlatformConfig pconfig = new InMemoryPlatformConfig(fs);
        pconfig
            .createModuleConfig("componentDefaultConfig")
            .setStringProperty("LoadFlowFactory", LoadFlowFactoryMock.class.getCanonicalName());
        ValidationConfig config = ValidationConfig.load(pconfig);
        config.setVerbose(true);
        config.setThreshold(threshold);
        config.setOkMissingValues(false);
        config.setLoadFlowParameters(new LoadFlowParameters());
        return config;
    }

    private static CgmesConformity1Catalog catalog;
    private static final Logger LOG = LoggerFactory.getLogger(CgmesConversionValidationTest.class);
}
