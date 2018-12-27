package com.powsybl.cgmes.validation.test.conformity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

import com.powsybl.cgmes.conversion.test.DebugPhaseTapChanger;
import com.powsybl.cgmes.model.CgmesModelException;
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
public class CgmesConformity2LoadFlowTest {
    @BeforeClass
    public static void setUp() throws IOException {
        catalog = new CgmesConformity2Catalog();
        tester = new LoadFlowTester(TripleStoreFactory.onlyDefaultImplementation());
        working = Files.createTempDirectory("temp-conformity2-loadflow-test-");
    }

    @Test
    public void entsoeExplicitLoadFlow() throws IOException {
        tester.testLoadFlow(catalog.entsoeExplicitLoadFlow());
    }

    @Test
    public void transformerLineTest() throws IOException {
        TestGridModel t = catalog.transformerLineTest();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(working.resolve(t.name()))
                .writeNetworksInputsResults(true)
                // The case is not solved,
                // relevant only for producing warnings about data?
                .validateInitialState(false)
                .compareWithInitialState(false)
                .build();
        tester.testLoadFlow(t, validation);
    }

    @Test
    public void reportMicroBaseCaseBE() throws IOException {
        TestGridModel t = catalog.microBaseCaseBE();
        try {
            new ReportBusBalances("microBaseCaseBE", t)
                    .setStrict(false)
                    .setConsiderPhaseAngleClock(false)
                    .setIgnoreBusesWithPhaseTapChanges(true)
                    .setConsiderRatioTapChangersFor3wTxAtNetworkSide(true)
                    .setLowImpedanceLine(0.0, 0.0)
                    .report();
        } catch (CgmesModelException x) {
            System.err.println(x);
        }
    }

    @Test
    public void microBaseCaseBE() throws IOException {
        TestGridModel t = catalog.microBaseCaseBE();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(working.resolve(t.name()))
                .writeNetworksInputsResults(true)
                .specificCompatibility(true)
                .validateInitialState(true)
                .maxBusesFailInitialState(1)
                .maxGeneratorsFailInitialState(1)
                .maxBusesFailComputedState(1)
                .maxGeneratorsFailComputedState(1)
                .debugNetwork(network -> new DebugPhaseTapChanger(
                        network.getTwoWindingsTransformer("_a708c3bc-465d-4fe7-b6ef-6fa6408a62b0"),
                        2,
                        new PowerFlow(-55.08795, 221.867401))
                                .debug())
                .build();
        tester.testLoadFlow(t, validation);
    }

    @Test
    public void microBaseCaseAssembled() throws IOException {
        TestGridModel t = catalog.microBaseCaseAssembled();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(working.resolve(t.name()))
                .writeNetworksInputsResults(true)
                .specificCompatibility(true)
                .validateInitialState(true)
                .maxBusesFailInitialState(1)
                .maxGeneratorsFailInitialState(3)
                .maxBusesFailComputedState(1)
                .maxGeneratorsFailComputedState(3)
                .build();
        tester.testLoadFlow(t, validation);
    }

    // FIXME(Luma): review results @Test
    public void real() throws IOException {
        TestGridModel t = catalog.real();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(working.resolve(t.name()))
                .writeNetworksInputsResults(true)
                .validateInitialState(true)
                .changeSignForShuntReactivePowerFlowInitialState(true)
                // Doubts about SV data corresponding to a solved state
                .threshold(10.0)
                .maxBusesFailInitialState(62)
                .compareWithInitialState(true)
                .maxBusesFailComputedState(62)
                .build();
        tester.testLoadFlow(t, validation);
    }

    private static CgmesConformity2Catalog catalog;
    private static LoadFlowTester tester;
    private static Path working;
}
