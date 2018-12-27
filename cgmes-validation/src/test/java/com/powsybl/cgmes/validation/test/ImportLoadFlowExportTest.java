package com.powsybl.cgmes.validation.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import com.powsybl.cgmes.conversion.CgmesExport;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.cgmes.model.test.cim14.Cim14SmallCasesCatalog;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class ImportLoadFlowExportTest {
    @BeforeClass
    public static void setUp() {
        catalog = new Cim14SmallCasesCatalog();
    }

    @Test
    public void smallcase1() throws IOException {
        importLoadFlowExport(catalog.small1());
    }

    @Test
    public void ieee14() throws IOException {
        importLoadFlowExport(catalog.ieee14());
    }

    private void importLoadFlowExport(TestGridModel gm) throws IOException {
        String impl = TripleStoreFactory.defaultImplementation();
        Properties importParameters = new Properties();
        importParameters.put("powsyblTripleStore", impl);
        importParameters.put("storeCgmesModelAsNetworkExtension", "true");

        String name1 = gm.name().replace('/', '-');
        Path working = Files.createTempDirectory("temp-loadflow-" + name1 + "-");
        Path output = working.resolve("cgmes-output");

        CgmesImport i = new CgmesImport();
        ReadOnlyDataSource ds = gm.dataSource();
        Network n = i.importData(ds, importParameters);

        // Run a LoadFlow
        LoadFlowComputation lf = new LoadFlowComputation();
        if (lf.available()) {
            LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
            String computedStateId = "computed";
            lf.compute(n, loadFlowParameters, computedStateId, working);

            // Leave load flow results as current state of network
            n.getStateManager().setWorkingState(computedStateId);
        }

        // Export the whole (updated) CgmesModel
        // By default, SV data in CgmesModel is replaced by current state of network
        CgmesExport e = new CgmesExport();
        Files.createDirectories(output);
        String basename = "";
        DataSource exportDataSource = new FileDataSource(output, basename);
        e.export(n, new Properties(), exportDataSource);
    }

    private static Cim14SmallCasesCatalog catalog;
}
