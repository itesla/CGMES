package com.powsybl.cgmes.validation.test.csi;

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

import com.powsybl.cgmes.test.csi.OtherCasesCatalog;
import com.powsybl.cgmes.validation.test.LoadFlowTester;
import com.powsybl.cgmes.validation.test.LoadFlowValidation;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class OtherCasesLoadFlowTest {
    @BeforeClass
    public static void setUp() {
        catalog = new OtherCasesCatalog();
        tester = new LoadFlowTester(
                TripleStoreFactory.implementationsWorkingWithNestedGraphClauses(),
                new LoadFlowValidation.Builder()
                        .writeNetworksInputsResults(true)
                        .validateInitialState(false)
                        .compareWithInitialState(false)
                        .maxGeneratorsFailInitialState(5)
                        .build());
    }

    @Test
    public void eu201711140000Cgmes() {
        tester.testLoadFlow(catalog.eu201711140000Cgmes());
    }

    private static OtherCasesCatalog catalog;
    private static LoadFlowTester    tester;
}
