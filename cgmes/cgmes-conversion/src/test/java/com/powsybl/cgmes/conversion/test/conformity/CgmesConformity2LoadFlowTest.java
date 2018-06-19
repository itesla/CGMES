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

import com.powsybl.cgmes.conversion.test.LoadFlowTester;
import com.powsybl.cgmes.conversion.test.LoadFlowValidation;
import com.powsybl.cgmes.test.TestGridModel;
import com.powsybl.cgmes_conformity.test.CgmesConformity2Catalog;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesConformity2LoadFlowTest {
    @BeforeClass
    public static void setUp() {
        catalog = new CgmesConformity2Catalog();
        tester = new LoadFlowTester(TripleStoreFactory.onlyDefaultImplementation());
    }

    @Test
    public void microBaseCaseBE() {
        TestGridModel tm = catalog.microBaseCaseBE();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(tm.path())
                .writeNetworksInputsResults(true)
                .specificCompatibility(true)
                .validateInitialState(true)
                .maxBusesFailInitialState(1)
                .maxGeneratorsFailInitialState(1)
                .maxBusesFailComputedState(1)
                .maxGeneratorsFailComputedState(1)
                .build();
        tester.testLoadFlow(tm, validation);
    }

    @Test
    public void microBaseCaseAssembled() {
        TestGridModel tm = catalog.microBaseCaseAssembled();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(tm.path())
                .writeNetworksInputsResults(true)
                .specificCompatibility(true)
                .validateInitialState(true)
                .maxBusesFailInitialState(1)
                .maxGeneratorsFailInitialState(3)
                .maxBusesFailComputedState(1)
                .maxGeneratorsFailComputedState(3)
                .build();
        tester.testLoadFlow(tm, validation);
    }

    private static CgmesConformity2Catalog catalog;
    private static LoadFlowTester          tester;
}
