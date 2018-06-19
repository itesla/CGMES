package com.powsybl.cgmes.conversion.test.conformity;

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

import com.powsybl.cgmes.PowerFlow;
import com.powsybl.cgmes.conversion.test.DebugPhaseTapChanger;
import com.powsybl.cgmes.conversion.test.LoadFlowTester;
import com.powsybl.cgmes.conversion.test.LoadFlowValidation;
import com.powsybl.cgmes.test.TestGridModel;
import com.powsybl.cgmes_conformity.test.CgmesConformity1Catalog;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesConformity1LoadFlowTest {
    @BeforeClass
    public static void setUp() {
        catalog = new CgmesConformity1Catalog();
        tester = new LoadFlowTester(TripleStoreFactory.onlyDefaultImplementation());
    }

    @Test
    public void microBE() {
        TestGridModel microBE = catalog.microBE();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(microBE.path())
                .writeNetworksInputsResults(true)
                .validateInitialState(true)
                .specificCompatibility(true)
                // PP_Brussels 110 kV has huge mismatch due to phase shifter
                // If we use the given SV tap position (16) mismatch is > 850 MW in P
                // If we use starting step for steady state (10) mismatch in P, Q are < 1.4 MVA
                // The mismatch could also be related to interpretation of phase shift sign:
                // angles for tap position 10 and 16 are the same, only sign changes
                .maxBusesFailInitialState(1)
                // Debug the phase tap changer that does not match expected flow
                .debugNetwork(network -> new DebugPhaseTapChanger(
                        network.getTwoWindingsTransformer("_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0"),
                        2,
                        new PowerFlow(-53.765f, 220.3866f))
                                .debug())
                // TODO _3a3b27be does not have reactive margins as synchronous machine properties
                // But it has a reactive capability curve
                .maxGeneratorsFailInitialState(1)
                // TODO boundaries are missing, do not compare with initial state
                .compareWithInitialState(false)
                .build();
        tester.testLoadFlow(microBE, validation);
    }

    @Test
    public void microNL() {
        TestGridModel microNL = catalog.microNL();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(microNL.path())
                .writeNetworksInputsResults(true)
                .validateInitialState(true)
                .specificCompatibility(true)
                // But two generators have Q out of allowed interval (1dc9afba and 2844585c)
                // In both cases Q = -26.68 when [Qmin, Qmax] = [0, 200]
                .maxGeneratorsFailInitialState(2)
                .compareWithInitialState(true)
                .maxGeneratorsFailComputedState(2)
                .build();
        tester.testLoadFlow(microNL, validation);
    }

    @Test
    public void microAssembled() {
        TestGridModel microAssembled = catalog.microAssembled();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(microAssembled.path())
                .writeNetworksInputsResults(true)
                .validateInitialState(true)
                .specificCompatibility(true)
                // Validation considerations from microNL and microBE apply here
                .threshold(1.45f)
                .maxBusesFailInitialState(1)
                .maxGeneratorsFailInitialState(3)
                .compareWithInitialState(true)
                .maxBusesFailComputedState(1)
                .maxGeneratorsFailComputedState(3)
                .build();
        tester.testLoadFlow(microAssembled, validation);
    }

    @Test
    public void miniBusBranch() {
        TestGridModel miniBusBranch = catalog.miniBusBranch();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(miniBusBranch.path())
                .writeNetworksInputsResults(true)
                .validateInitialState(true)
                .compareWithInitialState(false)
                .build();
        tester.testLoadFlow(miniBusBranch, validation);
    }

    @Test
    public void miniNodeBreaker() {
        TestGridModel miniNodeBreaker = catalog.miniNodeBreaker();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(miniNodeBreaker.path())
                .writeNetworksInputsResults(true)
                .validateInitialState(true)
                .compareWithInitialState(false)
                .build();
        tester.testLoadFlow(miniNodeBreaker, validation);
    }

    @Test
    public void smallBusBranch() {

        // For two-winding transformers LF results have been computed
        // considering half the total magnetizing branch susceptance
        // as shunt admittance to ground at each end of the transformer PI model.
        // Only transformer with b != 0 is at substation "Vale of Arryn".

        // There are two mismatches at bus balance > 0.1:
        // Substation Waymoot, TopologicalNode Fieldale (Q mismatch = 0.1710)
        // Substation Qarth, TopologicalNode EastLima1 (Q mismatch = 0.1441)
        // This is the reason to raise the threshold to 0.1711

        TestGridModel smallBusBranch = catalog.smallBusBranch();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(smallBusBranch.path())
                .writeNetworksInputsResults(true)
                .validateInitialState(true)
                .threshold(0.1711f)
                .specificCompatibility(true)
                .compareWithInitialState(true)
                .build();
        tester.testLoadFlow(smallBusBranch, validation);
    }

    @Test
    public void smallNodeBreaker() {
        // Same considerations made for bus-branch model apply here
        TestGridModel smallNodeBreaker = catalog.smallNodeBreaker();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(smallNodeBreaker.path())
                .writeNetworksInputsResults(true)
                .validateInitialState(true)
                .threshold(0.1711f)
                .specificCompatibility(true)
                .compareWithInitialState(true)
                .build();
        tester.testLoadFlow(smallNodeBreaker, validation);
    }

    @Test
    public void real() {
        TestGridModel real = catalog.real();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(real.path())
                .writeNetworksInputsResults(true)
                .validateInitialState(true)
                .changeSignForShuntReactivePowerFlowInitialState(true)
                // Doubts about SV data corresponding to a solved state
                .threshold(10.0f)
                .maxBusesFailInitialState(62)
                .compareWithInitialState(true)
                .maxBusesFailComputedState(62)
                .build();
        tester.testLoadFlow(real, validation);
    }

    private static CgmesConformity1Catalog catalog;
    private static LoadFlowTester         tester;
}
