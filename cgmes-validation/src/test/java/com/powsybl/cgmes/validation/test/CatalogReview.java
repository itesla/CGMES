package com.powsybl.cgmes.validation.test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.triplestore.api.PropertyBags;

public class CatalogReview extends TestBase {

    public CatalogReview(String dataRootPathname, String boundaryPathname) {
        super(dataRootPathname, boundaryPathname);
    }

    protected Map<Path, Exception> reviewAll(String pattern, Consumer<Path> consumer) throws IOException {
        return reviewAll(pattern, consumer, null);
    }

    protected Map<Path, Exception> reviewAll(String pattern, Consumer<Path> consumer, Consumer<Path> dryRunConsumer)
        throws IOException {
        // Review all files or folders that match a given pattern

        // Using "glob" patterns:
        // a double "**" means that there could be intermediate folders
        // a single "*" is any sequence of characters inside the same folder
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(pattern);

        // What could possibly go wrong
        Map<Path, Exception> wrong = new HashMap<>();
        try (Stream<Path> paths = Files.walk(this.dataRoot)) {
            paths.filter(pathMatcher::matches).forEach(path -> {
                try {
                    System.err.println(path);
                    if (!dryRun) {
                        consumer.accept(path);
                    } else if (dryRunConsumer != null) {
                        dryRunConsumer.accept(path);
                    }
                } catch (Exception x) {
                    wrong.put(path, x);
                }
            });
        }
        return wrong;
    }

    public void setDryRun(boolean d) {
        dryRun = d;
    }

    public String modelName(Path p) {
        // Identify the model using the portion of path relative to data root
        return p.subpath(this.dataRoot.getNameCount(), p.getNameCount()).toString();
    }

    public String tsoName(Path p) {
        String sp = p.toString();
        int i = sp.indexOf("_1D_") + 4;
        int j = sp.indexOf("_", i);
        if (j > i) {
            return sp.substring(i, j);
        } else {
            return sp.substring(i);
        }
    }

    public String country(Path p) {
        String sp = p.toString();
        int i = sp.lastIndexOf("_");
        return sp.substring(i + 1, i + 3);
    }

    protected String mas(CgmesModel cgmes) {
        PropertyBags models = ((CgmesModelTripleStore) cgmes).namedQuery("modelIds");
        return models.stream()
            .filter(m -> m.containsKey("modelingAuthoritySet"))
            .map(m -> m.get("modelingAuthoritySet"))
            .filter(mas -> !mas.contains("tscnet.eu"))
            .findFirst()
            .orElse("-");
    }

    protected void reportWrong(Map<Path, Exception> wrong) {
        if (wrong.isEmpty()) {
            return;
        }
        System.err.println("Wrong");
        System.err.println("Wrong    model, exception");
        wrong.keySet().stream()
            .sorted()
            .forEach(p -> System.err.printf("    %s %s%n", modelName(p), wrong.get(p).getMessage()));
    }

    private boolean dryRun = false;
}
