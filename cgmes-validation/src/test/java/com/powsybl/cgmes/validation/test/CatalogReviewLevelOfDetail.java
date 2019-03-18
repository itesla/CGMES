/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.powsybl.cgmes.model.CgmesModel;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CatalogReviewLevelOfDetail extends CatalogReview {

    public CatalogReviewLevelOfDetail(CatalogLocation location) {
        super(location);
    }

    public void reviewAll(String pattern) throws IOException {
        reviewAll(pattern, p -> {
            String m = modelName(p);
            models.add(m);
            levelsFromPath.put(m, level(m));
            levelsFromCgmes.put(m, level(cgmes(p)));
        });
    }

    public void report() {
        System.err.printf("%-20s\t%-20s\t%s%n", "levelPath", "levelCgmes", "model");
        models.forEach(m -> {
            System.err.printf("%-20s\t%-20s\t%s%n", levelsFromPath.get(m), levelsFromCgmes.get(m), m);
        });
    }

    private String level(CgmesModel cgmes) {
        return cgmes.isNodeBreaker() ? "NodeBreaker" : "BusBranch";
    }

    private String level(String modelName) {
        Matcher matcher = LEVEL_FROM_NAME_PATTERN.matcher(modelName);
        String level = "-";
        if (matcher.find()) {
            level = modelName.substring(matcher.start(), matcher.end());
        }
        return level;
    }

    private final List<String> models = new ArrayList<>();
    private final Map<String, String> levelsFromPath = new HashMap<>();
    private final Map<String, String> levelsFromCgmes = new HashMap<>();

    private static final Pattern LEVEL_FROM_NAME_PATTERN = Pattern.compile("(?i)(BusBranch|NodeBreaker)");
}
