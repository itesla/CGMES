package com.powsybl.cgmes.test;

/*
 * #%L
 * CGMES data model
 * %%
 * Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.CgmesModel;
import com.powsybl.cgmes.triplestore.CgmesModelTripleStore;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.triplestore.AbstractPowsyblTripleStore;
import com.powsybl.triplestore.PropertyBags;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesModelTester {

    public CgmesModelTester(TestGridModel gm) {
        this.gridModel = gm;
    }

    public void test() {
        for (String impl : TripleStoreFactory.allImplementations()) {
            CgmesModel actual = load(impl);
            test(gridModel.expected(), actual);
        }
    }

    private CgmesModel load(String impl) {
        ReadOnlyDataSource ds = DataSourceUtil.createDataSource(
                gridModel.path(),
                gridModel.basename(),
                gridModel.getCompressionExtension(),
                null);
        AbstractPowsyblTripleStore tripleStore = TripleStoreFactory.create(impl);
        CgmesModelTripleStore actual = new CgmesModelTripleStore(ds, tripleStore);
        actual.load();
        actual.dump(line -> LOG.info(line));
        return actual;
    }

    private void test(CgmesModel expected, CgmesModel actual) {
        assertEquals(gridModel.expected().version(), actual.version());
        assertEquals(gridModel.expected().isNodeBreaker(), actual.isNodeBreaker());
        // Model id is not checked
        LOG.info("ignoring model identifiers expected {}, actual {}",
                expected.modelId(),
                actual.modelId());
        testPropertyBags(expected.substations(), actual.substations());
        testPropertyBags(expected.voltageLevels(), actual.voltageLevels());
        testPropertyBags(expected.terminals(), actual.terminals());
        testPropertyBags(expected.terminalLimits(), actual.terminalLimits());
        testPropertyBags(expected.topologicalNodes(), actual.topologicalNodes());
        testPropertyBags(expected.switches(), actual.switches());
        testPropertyBags(expected.acLineSegments(), actual.acLineSegments());
        testPropertyBags(expected.transformerEnds(), actual.transformerEnds());
        testPropertyBags(expected.ratioTapChangers(), actual.ratioTapChangers());
        testPropertyBags(expected.phaseTapChangers(), actual.phaseTapChangers());
        testPropertyBags(expected.energyConsumers(), actual.energyConsumers());
        testPropertyBags(expected.shuntCompensators(), actual.shuntCompensators());
        testPropertyBags(expected.staticVarCompensators(), actual.staticVarCompensators());
        testPropertyBags(expected.synchronousMachines(), actual.synchronousMachines());
        testPropertyBags(expected.asynchronousMachines(), actual.asynchronousMachines());
    }

    private void testPropertyBags(PropertyBags expecteds, PropertyBags actuals) {
        if (actuals.size() > 0) {
            // Assume the first property name in expected is the main property (id)
            // that we want to extract from actuals to debug current set of actuals
            if (expecteds.size() > 0) {
                if (LOG.isDebugEnabled()) {
                    String debugPropertyName = expecteds.get(0).propertyNames().get(0);
                    String[] debugValues = actuals.pluck(debugPropertyName);
                    LOG.debug("Actuals:");
                    LOG.debug(Arrays.toString(debugValues));
                }
            } else {
                String names = String.join(",", actuals.get(0).propertyNames());
                LOG.warn("Unexpected actuals with properties {}", names);
            }
        }

        // Check that all property values in expected are present in actuals and have the same value
        assertNotNull(actuals);
        assertEquals(expecteds.size(), actuals.size());
        if (expecteds.size() == 0) {
            return;
        }
        List<String> expectedPropertyNames = expecteds.get(0).propertyNames();
        expectedPropertyNames.stream().forEach(p -> {
            assertArrayEquals(expecteds.pluck(p), actuals.pluck(p));
        });
    }

    private final TestGridModel gridModel;

    private static final Logger LOG = LoggerFactory.getLogger(CgmesModelTester.class);
}
