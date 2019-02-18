package com.powsybl.cgmes.validation.test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CatalogReview extends TestBase {

    public CatalogReview(String sdata) {
        super(sdata);
    }

    public void reviewAll(String pattern, Consumer<Path> consumer) throws IOException {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(pattern);
        // Review all files or folders that match a pattern
        try (Stream<Path> paths = Files.walk(this.data)) {
            paths.filter(pathMatcher::matches).forEach(path -> {
                consumer.accept(path);
            });
        }
    }
}
