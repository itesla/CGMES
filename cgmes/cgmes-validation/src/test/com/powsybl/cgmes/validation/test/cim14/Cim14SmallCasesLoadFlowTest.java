package com.powsybl.cgmes.validation.test.cim14;

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

import com.powsybl.cgmes.test.TestGridModel;
import com.powsybl.cgmes.test.cim14.Cim14SmallCasesCatalog;
import com.powsybl.cgmes.validation.test.LoadFlowTester;
import com.powsybl.cgmes.validation.test.LoadFlowValidation;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class Cim14SmallCasesLoadFlowTest {
    @BeforeClass
    public static void setUp() {
        catalog = new Cim14SmallCasesCatalog();
        tester = new LoadFlowTester(TripleStoreFactory.allImplementations());
    }

    @Test
    public void smallcase1() {
        tester.testLoadFlow(catalog.small1());
    }

    @Test
    public void txMicroBEAdapted() {
        TestGridModel tm = catalog.txMicroBEAdapted();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .workingDirectory(tm.path())
                .writeNetworksInputsResults(true)
                .specificCompatibility(true)
                .validateInitialState(true)
                .build();
        tester.testLoadFlow(tm, validation);
    }

    @Test
    public void ieee14() {
        tester.testLoadFlow(catalog.ieee14());
    }

    @Test
    public void nordic32() {
        tester.testLoadFlow(catalog.nordic32());
    }

    @Test
    public void m7buses() {
        tester.testLoadFlow(catalog.m7buses());
    }

    @Test
    public void m7busesBiggerZ() {
        // A variant of m7buses that uses bigger values in impedances
        // Relatively small values of impedances in original m7buses
        // could lead to precision problems if voltages are not stored
        // with enough precision
        tester.testLoadFlow(catalog.m7busesBiggerZ());
    }

    private static Cim14SmallCasesCatalog catalog;
    private static LoadFlowTester         tester;
}
