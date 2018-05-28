package com.powsybl.cgmes.alternatives.test;

/*
 * #%L
 * CGMES Model Alternatives
 * %%
 * Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.CgmesModelException;
import com.powsybl.cgmes.alternatives.test.AlternativeQueriesTester.Expected;
import com.powsybl.cgmes.test.TestGridModel;
import com.powsybl.cgmes.test.csi.RteCasesCatalog;
import com.powsybl.triplestore.PropertyBags;
import com.powsybl.triplestore.QueryCatalog;
import com.powsybl.triplestore.TripleStoreFactory;

import cern.colt.Arrays;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class AlternativeQueriesForVoltageLevels {

    @BeforeClass
    public static void setUp()  {
        TestGridModel model = new RteCasesCatalog().fr201707041430Cgmes();
        Expected expected = new Expected()
                .resultSize(6221)
                .propertyCount("nominalVoltage", 6221);

        int experiments = 1;
        boolean doAssert = true;
        Consumer<PropertyBags> consumer = AlternativeQueriesForVoltageLevels::checkBaseVoltagesMissingNominalVoltage;

        tester = new AlternativeQueriesTester(
                TripleStoreFactory.implementationsAllowingNestedGraphClauses(),
                new QueryCatalog("voltageLevels.sparql"),
                model,
                expected,
                experiments,
                doAssert,
                consumer);

        try {
            tester.load();
            isDataAvailable = true;
        } catch (CgmesModelException x) {
            isDataAvailable = false;
        }
    }

    @Test
    public void testBaseVoltages() {
        if (!isDataAvailable) {
            return;
        }
        Expected expected = new Expected()
                .resultSize(29)
                .propertyCount("nominalVoltage", 29);
        Consumer<PropertyBags> consumer = r -> r.stream()
                .forEach(bv -> LOG.info("{} {}", bv.getId("BaseVoltage"),
                        bv.asFloat("nominalVoltage")));
        tester.test("baseVoltages", expected, consumer);
    }

    @Test
    public void testUsingGraph() {
        if (!isDataAvailable) {
            return;
        }
        tester.test("usingGraphClauses");
    }

    @Test
    public void noGraphClauses() {
        if (!isDataAvailable) {
            return;
        }
        tester.test("noGraphClauses");
    }

    public static void checkBaseVoltagesMissingNominalVoltage(PropertyBags r) {
        Set<String> bvs = new HashSet<>();
        r.stream()
                .filter(vl -> !vl.containsKey("nominalVoltage"))
                .forEach(vl -> bvs.add(vl.getId("BaseVoltage")));
        LOG.info("Base Voltages missing nominal voltage : {}", Arrays.toString(bvs.toArray()));
    }

    private static AlternativeQueriesTester tester;
    private static boolean                  isDataAvailable;

    private static final Logger             LOG = LoggerFactory
            .getLogger(AlternativeQueriesForVoltageLevels.class);
}
