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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.cgmes.conversion.test.TopologyTester;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.network.Network;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class LoadFlowTester {

    public LoadFlowTester(List<String> tripleStoreImplementations) {
        this(tripleStoreImplementations, null);
    }

    public LoadFlowTester(List<String> tripleStoreImplementations, LoadFlowValidation loadFlowValidation) {
        this.tripleStoreImplementations = tripleStoreImplementations;
        this.loadFlowValidation = loadFlowValidation;
        this.validateTopology = true;
        this.strictTopologyTest = false;
    }

    public void testLoadFlow(TestGridModel gm) throws IOException {
        testLoadFlow(gm, this.loadFlowValidation);
    }

    public void testLoadFlow(TestGridModel gm, LoadFlowValidation loadFlowValidation0) throws IOException {
        if (!gm.exists()) {
            LOG.error("Test grid model does not exist {}", gm.name());
            fail();
        }
        LoadFlowValidation loadFlowValidation = loadFlowValidation0;
        if (loadFlowValidation == null) {
            loadFlowValidation = loadFlowValidationFor(gm);
        }
        if (!loadFlowValidation.computationAvailable() && !loadFlowValidation.validateInitialState()) {
            LOG.error("LoadFlowValidation not avaialable");
            fail();
        }
        // TODO receive import parameters for the test
        Properties importParams = null;
        Properties iparams = importParams == null ? new Properties() : importParams;
        iparams.put("storeCgmesModelAsNetworkProperty", "true");
        // Ensure properties are stored as strings
        // (getProperty returns null if the property exists but is not a string)
        String sb = Boolean.toString(loadFlowValidation.changeSignForShuntReactivePowerFlowInitialState());
        iparams.put("changeSignForShuntReactivePowerFlowInitialState", sb);
        for (String impl : tripleStoreImplementations) {
            LOG.info("testLoadFlow {} {}", gm.name(), impl);
            iparams.put("powsyblTripleStore", impl);
            testLoadFlow(gm, loadFlowValidation, iparams);
        }
    }

    public void testLoadFlow(TestGridModel gm, LoadFlowValidation loadFlowValidation, Properties iparams)
            throws IOException {
        CgmesImport i = new CgmesImport();
        try (FileSystem fs = Jimfs.newFileSystem()) {
            ReadOnlyDataSource ds = gm.dataSourceBasedOn(fs);
            Network network = i.importData(ds, iparams);
            if (network.getSubstationCount() == 0) {
                fail("Model is empty");
            }
            if (validateTopology) {
                CgmesModel cgmes = (CgmesModel) network.getProperties().get(CgmesImport.NETWORK_PS_CGMES_MODEL);
                if (!new TopologyTester(cgmes, network).test(strictTopologyTest)) {
                    fail("Topology test failed");
                }
            }
            if (loadFlowValidation.debugNetwork() != null) {
                loadFlowValidation.debugNetwork().accept(network);
            }
            loadFlowValidation.validate(network);
        }
    }

    public static LoadFlowValidation loadFlowValidationFor(TestGridModel gm) throws IOException {
        String name1 = gm.name().replace('/', '-');
        Path working = Files.createTempDirectory("temp-loadflow-validation-" + name1 + "-");
        return new LoadFlowValidation.Builder()
                .workingDirectory(working)
                .writeNetworksInputsResults(true)
                // Assume all test cases are solved
                .validateInitialState(false)
                .compareWithInitialState(false)
                .build();
    }

    private final List<String> tripleStoreImplementations;
    private final LoadFlowValidation loadFlowValidation;
    private final boolean validateTopology;
    private final boolean strictTopologyTest;

    private static final Logger LOG = LoggerFactory.getLogger(LoadFlowTester.class);
}