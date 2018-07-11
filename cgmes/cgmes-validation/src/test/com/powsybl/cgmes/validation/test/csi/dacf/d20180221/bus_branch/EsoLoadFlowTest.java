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

import com.powsybl.cgmes.conversion.test.csi.dacf.d20180221.bus_branch.EsoCasesCatalog;
import com.powsybl.cgmes.validation.test.LoadFlowTester;
import com.powsybl.cgmes.validation.test.LoadFlowValidation;
import com.powsybl.triplestore.TripleStoreFactory;

public class EsoLoadFlowTest {
    @BeforeClass
    public static void setUp() {
        catalog = new EsoCasesCatalog();
        tester = new LoadFlowTester(
                TripleStoreFactory.onlyDefaultImplementation(),
                new LoadFlowValidation.Builder()
                        .writeNetworksInputsResults(true)
                        .validateInitialState(true)
                        .compareWithInitialState(true)
                        .build());
    }

    @Test
    public void eso20180221T0930Z() {
        tester.testLoadFlow(catalog.eso20180221T0930Z());
    }

    @Test
    public void eso20180221T0030Z() {
        tester.testLoadFlow(catalog.eso20180221T0030Z());
    }

    @Test
    public void eso20180220T2330Z() {
        tester.testLoadFlow(catalog.eso20180220T2330Z());
    }

    private static EsoCasesCatalog catalog;
    private static LoadFlowTester  tester;
}
