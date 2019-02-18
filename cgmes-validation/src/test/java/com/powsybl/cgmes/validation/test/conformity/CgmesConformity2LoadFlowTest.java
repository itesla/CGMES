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

import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.cgmes.validation.test.loadflow.LoadFlowTester;
import com.powsybl.cgmes.validation.test.loadflow.LoadFlowValidation;
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
                // The case is not solved if we take into account reactive injections
                // coming from the two-winding transformers
                .validateInitialState(false)
                .compareWithInitialState(false)
                .build();
        tester.testLoadFlow(t, validation);
    }

    private static CgmesConformity2Catalog catalog;
    private static LoadFlowTester tester;
    private static Path working;
}
