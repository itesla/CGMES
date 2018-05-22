package com.powsybl.cgmes.conversion.test;

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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.CgmesModel;
import com.powsybl.cgmes.CgmesModelException;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.cgmes.test.TestGridModel;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.network.Network;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class LoadFlowTester {

    public LoadFlowTester(String[] tripleStoreImplementations) {
        this(tripleStoreImplementations, null);
    }

    public LoadFlowTester(
            String[] tripleStoreImplementations,
            LoadFlowValidation loadFlowValidation) {
        this.tripleStoreImplementations = tripleStoreImplementations;
        this.loadFlowValidation = loadFlowValidation;
        this.validateTopology = true;
        this.strictTopologyTest = false;
    }

    public void testLoadFlow(TestGridModel gm) {
        testLoadFlow(gm, this.loadFlowValidation);
    }

    public void testLoadFlow(TestGridModel gm, LoadFlowValidation loadFlowValidation0) {
        Path path = gm.path();
        if (Files.notExists(path)) {
            LOG.error("Path for grid model does not exist {}", path);
            return;
        }
        String basename = gm.basename();
        if (basename == null) {
            basename = "";
        }
        CgmesImport i = new CgmesImport();
        ReadOnlyDataSource dataSource = DataSourceUtil.createDataSource(gm.path(), gm.basename(), gm.getCompressionExtension(), null);
        LoadFlowValidation loadFlowValidation = loadFlowValidation0;
        if (loadFlowValidation == null) {
            loadFlowValidation = loadFlowValidationFor(gm);
        }
        if (!loadFlowValidation.computationAvailable()
                && !loadFlowValidation.validateInitialState()) {
            LOG.error("LoadFlowValidation not avaialable");
            return;
        }
        String name = path.getName(path.getNameCount() - 1).toString();
        for (String impl : tripleStoreImplementations) {
            LOG.info("testLoadFlow {} {}", name, impl);
            try {
                Properties parameters = new Properties();
                parameters.put("powsyblTripleStore", impl);
                parameters.put("storeCgmesModelAsNetworkProperty", "true");
                // Ensure properties are stored as strings
                // (getProperty returns null if the property exists but is not a string)
                String sb = Boolean.toString(
                        loadFlowValidation.changeSignForShuntReactivePowerFlowInitialState());
                parameters.put("changeSignForShuntReactivePowerFlowInitialState", sb);
                Network network = i.importData(dataSource, parameters);
                if (validateTopology) {
                    CgmesModel cgmes = (CgmesModel) network.getProperties()
                            .get(CgmesImport.NETWORK_PS_CGMES_MODEL);
                    if (!new TopologyTester(cgmes, network).test(strictTopologyTest)) {
                        fail("Topology test failed");
                    }
                }
                if (loadFlowValidation.debugNetwork() != null) {
                    loadFlowValidation.debugNetwork().accept(network);
                }
                loadFlowValidation.validate(network);
            } catch (CgmesModelException | IOException x) {
                LOG.error(x.getMessage());
                x.printStackTrace();
                fail();
            }
        }
    }

    public static LoadFlowValidation loadFlowValidationFor(TestGridModel gm) {
        LoadFlowValidation.Builder b = new LoadFlowValidation.Builder()
                .workingDirectory(gm.path())
                .writeNetworksInputsResults(true);
        if (!gm.solved()) {
            b.validateInitialState(false).compareWithInitialState(false);
        }
        return b.build();
    }

    private final String[]           tripleStoreImplementations;
    private final LoadFlowValidation loadFlowValidation;
    private final boolean            validateTopology;
    private final boolean            strictTopologyTest;

    private static final Logger      LOG = LoggerFactory.getLogger(LoadFlowTester.class);
}
