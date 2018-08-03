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

public class EsoConversionTest {
    @BeforeClass
    public static void setUp() {
        actuals = new EsoCasesCatalog();
        tester = new ConversionTester(
                TripleStoreFactory.onlyDefaultImplementation(),
                new ComparisonConfig()
                        .checkNetworkId(false)
                        .checkVoltageLevelLimits(false)
                        .checkGeneratorReactiveCapabilityCurve(false)
                        .checkGeneratorRegulatingTerminal(false));
    }

    @Test
    public void eso20180221T0930Z() {
        tester.testConversion(null, actuals.eso20180221T0930Z());
    }

    @Test
    public void eso20180221T0030Z() {
        tester.testConversion(null, actuals.eso20180221T0030Z());
    }

    @Test
    public void eso20180220T2330Z() {
        tester.testConversion(null, actuals.eso20180220T2330Z());
    }

    private static EsoCasesCatalog  actuals;
    private static ConversionTester tester;
}
