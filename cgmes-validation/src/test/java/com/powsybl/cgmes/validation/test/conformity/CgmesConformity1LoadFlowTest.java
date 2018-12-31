/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test.conformity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import com.powsybl.cgmes.conversion.test.DebugPhaseTapChanger;
import com.powsybl.cgmes.model.PowerFlow;
import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.cgmes.validation.test.LoadFlowTester;
import com.powsybl.cgmes.validation.test.LoadFlowValidation;
import com.powsybl.cgmes.validation.test.TestGridModelPath;
import com.powsybl.cgmes.validation.test.balance.ReportBusBalances;
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
    public void reportMicroGridBaseCaseBE() throws IOException {
        new ReportBusBalances(catalog.microGridBaseCaseBE())
                .setConsiderPhaseAngleClock(false)
                .report();
    }

    @Test
    public void reportMiniBusBranchBaseCase() throws IOException {
        TestGridModel t = catalog.miniBusBranch();
        new ReportBusBalances(t).report();
    }

    @Test
    public void reportMiniBusBranchType1() throws IOException {
        Path p = Paths.get(
                "/Users/zamarrenolm//works/RTE/cgmes_2017_2018/references/ENTSOE/CGMES Conformity Test Configurations/CGMES_v2.4.15_TestConfigurations_v4.0.3/MiniGrid/BusBranch/Type1");
        String filename = "MiniGridTestConfiguration_T1_EQ_v3.0.0.xml";
        new ReportBusBalances(new TestGridModelPath(p, filename, null))
                .setConsiderPhaseAngleClock(false)
                .report();
    }

    @Test
    public void reportMiniBusBranchType2() throws IOException {
        Path p = Paths.get(
                "/Users/zamarrenolm//works/RTE/cgmes_2017_2018/references/ENTSOE/CGMES Conformity Test Configurations/CGMES_v2.4.15_TestConfigurations_v4.0.3/MiniGrid/BusBranch/Type2");
        String filename = "MiniGridTestConfiguration_T2_EQ_v3.0.0.xml";
        new ReportBusBalances(new TestGridModelPath(p, filename, null))
                .setConsiderPhaseAngleClock(false)
                .report();
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
                .maxBusesFailInitialState(2)
                // Debug the phase tap changer that does not match expected flow
                .debugNetwork(network -> new DebugPhaseTapChanger(
                        network.getTwoWindingsTransformer("_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0"),
                        2,
                        new PowerFlow(-55.2263, 221.8674))
                                .debug())
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
                .threshold(1.45)
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

    private static CgmesConformity1Catalog catalog;
    private static LoadFlowTester tester;
    private static Path working;
}
