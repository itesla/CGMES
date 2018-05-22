package com.powsybl.triplestore.test;

/*
 * #%L
 * Triple stores for CGMES models
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
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.triplestore.AbstractPowsyblTripleStore;
import com.powsybl.triplestore.PropertyBags;
import com.powsybl.triplestore.TripleStoreException;
import com.powsybl.triplestore.TripleStoreFactory;

import cern.colt.Arrays;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class TripleStoreTester {

    public TripleStoreTester(
            boolean doAsserts,
            String[] implementations,
            String base,
            Path workingDir,
            Path... files) {
        this.doAsserts = doAsserts;
        this.implementations = implementations;
        this.base = base;
        this.workingDir = workingDir;
        this.files = files;
        this.tripleStores = new HashMap<>(implementations.length);
    }

    public void load() {
        // Load the model for every triple store implementation
        for (String impl : implementations) {
            AbstractPowsyblTripleStore ts = TripleStoreFactory.create(impl);
            for (Path f : files) {
                try (InputStream is = new BufferedInputStream(Files.newInputStream(f))) {
                    ts.deserialize(is, f.getFileName().toString(), base);
                } catch (IOException e) {
                    throw new TripleStoreException(
                            String.format("Deserializing %s %s", f.getFileName().toString(), base), e);
                }
            }
            ts.dump(line -> LOG.info(line));
            tripleStores.put(impl, ts);
        }
    }

    public void testSerialization() throws Exception {
        for (String impl : implementations) {
            Path output = workingDir.resolve("temp-tstest-serialization").resolve(impl);
            ensureFolder(output);
            String baseName = "";
            tripleStores.get(impl).serialize(new FileDataSource(output, baseName));
        }
    }

    private void ensureFolder(Path p) {
        try {
            Files.createDirectories(p);
        } catch (IOException x) {
            throw new TripleStoreException(
                    String.format("Serialize. Creating directories %s", p), x);
        }
    }

    public void testQuery(
            String queryText,
            Expected expected)
            throws Exception {
        for (String impl : implementations) {
            PropertyBags results = tripleStores.get(impl).query(queryText);
            logResults(impl, results, expected);
            if (doAsserts) {
                assertTrue(results.size() > 0);
                int size = expected.values().iterator().next().length;
                assertEquals(size, results.size());
                expected.keySet().stream()
                        .forEach(property -> assertArrayEquals(
                                expected.get(property),
                                results.pluck(property)));
            }
        }
    }

    private void logResults(
            String impl,
            PropertyBags results,
            Expected expected) {
        LOG.info("{} query result size     : {}", impl, results.size());
        if (results.size() == 0) {
            return;
        }
        LOG.info("{} query result names[0] : {}", impl, results.get(0).keySet().toString());
        LOG.info("{} tabulated results", impl);
        LOG.info(results.tabulate());
        LOG.info("{} tabulated results as localValues", impl);
        LOG.info(results.tabulateLocals());
        expected.keySet().stream().forEach(property -> {
            String[] expectedValues = expected.get(property);
            String[] actualValues = results.pluck(property);
            LOG.info("{} expected values for property {} : {}",
                    impl,
                    property,
                    Arrays.toString(expectedValues));
            LOG.info("{} actual values for property {}   : {}",
                    impl,
                    property,
                    Arrays.toString(actualValues));
        });
    }

    public static class Expected extends HashMap<String, String[]> {
        public Expected expect(String property, String... values) {
            put(property, values);
            return this;
        }
    }

    private final boolean                                 doAsserts;
    private final String[]                                implementations;
    private final String                                  base;
    private final Path                                    workingDir;
    private final Path[]                                  files;
    private final Map<String, AbstractPowsyblTripleStore> tripleStores;

    private static final Logger                           LOG = LoggerFactory
            .getLogger(TripleStoreTester.class);
}
