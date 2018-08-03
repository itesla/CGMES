package com.powsybl.cgmes.validation.test.csi.dacf.d20180221.bus_branch;

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

import com.powsybl.cgmes.validation.test.LoadFlowTester;
import com.powsybl.cgmes.validation.test.LoadFlowValidation;
import com.powsybl.triplestore.TripleStoreFactory;

public class AstLoadFlowTest {
    @BeforeClass
    public static void setUp() {
        catalog = new AstCasesCatalog();
        tester = new LoadFlowTester(
                TripleStoreFactory.onlyDefaultImplementation(),
                new LoadFlowValidation.Builder()
                        .writeNetworksInputsResults(true)
                        .validateInitialState(true)
                        .compareWithInitialState(true)
                        .build());
    }

    @Test
    public void ast20180221T0130Z() {
        tester.testLoadFlow(catalog.ast20180221T0130Z());
    }

    @Test
    public void ast20180221T1530Z() {
        tester.testLoadFlow(catalog.ast20180221T1530Z());
    }

    @Test
    public void ast20180221T2130Z() {
        tester.testLoadFlow(catalog.ast20180221T2130Z());
    }

    @Test
    public void ast20180220T2330Z() {
        tester.testLoadFlow(catalog.ast20180220T2330Z());
    }

    @Test
    public void ast20180221T0730Z() {
        tester.testLoadFlow(catalog.ast20180221T0730Z());
    }

    @Test
    public void ast20180221T1730Z() {
        tester.testLoadFlow(catalog.ast20180221T1730Z());
    }

    @Test
    public void ast20180221T2030Z() {
        tester.testLoadFlow(catalog.ast20180221T2030Z());
    }

    @Test
    public void ast20180221T0030Z() {
        tester.testLoadFlow(catalog.ast20180221T0030Z());
    }

    @Test
    public void ast20180221T1630Z() {
        tester.testLoadFlow(catalog.ast20180221T1630Z());
    }

    @Test
    public void ast20180221T0230Z() {
        tester.testLoadFlow(catalog.ast20180221T0230Z());
    }

    @Test
    public void ast20180221T0330Z() {
        tester.testLoadFlow(catalog.ast20180221T0330Z());
    }

    @Test
    public void ast20180221T1230Z() {
        tester.testLoadFlow(catalog.ast20180221T1230Z());
    }

    @Test
    public void ast20180221T0630Z() {
        tester.testLoadFlow(catalog.ast20180221T0630Z());
    }

    @Test
    public void ast20180221T0830Z() {
        tester.testLoadFlow(catalog.ast20180221T0830Z());
    }

    @Test
    public void ast20180221T1430Z() {
        tester.testLoadFlow(catalog.ast20180221T1430Z());
    }

    @Test
    public void ast20180221T0430Z() {
        tester.testLoadFlow(catalog.ast20180221T0430Z());
    }

    @Test
    public void ast20180221T1330Z() {
        tester.testLoadFlow(catalog.ast20180221T1330Z());
    }

    @Test
    public void ast20180221T0530Z() {
        tester.testLoadFlow(catalog.ast20180221T0530Z());
    }

    @Test
    public void ast20180221T1830Z() {
        tester.testLoadFlow(catalog.ast20180221T1830Z());
    }

    @Test
    public void ast20180221T0930Z() {
        tester.testLoadFlow(catalog.ast20180221T0930Z());
    }

    @Test
    public void ast20180221T2230Z() {
        tester.testLoadFlow(catalog.ast20180221T2230Z());
    }

    @Test
    public void ast20180221T1130Z() {
        tester.testLoadFlow(catalog.ast20180221T1130Z());
    }

    @Test
    public void ast20180221T1030Z() {
        tester.testLoadFlow(catalog.ast20180221T1030Z());
    }

    @Test
    public void ast20180221T1930Z() {
        tester.testLoadFlow(catalog.ast20180221T1930Z());
    }

    private static AstCasesCatalog catalog;
    private static LoadFlowTester  tester;
}
