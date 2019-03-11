package com.powsybl.cgmes.validation.test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.powsybl.cgmes.conversion.CgmesModelExtension;

public class CatalogReviewLevelOfDetail extends CatalogReview {

    public CatalogReviewLevelOfDetail(CatalogLocation location) {
        super(location);
    }

    public void reviewAll(String pattern) throws IOException {
        List<String> models = new ArrayList<>();
        Map<String, String> levelsFromPath = new HashMap<>();
        Map<String, String> levelsFromCgmes = new HashMap<>();
        Map<Path, Exception> wrong = reviewAll(pattern, p -> {
            String m = modelName(p);
            String levelFromCgmes = convert(p).getExtension(CgmesModelExtension.class).getCgmesModel().isNodeBreaker()
                ? "NodeBreaker"
                : "BusBranch";
            models.add(m);
            levelsFromPath.put(m, levelFromModelName(m));
            levelsFromCgmes.put(m, levelFromCgmes);
        });
        System.err.printf("%-20s\t%-20s\t%s%n", "levelPath", "levelCgmes", "model");
        models.forEach(m -> {
            System.err.printf("%-20s\t%-20s\t%s%n", levelsFromPath.get(m), levelsFromCgmes.get(m), m);
        });
        reportWrong(wrong);
    }

    private String levelFromModelName(String m) {
        Matcher matcher = LEVEL_FROM_NAME_PATTERN.matcher(m);
        String level = "-";
        if (matcher.find()) {
            level = m.substring(matcher.start(), matcher.end());
        }
        return level;
    }

    private static final Pattern LEVEL_FROM_NAME_PATTERN = Pattern.compile("(?i)(BusBranch|NodeBreaker)");
}
