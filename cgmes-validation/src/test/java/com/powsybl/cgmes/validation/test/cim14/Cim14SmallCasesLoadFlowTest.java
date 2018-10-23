package com.powsybl.cgmes.validation.test.cim14;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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

import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.cgmes.model.test.TestGridModelPath;
import com.powsybl.cgmes.model.test.cim14.Cim14SmallCasesCatalog;
import com.powsybl.cgmes.validation.test.LoadFlowTester;
import com.powsybl.cgmes.validation.test.LoadFlowValidation;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class Cim14SmallCasesLoadFlowTest {
    @BeforeClass
    public static void setUp() {
        catalog = new Cim14SmallCasesCatalog();
        tester = new LoadFlowTester(TripleStoreFactory.implementationsWorkingWithNestedGraphClauses());
    }

    @Test
    public void smallcase1() throws IOException {
        tester.testLoadFlow(catalog.small1());
    }

    @Test
    public void txMicroBEAdapted() throws IOException {
        TestGridModel tm = catalog.txMicroBEAdapted();
        LoadFlowValidation validation = new LoadFlowValidation.Builder()
                .writeNetworksInputsResults(true)
                .specificCompatibility(true)
                .validateInitialState(true)
                .build();
        tester.testLoadFlow(tm, validation);
    }

    @Test
    public void ieee14() throws IOException {
        tester.testLoadFlow(catalog.ieee14());
    }

    @Test
    public void nordic32() throws IOException {
        tester.testLoadFlow(catalog.nordic32());
    }

    @Test
    public void m7buses() throws IOException {
        tester.testLoadFlow(catalog.m7buses());
    }

    @Test
    public void m7busesBiggerZ() throws IOException {
        // A variant of m7buses that uses bigger values in impedances
        // Relatively small values of impedances in original m7buses
        // could lead to precision problems if voltages are not stored
        // with enough precision
        Path p = Paths.get("../data/cim14/m7buses_bigger_z");
        TestGridModel t = new TestGridModelPath(p, "m7buses", null);
        tester.testLoadFlow(t);
    }

    private static Cim14SmallCasesCatalog catalog;
    private static LoadFlowTester         tester;
}
