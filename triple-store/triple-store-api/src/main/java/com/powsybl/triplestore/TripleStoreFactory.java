package com.powsybl.triplestore;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.powsybl.commons.util.ServiceLoaderCache;

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

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public final class TripleStoreFactory {

    // XXX LUMA define PowsyblTripleStore as an interface,
    // then either let AbstractPowsyblTripleStore exist, or move methods to a utility class
    private static final ServiceLoaderCache<AbstractPowsyblTripleStore> LOADER = new ServiceLoaderCache(
            AbstractPowsyblTripleStore.class);

    private TripleStoreFactory() {
    }

    public static AbstractPowsyblTripleStore create() {
        return create(DEFAULT_IMPLEMENTATION);
    }

    public static AbstractPowsyblTripleStore create(String impl) {
        Objects.requireNonNull(impl);
        for (AbstractPowsyblTripleStore ts : LOADER.getServices()) {
            if (ts.getName().equals(impl)) {
                // XXX LUMA to maintain compatibility with existing code,
                // triple store are in fact factories,
                // we create a new instance
                // The ServiceLoader should be applied to factories, not to tripleStore classes
                return ts.create();
            }
        }
        return null;
    }

    public static String[] allImplementations() {
        // XXX LUMA use List<String> or List<AbstractPowsyblTripleStore> instead of String[]
        List<String> impls = LOADER.getServices().stream()
                .map(AbstractPowsyblTripleStore::getName)
                .collect(Collectors.toList());
        return impls.toArray(new String[impls.size()]);
    }

    public static String[] implementationsWorkingWithNestedGraphClauses() {
        // XXX LUMA use List<String> or List<AbstractPowsyblTripleStore> instead of String[]
        List<String> impls = LOADER.getServices().stream()
                .filter(AbstractPowsyblTripleStore::worksWithNestedGraphClauses)
                .map(AbstractPowsyblTripleStore::getName)
                .collect(Collectors.toList());
        return impls.toArray(new String[impls.size()]);
    }

    public static String[] implementationsBadNestedGraphClauses() {
        // XXX LUMA use List<String> or List<AbstractPowsyblTripleStore> instead of String[]
        List<String> impls = LOADER.getServices().stream()
                .filter(ts -> !ts.worksWithNestedGraphClauses())
                .map(AbstractPowsyblTripleStore::getName)
                .collect(Collectors.toList());
        return impls.toArray(new String[impls.size()]);
    }

    public static String[] onlyDefaultImplementation() {
        // XXX LUMA use List<String> or List<AbstractPowsyblTripleStore> instead of String[]
        return ONLY_DEFAULT_IMPLEMENTATION;
    }

    public static String defaultImplementation() {
        return DEFAULT_IMPLEMENTATION;
    }

    private static final String   DEFAULT_IMPLEMENTATION      = "rdf4j";
    private static final String[] ONLY_DEFAULT_IMPLEMENTATION = {DEFAULT_IMPLEMENTATION};
}
