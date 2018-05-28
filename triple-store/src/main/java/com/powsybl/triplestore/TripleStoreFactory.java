package com.powsybl.triplestore;

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

import com.powsybl.triplestore.blazegraph.TripleStoreBlazegraph;
import com.powsybl.triplestore.jena.TripleStoreJena;
import com.powsybl.triplestore.rdf4j.TripleStoreRDF4J;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public final class TripleStoreFactory {

    private TripleStoreFactory() {
    }

    public static AbstractPowsyblTripleStore create() {
        return create(DEFAULT_IMPLEMENTATION);
    }

    public static AbstractPowsyblTripleStore create(String impl) {
        switch (impl) {
            case IMPL_JENA:
                return new TripleStoreJena();
            case IMPL_BLAZEGRAPH:
                return new TripleStoreBlazegraph();
            case IMPL_RDF4J:
            default:
                return new TripleStoreRDF4J();
        }
    }

    public static String[] allImplementations() {
        return ALL_IMPLEMENTATIONS;
    }

    public static String[] implementationsAllowingNestedGraphClauses() {
        return IMPLEMENTATIONS_ALLOWING_NESTED_GRAPH_CLAUSES;
    }

    public static String[] onlyDefaultImplementation() {
        return ONLY_DEFAULT_IMPLEMENTATION;
    }

    public static final String    IMPL_RDF4J                                    = "rdf4j";
    public static final String    IMPL_JENA                                     = "jena";
    public static final String    IMPL_BLAZEGRAPH                               = "blazegraph";

    private static final String[] ALL_IMPLEMENTATIONS                           = {IMPL_RDF4J, IMPL_JENA, IMPL_BLAZEGRAPH};
    private static final String[] IMPLEMENTATIONS_ALLOWING_NESTED_GRAPH_CLAUSES = {IMPL_RDF4J, IMPL_JENA};
    public static final String    DEFAULT_IMPLEMENTATION                        = IMPL_RDF4J;
    private static final String[] ONLY_DEFAULT_IMPLEMENTATION                   = {DEFAULT_IMPLEMENTATION};
}
