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

import com.powsybl.cgmes.conversion.test.ConversionTester;
import com.powsybl.cgmes.conversion.test.network.compare.ComparisonConfig;
import com.powsybl.cgmes_conformity.test.CgmesConformityCatalog;
import com.powsybl.cgmes_conformity.test.CgmesConformityNetworkCatalog;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesConformityConversionTest {
    @BeforeClass
    public static void setUp() {
        actuals = new CgmesConformityCatalog();
        expecteds = new CgmesConformityNetworkCatalog();
        tester = new ConversionTester(
                TripleStoreFactory.onlyDefaultImplementation(),
                new ComparisonConfig());
    }

    @Test
    public void microBE() {
        tester.testConversion(expecteds.microBE(), actuals.microBE());
    }

    @Test
    public void microNL() {
        tester.testConversion(expecteds.microNL(), actuals.microNL());
    }

    @Test
    public void microAssembled() {
        tester.testConversion(expecteds.microAssembled(), actuals.microAssembled());
    }

    @Test
    public void miniBusBranch() {
        tester.testConversion(expecteds.miniBusBranch(), actuals.miniBusBranch());
    }

    @Test
    public void miniNodeBreaker() {
        tester.testConversion(expecteds.miniNodeBreaker(), actuals.miniNodeBreaker());
    }

    @Test
    public void smallBusBranch() {
        tester.testConversion(expecteds.smallBusBranch(), actuals.smallBusBranch());
    }

    @Test
    public void smallNodeBreaker() {
        tester.testConversion(expecteds.smallNodeBreaker(), actuals.smallNodeBreaker());
    }

    @Test
    public void real() {
        tester.testConversion(expecteds.real(), actuals.real());
    }

    private static CgmesConformityCatalog        actuals;
    private static CgmesConformityNetworkCatalog expecteds;
    private static ConversionTester              tester;
}
