package com.powsybl.cgmes.conversion.test.cim14;

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

import com.powsybl.cgmes.conversion.test.ConversionTester;
import com.powsybl.cgmes.conversion.test.network.compare.ComparisonConfig;
import com.powsybl.cgmes.test.cim14.Cim14SmallCasesCatalog;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class Cim14SmallCasesConversionTest {
    @BeforeClass
    public static void setUp() {
        actuals = new Cim14SmallCasesCatalog();
        expected = new Cim14SmallCasesNetworkCatalog();
        tester = new ConversionTester(
                TripleStoreFactory.allImplementations(),
                new ComparisonConfig()
                        .checkNetworkId(false)
                        // Expected cases are read using CIM1Importer, that uses floats to read numbers
                        // IIDM and CGMES now stores numbers as doubles
                        .tolerance(2.4e-4));
    }

    @Test
    public void txMicroBEAdapted() {
        tester.testConversion(expected.txMicroBEAdapted(), actuals.txMicroBEAdapted());
    }

    @Test
    public void smallcase1() {
        tester.testConversion(expected.smallcase1(), actuals.small1());
    }

    @Test
    public void ieee14() {
        tester.testConversion(expected.ieee14(), actuals.ieee14());
    }

    @Test
    public void ieee14zipped() {
        tester.testConversion(expected.ieee14(), actuals.ieee14zipped());
    }

    @Test
    public void nordic32() {
        tester.testConversion(expected.nordic32(), actuals.nordic32());
    }

    @Test
    public void m7buses() {
        tester.testConversion(expected.m7buses(), actuals.m7buses());
    }

    private static Cim14SmallCasesCatalog        actuals;
    private static Cim14SmallCasesNetworkCatalog expected;
    private static ConversionTester              tester;
}
