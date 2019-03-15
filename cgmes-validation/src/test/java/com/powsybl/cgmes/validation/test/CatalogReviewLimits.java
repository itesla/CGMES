package com.powsybl.cgmes.validation.test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.validation.test.LimitsSummary.Limits;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.iidm.network.Network;

public class CatalogReviewLimits extends CatalogReview {

    public CatalogReviewLimits(CatalogLocation location) {
        super(location);
    }

    public void reviewAll(String pattern) throws IOException {
        reviewAll(
            pattern,
            p -> {
                Network network = convert(p);
                CgmesModel cgmes = network.getExtension(CgmesModelExtension.class).getCgmesModel();
                mass.put(p, mas(cgmes));
                limits.put(p, new LimitsSummary(cgmes));
            },
            p -> {
                mass.put(p, "-");
                limits.put(p, new LimitsSummary());
            });
    }

    public void report(String outputFilename) throws IOException {
        reportLimits(outputFilename, limits, mass);
        reportLimitUseByEqClass(limits);
        reportLimitUseByEqClass3(limits);
        reportLimitAnomalies(limits);
    }

    private void reportLimitUseByEqClass(Map<Path, LimitsSummary> limits) {
        System.err.println("Use of Limit Types by Equipment Class");
        limits.forEach((p, l) -> {
            System.err.println(modelName(p));
            l.countsByEqClassAndLimitType().forEach((eqclass, ls) -> {
                long numObjectsClass = l.eqClassNumObjects().get(eqclass);
                System.err.printf("    %6d %s%n", numObjectsClass, eqclass);
                ls.forEach((type, numLimits) -> {
                    System.err.printf("        %6d %5.1f%% %s%n", numLimits, 100.0 * numLimits / numObjectsClass, type);
                });
            });
        });
    }

    private void reportLimitUseByEqClass3(Map<Path, LimitsSummary> limits) {
        System.err.println("Use of Limit Types by Equipment Class 3");
        limits.forEach((p, l) -> {
            System.err.println(modelName(p));
            l.countsByEqClassAndLimitTypeAndTerminal().forEach((eqclass, ls) -> {
                long numObjectsClass = l.eqClassNumObjects().get(eqclass);
                System.err.printf("    %6d %s%n", numObjectsClass, eqclass);
                ls.forEach((type, tels) -> {
                    System.err.printf("           %s%n", type);
                    tels.forEach((te, numLimits) -> {
                        System.err.printf("               %6d %5.1f%% %s%n",
                            numLimits,
                            100.0 * numLimits / numObjectsClass,
                            te);
                    });
                });
            });
        });
    }

    private void reportLimitAnomalies(Map<Path, LimitsSummary> limits) {
        boolean found = limits.values().stream()
            .filter(LimitsSummary::hasEquipmentWithSameLimitTypeAndDifferentSubclasses)
            .findAny()
            .isPresent();
        if (found) {
            System.err.println("Anomalies");
            System.err.println("Different subclasses for the same limit Type");
            System.err.println("    model");
            System.err.println("        number of equipment, sample equipment Id, type with different subclasses");
        }
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

    private final Map<Path, LimitsSummary> limits = new HashMap<>();
    private final Map<Path, String> mass = new HashMap<>();
}
