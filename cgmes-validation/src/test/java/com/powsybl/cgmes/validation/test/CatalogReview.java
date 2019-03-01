package com.powsybl.cgmes.validation.test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.cgmes.validation.test.LimitsSummary.Limits;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBags;

public class CatalogReview extends TestBase {

    public CatalogReview(String sdata) {
        super(sdata);
    }

    public void setDryRun(boolean d) {
        dryRun = d;
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

    public void reviewLimits(String pattern, String outputFilename) throws IOException {
        Map<Path, LimitsSummary> limits = new HashMap<>();
        Map<Path, String> mass = new HashMap<>();
        Map<Path, Exception> wrong = reviewAll(pattern, p -> {
            if (dryRun) {
                mass.put(p, "-");
                limits.put(p, new LimitsSummary());
            } else {
                Network network = convert(p);
                CgmesModel cgmes = network.getExtension(CgmesModelExtension.class).getCgmesModel();
                mass.put(p, mas(cgmes));
                limits.put(p, new LimitsSummary(cgmes));
            }
        });
        reportLimits(outputFilename, limits, mass);
        reportWrong(wrong);

        System.err.println("Anomalies");
        System.err.println("Different subclasses for the same limit Type");
        System.err.println("    model");
        System.err.println("        number, sample equipment Id, type with different subclasses");
        limits.forEach((p, l) -> {
            if (l.hasEquipmentWithSameLimitTypeAndDifferentSubclasses()) {
                System.err.println(modelName(p));
                l.sameLimitTypeAndDifferentSubclasses().forEach(ls -> System.err.printf("    %5d %-32s %s%n",
                    l.sameLimitTypeAndDifferentSubclassesCount(ls),
                    l.sameLimitTypeAndDifferentSubclassesSample(ls),
                    ls));
            }
        });
    }

    private void reportLimits(String outputFilename, Map<Path, LimitsSummary> limits, Map<Path, String> mass)
        throws IOException {
        boolean printHeader = true;
        boolean printTitle = false;
        TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", printHeader, printTitle);
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        Column[] columns = new Column[] {
            new Column("Model"),
            new Column("TSO"),
            new Column("Country"),
            new Column("MAS"),
            new Column("DefinedFor"),
            new Column("Equipment Class"),
            new Column("Limit EqClass Count"),
            new Column("Limit Type"),
            new Column("Limit Type Name"),
            new Column("Limit Subclass"),
            new Column("Limit Type Count")
        };
        try (Writer writer = new FileWriter(outputFilename)) {
            TableFormatter formatter = factory.create(writer, "OperationalLimits Summary", config, columns);
            limits.forEach((p, l) -> {
                String m = modelName(p);
                String tso = tsoName(p);
                String country = country(p);
                String mas = mass.get(p);
                reportLimits(l.forTerminals(), formatter, m, tso, country, mas, "Terminal");
                reportLimits(l.forEquipment(), formatter, m, tso, country, mas, "Equipment");
            });
        }
    }

    private void reportLimits(
        List<Limits> limits,
        TableFormatter formatter,
        String modelName,
        String tso,
        String country,
        String mas,
        String definedFor) {
        limits.forEach(l -> l.types.forEach((lt, num) -> {
            try {
                formatter
                    .writeCell(modelName)
                    .writeCell(tso)
                    .writeCell(country)
                    .writeCell(mas)
                    .writeCell(definedFor)
                    .writeCell(l.equipmentClass)
                    .writeCell(l.num)
                    .writeCell(lt.type)
                    .writeCell(lt.name)
                    .writeCell(lt.subclass)
                    .writeCell(num);
            } catch (IOException x) {
                // Ignored
            }
        }));
    }

    private static class ResultsCheckBuses {
        ResultsCheckBuses(Network network) {
            numBuses = (int) network.getBusView().getBusStream().count();
            errors = new HashMap<>(numBuses);
        }

        private boolean checkBuses;
        private final Map<String, Complex> errors;
        private final int numBuses;
    }

    public void reviewCheckBuses(String pattern) throws IOException {
        Map<Path, ResultsCheckBuses> results = new HashMap<>();
        Map<Path, Exception> wrong = reviewAll(pattern, p -> {
            Network network = convert(p);
            ResultsCheckBuses r = new ResultsCheckBuses(network);
            checkBuses(network, r.errors);
            // intermediate results should be processed/compressed/summarized
            // so we keep use of memory "relatively" low
            // here, we are not doing any post-processing of the checkBuses results
            // we are simply storing all the details
            results.put(p, r);
        });
        reportCheckBuses(results);
        reportWrong(wrong);
    }

    private void reportCheckBuses(Map<Path, ResultsCheckBuses> results) {
        System.err.println("");
        System.err.println("Results");
        System.err.println("Results    size, pctok, errp, errq, check, path");
        boolean verbose = true;
        if (verbose) {
            System.err.println("Results        errp, errq, bus");
        }
        results.keySet().stream()
            .sorted()
            .forEach(p -> report(p, results.get(p), verbose));
    }

    private void report(Path p, ResultsCheckBuses r, boolean verbose) {
        // Total error and statistics about buses with error
        Complex error = r.errors.values().stream().reduce(Complex::add).orElse(Complex.ZERO);
        double pctok = 100.0 * (1 - r.errors.values().size() / r.numBuses);
        System.err.printf("    %5d %5.1f%% (%10.2f, %10.2f) %b %s%n",
            r.numBuses,
            pctok,
            error.getReal(), error.getImaginary(),
            r.checkBuses,
            p);
        if (verbose) {
            // Details about all errors by bus
            r.errors.entrySet().stream()
                .sorted((e1, e2) -> -Double.compare(e1.getValue().abs(), e2.getValue().abs()))
                .forEach(e -> reportBusError(e.getKey(), e.getValue()));
        }
    }

    private void reportBusError(String id, Complex error) {
        System.err.printf("        %10.2f %10.2f %s%n", error.getReal(), error.getImaginary(), id);
    }

    private void reportWrong(Map<Path, Exception> wrong) {
        if (wrong.isEmpty()) {
            return;
        }
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

    private String mas(CgmesModel cgmes) {
        PropertyBags models = ((CgmesModelTripleStore) cgmes).namedQuery("modelIds");
        return models.stream()
            .filter(m -> m.containsKey("modelingAuthoritySet"))
            .map(m -> m.get("modelingAuthoritySet"))
            .filter(mas -> !mas.contains("tscnet.eu"))
            .findFirst()
            .orElse("-");
    }

    private boolean dryRun = false;

    private static final Pattern LEVEL_FROM_NAME_PATTERN = Pattern.compile("(?i)(BusBranch|NodeBreaker)");
}
