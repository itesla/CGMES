package com.powsybl.cgmes.conversion.test.csi;

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
import com.powsybl.cgmes.test.csi.ApgCasesCatalog;
import com.powsybl.cgmes.test.csi.OtherCasesCatalog;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class OtherCasesConversionTest {
    @BeforeClass
    public static void setUp() {
        actuals = new OtherCasesCatalog();
        tester = new ConversionTester(
                TripleStoreFactory.onlyDefaultImplementation(),
                new ComparisonConfig()
                        .checkNetworkId(false)
                        .checkVoltageLevelLimits(false)
                        .checkGeneratorReactiveCapabilityCurve(false)
                        .checkGeneratorRegulatingTerminal(false));
    }

    @Test
    public void eu201711140000Cgmes()  {
        tester.testConversion(null, actuals.eu201711140000Cgmes());
    }

    @Test
    public void ba201709050930Cgmes()  {
        tester.testConversion(null, actuals.ba201709050930Cgmes());
    }

    @Test
    public void bg201709051130Cgmes()  {
        tester.testConversion(null, actuals.bg201709051130Cgmes());
    }

    @Test
    public void hu201709051130Cgmes()  {
        tester.testConversion(null, actuals.hu201709051130Cgmes());
    }

    @Test
    public void rs201709050930Cgmes()  {
        tester.testConversion(null, actuals.rs201709050930Cgmes());
    }

    @Test
    public void apgBD291120171D()  {
        // TODO move out to specific test class
        tester.testConversion(null, new ApgCasesCatalog().apgBD291120171D());
    }

    private static OtherCasesCatalog actuals;
    private static ConversionTester  tester;
}
