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

import com.powsybl.cgmes.test.csi.RteCasesCatalog;
import com.powsybl.cgmes.validation.test.LoadFlowTester;
import com.powsybl.cgmes.validation.test.LoadFlowValidation;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class RteCasesLoadFlowTest {
    @BeforeClass
    public static void setUp() {
        catalog = new RteCasesCatalog();
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
    public void fr201303151230Cim14() {
        tester.testLoadFlow(catalog.fr201303151230());
    }

    @Test
    public void fr201707041430Cim14() {
        tester.testLoadFlow(catalog.fr201707041430Cim14());
    }

    @Test
    public void fr201707041430Cgmes() {
        tester.testLoadFlow(catalog.fr201707041430Cgmes());
    }

    private static RteCasesCatalog catalog;
    private static LoadFlowTester  tester;
}
