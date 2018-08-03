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

import com.powsybl.cgmes.conversion.test.ConversionTester;
import com.powsybl.cgmes.conversion.test.network.compare.ComparisonConfig;
import com.powsybl.triplestore.TripleStoreFactory;

public class ApgConversionTest {
    @BeforeClass
    public static void setUp() {
        actuals = new ApgCasesCatalog();
        tester = new ConversionTester(
                TripleStoreFactory.onlyDefaultImplementation(),
                new ComparisonConfig()
                        .checkNetworkId(false)
                        .checkVoltageLevelLimits(false)
                        .checkGeneratorReactiveCapabilityCurve(false)
                        .checkGeneratorRegulatingTerminal(false));
    }

    @Test
    public void apg20180221T0130Z() {
        tester.testConversion(null, actuals.apg20180221T0130Z());
    }

    @Test
    public void apg20180221T1530Z() {
        tester.testConversion(null, actuals.apg20180221T1530Z());
    }

    @Test
    public void apg20180221T2130Z() {
        tester.testConversion(null, actuals.apg20180221T2130Z());
    }

    @Test
    public void apg20180220T2330Z() {
        tester.testConversion(null, actuals.apg20180220T2330Z());
    }

    @Test
    public void apg20180221T0730Z() {
        tester.testConversion(null, actuals.apg20180221T0730Z());
    }

    @Test
    public void apg20180221T1730Z() {
        tester.testConversion(null, actuals.apg20180221T1730Z());
    }

    @Test
    public void apg20180221T2030Z() {
        tester.testConversion(null, actuals.apg20180221T2030Z());
    }

    @Test
    public void apg20180221T0030Z() {
        tester.testConversion(null, actuals.apg20180221T0030Z());
    }

    @Test
    public void apg20180221T1630Z() {
        tester.testConversion(null, actuals.apg20180221T1630Z());
    }

    @Test
    public void apg20180221T0230Z() {
        tester.testConversion(null, actuals.apg20180221T0230Z());
    }

    @Test
    public void apg20180221T0330Z() {
        tester.testConversion(null, actuals.apg20180221T0330Z());
    }

    @Test
    public void apg20180221T1230Z() {
        tester.testConversion(null, actuals.apg20180221T1230Z());
    }

    @Test
    public void apg20180221T0630Z() {
        tester.testConversion(null, actuals.apg20180221T0630Z());
    }

    @Test
    public void apg20180221T0830Z() {
        tester.testConversion(null, actuals.apg20180221T0830Z());
    }

    @Test
    public void apg20180221T1430Z() {
        tester.testConversion(null, actuals.apg20180221T1430Z());
    }

    @Test
    public void apg20180221T0430Z() {
        tester.testConversion(null, actuals.apg20180221T0430Z());
    }

    @Test
    public void apg20180221T1330Z() {
        tester.testConversion(null, actuals.apg20180221T1330Z());
    }

    @Test
    public void apg20180221T0530Z() {
        tester.testConversion(null, actuals.apg20180221T0530Z());
    }

    @Test
    public void apg20180221T1830Z() {
        tester.testConversion(null, actuals.apg20180221T1830Z());
    }

    @Test
    public void apg20180221T0930Z() {
        tester.testConversion(null, actuals.apg20180221T0930Z());
    }

    @Test
    public void apg20180221T2230Z() {
        tester.testConversion(null, actuals.apg20180221T2230Z());
    }

    @Test
    public void apg20180221T1130Z() {
        tester.testConversion(null, actuals.apg20180221T1130Z());
    }

    @Test
    public void apg20180221T1030Z() {
        tester.testConversion(null, actuals.apg20180221T1030Z());
    }

    @Test
    public void apg20180221T1930Z() {
        tester.testConversion(null, actuals.apg20180221T1930Z());
    }

    private static ApgCasesCatalog  actuals;
    private static ConversionTester tester;
}
