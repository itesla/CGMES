package com.powsybl.cgmes.tools;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Catalog extends Model {

    public Catalog(String sdata) {
        super(sdata);
    }

    public String modelName(Path p) {
        // Identify the model using the portion of path relative to data root
        return p.subpath(this.data.getNameCount(), p.getNameCount()).toString();
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

    public Map<Path, Exception> reviewAll(String pattern, Consumer<Path> consumer) throws IOException {
        // Review all files or folders that match a given pattern

        // Using "glob" patterns:
        // a double "**" means that there could be intermediate folders
        // a single "*" is any sequence of characters inside the same folder
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(pattern);

        // What could possibly go wrong
        Map<Path, Exception> wrong = new HashMap<>();
        try (Stream<Path> paths = Files.walk(this.data)) {
            paths.filter(pathMatcher::matches).forEach(path -> {
                try {
                    System.err.println(path);
                    consumer.accept(path);
                } catch (Exception x) {
                    wrong.put(path, x);
                }
            });
        }
        return wrong;
    }
}
