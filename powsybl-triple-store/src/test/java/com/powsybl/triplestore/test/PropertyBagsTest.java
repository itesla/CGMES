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

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.powsybl.triplestore.PropertyBag;
import com.powsybl.triplestore.PropertyBags;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class PropertyBagsTest {
    @BeforeClass
    public static void setup() {
        bags = new PropertyBags();
        List<String> properties = Arrays.asList("key0", "key1");
        PropertyBag b0 = new PropertyBag(properties);
        PropertyBag b1 = new PropertyBag(properties);
        bags.add(b0);
        bags.add(b1);
        b0.put("key0", "http://example.com/#key0-value0");
        b1.put("key0", "http://example.com/#key0-value1");
        b0.put("key1", "http://example.com/#key1-value0");
        b1.put("key1", "http://example.com/#key1-value1");
    }

    @Test
    public void testPluck() {
        String[] expectedKeys0 = {"key0-value0", "key0-value1"};
        String[] expectedKeys1 = {"key1-value0", "key1-value1"};
        assertArrayEquals(expectedKeys0, bags.pluck("key0"));
        assertArrayEquals(expectedKeys1, bags.pluck("key1"));
    }

    @Test
    public void testTabulateLocals() {
        StringBuffer s = new StringBuffer(100);
        s.append("key0 \t key1");
        s.append("\n");
        s.append("key0-value0 \t key1-value0");
        s.append("\n");
        s.append("key0-value1 \t key1-value1");
        String expected = s.toString();
        assertEquals(expected, bags.tabulateLocals());
    }

    @Test
    public void testTabulate() {
        StringBuffer s = new StringBuffer(100);
        s.append("key0 \t key1");
        s.append("\n");
        s.append("http://example.com/#key0-value0 \t http://example.com/#key1-value0");
        s.append("\n");
        s.append("http://example.com/#key0-value1 \t http://example.com/#key1-value1");
        String expected = s.toString();
        assertEquals(expected, bags.tabulate());
    }

    private static PropertyBags bags;
}
