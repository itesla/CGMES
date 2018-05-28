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

import java.util.function.Consumer;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.CgmesModelException;
import com.powsybl.cgmes.alternatives.test.AlternativeQueriesTester.Expected;
import com.powsybl.cgmes.test.TestGridModel;
import com.powsybl.cgmes.test.csi.ApgCasesCatalog;
import com.powsybl.triplestore.PropertyBags;
import com.powsybl.triplestore.QueryCatalog;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class AlternativeQueriesForTopologicalNodes {

    @BeforeClass
    public static void setUp()  {
        model = new ApgCasesCatalog().apgBD291120171D();
        numTopoNodesModel = 607;
        numTopoNodesWithSvVoltageInBoundary = 35;
        numMissingTopoNodes = 7;
        numSvVoltages = numTopoNodesModel
                + numTopoNodesWithSvVoltageInBoundary
                + numMissingTopoNodes;

        int experiments = 1;
        boolean doAssert = true;
        tester = new AlternativeQueriesTester(
                TripleStoreFactory.onlyDefaultImplementation(),
                new QueryCatalog("topologicalNodes.sparql"),
                model,
                new Expected(),
                experiments,
                doAssert,
                null);
        try {
            tester.load();
            isDataAvailable = true;
        } catch (CgmesModelException x) {
            isDataAvailable = false;
        }
    }

    @Test
    public void testTopoNodes() {
        if (!isDataAvailable) {
            return;
        }
        Expected expected = new Expected().resultSize(numTopoNodesModel);
        tester.test("topoNodes", expected);
    }

    @Test
    public void testTopoNodesSvVoltages() {
        if (!isDataAvailable) {
            return;
        }
        Expected expected = new Expected().resultSize(numTopoNodesModel);
        tester.test("topoNodesSvVoltages", expected);
    }

    @Test
    public void testSvVoltagesForTopoNodes() {
        if (!isDataAvailable) {
            return;
        }
        Expected expectedSv = new Expected().resultSize(numSvVoltages);
        tester.test("SvVoltagesForTopoNodes", expectedSv);
    }

    @Test
    public void testSvVoltagesForTopoNodesWithoutVoltageLevel() {
        if (!isDataAvailable) {
            return;
        }
        // These results correspond to nodes on the boundary,
        // where connectivity container is not defined for topoNodes
        Expected expected = new Expected().resultSize(numTopoNodesWithSvVoltageInBoundary);
        Consumer<PropertyBags> consumer = s -> s.stream()
                .forEach(t -> LOG.info("{} {}", t.getId("TopologicalNode"), t.getId("graph")));
        tester.test("SvVoltagesForTopoNodesWithoutVoltageLevel", expected, consumer);
    }

    @Test
    public void testSvVoltagesWithoutTopoNodes() {
        if (!isDataAvailable) {
            return;
        }
        Expected expected = new Expected().resultSize(numMissingTopoNodes);
        Consumer<PropertyBags> consumer = s -> LOG.info(s.tabulateLocals());
        tester.test("SvVoltagesWithoutTopoNodes", expected, consumer);
    }

    private static TestGridModel            model;
    private static int                      numTopoNodesModel;
    private static int                      numTopoNodesWithSvVoltageInBoundary;
    private static int                      numMissingTopoNodes;
    private static int                      numSvVoltages;
    private static AlternativeQueriesTester tester;
    private static boolean                  isDataAvailable;

    private static final Logger             LOG = LoggerFactory
            .getLogger(AlternativeQueriesForTopologicalNodes.class);
}
