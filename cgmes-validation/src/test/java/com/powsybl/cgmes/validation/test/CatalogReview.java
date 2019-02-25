package com.powsybl.cgmes.validation.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.iidm.network.Network;

public class CatalogReview extends TestBase {

    public CatalogReview(String sdata) {
        super(sdata);
    }

    public String modelName(Path p) {
        // Identify the model using the portion of path relative to data root
        return p.subpath(this.data.getNameCount(), p.getNameCount()).toString();
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

    public void reviewLevels(String pattern) throws IOException {
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

    private static class ResultsCheckBuses {
        ResultsCheckBuses(Network network) {
            numBuses = (int) network.getBusView().getBusStream().count();
            details = new ArrayList<>(numBuses);
        }

        private boolean checkBuses;
        private final List<String> details;
        private final int numBuses;
    }

    public void reviewCheckBuses(String pattern) throws IOException {
        Map<Path, ResultsCheckBuses> results = new HashMap<>();
        Map<Path, Exception> wrong = reviewAll(pattern, p -> {
            Network network = convert(p);
            ResultsCheckBuses r = new ResultsCheckBuses(network);
            checkBuses(network, r.details);
            // intermediate results should be processed/compressed/summarized
            // so we keep use of memory "relatively" low
            // here, we are not doing any post-processing of the checkBuses results
            // we are simply storing all the details
            results.put(p, r);
        });
        report(results);
        reportWrong(wrong);
    }

    private void report(Map<Path, ResultsCheckBuses> results) {
        System.err.println("");
        System.err.println("Results");
        System.err.println("Results    pctok, errp, errq, check, path");
        boolean verbose = false;
        if (verbose) {
            System.err.println("Results        errp, errq, bus");
        }
        results.keySet().stream()
                .sorted()
                .forEach(p -> report(p, results.get(p), verbose));
    }

    private void report(Path p, ResultsCheckBuses r, boolean verbose) {
        // Check if all buses have been included in the detailed results
        assertEquals(r.numBuses, r.details.size() - 2);
        // Errors by bus
        Map<String, Complex> errors = new HashMap<>(r.details.size());
        // Check header matches expected contents
        assertEquals("id;incomingP;incomingQ;loadP;loadQ", r.details.get(1));
        // Skip title and header
        r.details.stream().skip(2).forEach(d -> {
            String[] fields = d.split(";");
            String id = fields[0];
            try {
                double incomingp = asDouble(fields[1]);
                double incomingq = asDouble(fields[2]);
                double loadp = asDouble(fields[3]);
                double loadq = asDouble(fields[4]);
                // Ignore invalid values
                if (valid(incomingp, incomingq, loadp, loadq)) {
                    errors.put(id, new Complex(Math.abs(incomingp + loadp), Math.abs(incomingq + loadq)));
                }
            } catch (Exception x) {
                System.out.println("error " + x.getMessage());
            }
        });
        // Total error and statistics about buses with error
        Complex error = errors.values().stream()
                .reduce(Complex::add).orElse(Complex.ZERO);
        double threshold = 1.0;
        double pctok = 100.0 * errors.values().stream()
                .filter(e -> Math.abs(e.getReal()) < threshold && Math.abs(e.getImaginary()) < threshold)
                .count()
                / r.numBuses;
        System.err.printf("    %5.1f%% (%10.2f, %10.2f) %b %s%n",
                pctok,
                error.getReal(), error.getImaginary(),
                r.checkBuses,
                p);
        if (verbose) {
            // Details about all errors by bus
            errors.entrySet().stream()
                    .sorted((e1, e2) -> -Double.compare(e1.getValue().abs(), e2.getValue().abs()))
                    .forEach(e -> reportBusError(e.getKey(), e.getValue()));
        }
    }

    private void reportBusError(String id, Complex error) {
        System.err.printf("        %10.2f %10.2f %s%n", error.getReal(), error.getImaginary(), id);
    }

    private void reportWrong(Map<Path, Exception> wrong) {
        System.err.println("Wrong");
        System.err.println("Wrong    model, exception");
        wrong.keySet().stream()
                .sorted()
                .forEach(p -> System.err.printf("    %s %s%n", modelName(p), wrong.get(p).getMessage()));
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
