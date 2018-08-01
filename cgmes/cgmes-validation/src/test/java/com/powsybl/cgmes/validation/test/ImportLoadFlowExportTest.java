package com.powsybl.cgmes.validation.test;

/*
 * #%L
 * CGMES conversion
 * %%
 * Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.CgmesModel;
import com.powsybl.cgmes.conversion.CgmesExport;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.cgmes.test.TestGridModel;
import com.powsybl.cgmes.test.cim14.Cim14SmallCasesCatalog;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class ImportLoadFlowExportTest {
    @BeforeClass
    public static void setUp() {
        catalog = new Cim14SmallCasesCatalog();
    }

    @Test
    public void smallcase1() {
        importLoadFlowExport(catalog.small1());
    }

    @Test
    public void ieee14() {
        importLoadFlowExport(catalog.ieee14());
    }

    private void importLoadFlowExport(TestGridModel gm) {
        String impl = TripleStoreFactory.defaultImplementation();

        CgmesImport i = new CgmesImport();
        ReadOnlyDataSource importDataSource = DataSourceUtil.createDataSource(
                gm.path(),
                gm.basename(),
                gm.getCompressionExtension(),
                null);
        Properties importParameters = new Properties();
        importParameters.put("powsyblTripleStore", impl);
        importParameters.put("storeCgmesModelAsNetworkProperty", "true");
        Network n = i.importData(importDataSource, importParameters);

        // Run a LoadFlow
        LoadFlowComputation lf = new LoadFlowComputation();
        if (lf.available()) {
            LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
            String computedStateId = "computed";
            lf.compute(n, loadFlowParameters, computedStateId, gm.path());

            // Leave load flow results as current state of network
            n.getStateManager().setWorkingState(computedStateId);
        }

        // Export the whole (updated) CgmesModel
        // By default, SV data in CgmesModel is replaced by current state of network
        CgmesExport e = new CgmesExport();
        Path output = gm.path().resolve("temp-import-lf-export");
        ensureFolder(output);
        String basename = "";
        DataSource exportDataSource = new FileDataSource(output, basename);
        e.export(n, new Properties(), exportDataSource);
    }

    private void ensureFolder(Path p) {
        try {
            Files.createDirectories(p);
        } catch (IOException x) {
            throw new PowsyblException(
                    String.format("Creating directories %s", p), x);
        }
    }

    private CgmesModel cgmesModel(Network n) {
        Object c = n.getProperties().get(CgmesImport.NETWORK_PS_CGMES_MODEL);
        assert c instanceof CgmesModel;
        CgmesModel cgmes = (CgmesModel) c;
        return cgmes;
    }

    private static Cim14SmallCasesCatalog catalog;

    private static final Logger           LOG = LoggerFactory
            .getLogger(ImportLoadFlowExportTest.class);
}
