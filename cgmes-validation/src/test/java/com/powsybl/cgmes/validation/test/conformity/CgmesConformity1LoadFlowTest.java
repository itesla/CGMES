/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test.conformity;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

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

import org.junit.BeforeClass;
import org.junit.Test;

import com.powsybl.cgmes.conformity.test.CgmesConformity1Catalog;
import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.cgmes.conversion.test.DebugPhaseTapChanger;
import com.powsybl.cgmes.model.PowerFlow;
import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.cgmes.validation.test.loadflow.LoadFlowTester;
import com.powsybl.cgmes.validation.test.loadflow.LoadFlowValidation;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesConformity1LoadFlowTest {
    @BeforeClass
    public static void setUp() throws IOException {
        catalog = new CgmesConformity1Catalog();
        tester = new LoadFlowTester(TripleStoreFactory.onlyDefaultImplementation());
        working = Files.createTempDirectory("temp-conformity1-loadflow-test-");
    }

    @Test
    public void microGridBaseCaseBE() throws IOException {
        TestGridModel t = catalog.microGridBaseCaseBE();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(working.resolve(t.name()))
                .writeNetworksInputsResults(true)
                .validateInitialState(true)
                .specificCompatibility(true)
                // Threshold raised from 0.1 to 0.3 after 3wtx
                .threshold(0.3)
                // PP_Brussels 110 kV has huge mismatch due to phase shifter
                // If we use the given SV tap position (16) mismatch is > 850 MW in P
                // If we use starting step for steady state (10) mismatch in P, Q are < 1.4 MVA
                // The mismatch could also be related to interpretation of phase shift sign:
                // angles for tap position 10 and 16 are the same, only sign changes
                .maxBusesFailInitialState(0)
                .changeSignForPhaseTapChange(true)
                // Debug the phase tap changer that does not match expected flow
                .debugNetwork(network -> new DebugPhaseTapChanger(
                        network.getTwoWindingsTransformer("_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0"),
                        2,
                        new PowerFlow(-55.2263, 221.8674))
                                .debug())
                // This is the required threshold after 3wtx flows calc has been added
                .threshold(1.2)
                // TODO _3a3b27be does not have reactive margins as synchronous machine
                // properties
                // But it has a reactive capability curve
                .maxGeneratorsFailInitialState(1)
                // TODO boundaries are missing, do not compare with initial state
                .compareWithInitialState(false)
                .build();
        Properties importParams = new Properties();
        importParams.put("considerPhaseAngleClock", "false");
        tester.testLoadFlow(t, validation, importParams);
    }

    @Test
    public void microGridBaseCaseNL() throws IOException {
        TestGridModel t = catalog.microGridBaseCaseNL();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
            .workingDirectory(working.resolve(t.name()))
            .writeNetworksInputsResults(true)
            .validateInitialState(true)
            .specificCompatibility(true)
            // But two generators have Q out of allowed interval (1dc9afba and 2844585c)
            // In both cases Q = -26.68 when [Qmin, Qmax] = [0, 200]
            .maxGeneratorsFailInitialState(2)
            .compareWithInitialState(true)
            .maxGeneratorsFailComputedState(2)
            .build();
        tester.testLoadFlow(t, validation);
    }

    @Test
    public void microGridBaseCaseAssembled() throws IOException {
        TestGridModel t = catalog.microGridBaseCaseAssembled();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
            .workingDirectory(working.resolve(t.name()))
            .writeNetworksInputsResults(true)
            .validateInitialState(true)
            .specificCompatibility(true)
            // Validation considerations from microNL and microBE apply here
            .changeSignForPhaseTapChange(true)
            .threshold(1.2)
            .maxBusesFailInitialState(1)
            .maxGeneratorsFailInitialState(3)
            .compareWithInitialState(true)
            .maxBusesFailComputedState(1)
            .maxGeneratorsFailComputedState(3)
            .build();
        tester.testLoadFlow(t, validation);
    }

    @Test
    public void microGridType4BE() throws IOException {
        TestGridModel t = catalog.microGridType4BE();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
            .workingDirectory(working.resolve(t.name()))
            .writeNetworksInputsResults(true)
            .validateInitialState(true)
            .specificCompatibility(true)
            .maxBusesFailInitialState(1)
            // TODO _3a3b27be does not have reactive margins as synchronous machine
            // properties
            // But it has a reactive capability curve
            .maxGeneratorsFailInitialState(1)
            .build();
        tester.testLoadFlow(t, validation);
    }

    @Test
    public void miniBusBranch() throws IOException {
        TestGridModel t = catalog.miniBusBranch();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
            .workingDirectory(working.resolve(t.name()))
            .writeNetworksInputsResults(true)
            .validateInitialState(true)
            .compareWithInitialState(false)
            .build();
        tester.testLoadFlow(t, validation);
    }

    @Test
    public void miniNodeBreaker() throws IOException {
        TestGridModel t = catalog.miniNodeBreaker();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
            .workingDirectory(working.resolve(t.name()))
            .writeNetworksInputsResults(true)
            .validateInitialState(true)
            .compareWithInitialState(false)
            .build();
        tester.testLoadFlow(t, validation);
    }

    @Test
    public void smallBusBranch() throws IOException {

        // For two-winding transformers LF results have been computed
        // considering half the total magnetizing branch susceptance
        // as shunt admittance to ground at each end of the transformer PI model.
        // Only transformer with b != 0 is at substation "Vale of Arryn".

        // There are two mismatches at bus balance > 0.1:
        // Substation Waymoot, TopologicalNode Fieldale (Q mismatch = 0.1710)
        // Substation Qarth, TopologicalNode EastLima1 (Q mismatch = 0.1441)
        // This is the reason to raise the threshold to 0.1711

        TestGridModel t = catalog.smallBusBranch();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
            .workingDirectory(working.resolve(t.name()))
            .writeNetworksInputsResults(true)
            .validateInitialState(true)
            .threshold(0.1711)
            .specificCompatibility(true)
            .compareWithInitialState(true)
            .build();
        tester.testLoadFlow(t, validation);
    }

    @Test
    public void smallNodeBreaker() throws IOException {
        // Same considerations made for bus-branch model apply here
        TestGridModel t = catalog.smallNodeBreaker();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
            .workingDirectory(working.resolve(t.name()))
            .writeNetworksInputsResults(true)
            .validateInitialState(true)
            .threshold(0.1711)
            .specificCompatibility(true)
            .compareWithInitialState(true)
            .build();
        tester.testLoadFlow(t, validation);
    }

    @Test
    public void smallGridNodeBreakerDL() {
        Network network = Importers.importData("CGMES",
            catalog.smallNodeBreaker().dataSource(),
            null,
            LocalComputationManager.getDefault());
        CgmesModelTripleStore cgmes = (CgmesModelTripleStore) network
            .getExtension(CgmesModelExtension.class)
            .getCgmesModel();
        PropertyBags ds = cgmes.query(
            "SELECT * WHERE { "
                + "?Diagram a cim:Diagram ; "
                + "cim:IdentifiedObject.name ?name ; "
                + "cim:Diagram.orientation ?orientation }");
        assertEquals("_bcd073b6-227c-4a1d-923d-20a01b5ffe12", ds.get(0).getId("Diagram"));
        assertEquals("Diagram1", ds.get(0).getLocal("name"));
        assertEquals("OrientationKind.negative", ds.get(0).getLocal("orientation"));
    }

    private static CgmesConformity1Catalog catalog;
    private static LoadFlowTester tester;
    private static Path working;
}
