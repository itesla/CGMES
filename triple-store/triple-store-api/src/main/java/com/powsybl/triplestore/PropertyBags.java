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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class PropertyBags extends ArrayList<PropertyBag> {

    public PropertyBags() {
        super();
    }

    public PropertyBags(Collection<PropertyBag> ps) {
        super(ps);
    }

    public String[] pluck(String property) {
        return stream()
                .map(r -> r.getLocal(property))
                .sorted(Comparator.nullsLast(String::compareTo))
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    public String tabulateLocals() {
        return tabulate((bag, property) -> bag.getLocal(property));
    }

    public String tabulate() {
        return tabulate((bag, property) -> bag.get(property));
    }

    private String tabulate(BiFunction<PropertyBag, String, String> getValue) {
        if (size() == 0) {
            return "";
        }
        List<String> names = get(0).propertyNames();
        String columnSeparator = " \t ";
        String lineSeparator = System.lineSeparator();

        StringBuilder s = new StringBuilder(size() * 80);
        s.append(names.stream().collect(Collectors.joining(columnSeparator)));
        s.append(lineSeparator);
        s.append(stream()
                .map(r -> names.stream()
                        .map(n -> r.containsKey(n) ? getValue.apply(r, n) : "N/A")
                        .collect(Collectors.joining(columnSeparator)))
                .collect(Collectors.joining(lineSeparator)));
        return s.toString();
    }
}
