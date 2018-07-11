package com.powsybl.cgmes.validation.test.csi.dacf.d20180221.node_breaker;

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

import com.powsybl.cgmes.conversion.test.csi.dacf.d20180221.node_breaker.LitgridCasesCatalog;
import com.powsybl.cgmes.test.TestGridModel;
import com.powsybl.cgmes.validation.test.LoadFlowTester;
import com.powsybl.cgmes.validation.test.LoadFlowValidation;
import com.powsybl.triplestore.TripleStoreFactory;

public class LitgridLoadFlowTest {
    @BeforeClass
    public static void setUp() {
        catalog = new LitgridCasesCatalog();
    }

    @Test
    public void litgrid20180220T2330Z() {
        TestGridModel t = catalog.litgrid20180220T2330Z();
        loadFlowTesterFor(t).testLoadFlow(t);
    }

    private LoadFlowTester loadFlowTesterFor(TestGridModel gm) {
        return new LoadFlowTester(
                TripleStoreFactory.onlyDefaultImplementation(),
                new LoadFlowValidation.Builder()
                        .workingDirectory(gm.path())
                        .writeNetworksInputsResults(true)
                        .validateInitialState(true)
                        .compareWithInitialState(true)
                        .build());
    }

    private static LitgridCasesCatalog catalog;
}
