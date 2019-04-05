package com.powsybl.cgmes.model.interpretation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.interpretation.InterpretationResult.ValidationData;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;

public class BestInterpretationReport {

    private static final double BALANCE_TOLERANCE = 1.0;

    public BestInterpretationReport(Path output) {
        this.output = output;
    }

    public void report(Map<String, InterpretationResult> interpretations) throws IOException {
        Comparator<Map.Entry<String, InterpretationResult>> byError = (
                Entry<String, InterpretationResult> o1,
                Entry<String, InterpretationResult> o2) -> {
            return Double.compare(o1.getValue().error, o2.getValue().error);
        };
        Map<String, InterpretationResult> sortedResults = interpretations.entrySet().stream().sorted(byError.reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));
        dump(sortedResults);
    }

    private void dump(Map<String, InterpretationResult> interpretations) throws IOException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime dateTime = LocalDateTime.now();
        String formattedDateTime = dateTime.format(dateFormatter);
        try (BufferedWriter w = Files.newBufferedWriter(
                output.resolve("CGMESModelInterpretationReport." + formattedDateTime + ".csv"),
                StandardCharsets.UTF_8)) {
            TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, false);
            CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
            Column[] columns = new Column[] {
                new Column("TSO"),
                new Column("Best interpretation"),
                new Column("Num nodes"),
                new Column("Pct of bad nodes"),
                new Column("Num bad nodes"),
                new Column("Total error bad nodes"),
                new Column("Total error"),
                new Column("Isolated nodes"),
                new Column("Non-calculated nodes"),
                new Column("Num nodes ok"),
                new Column("Total bad voltage error"),
                new Column("Num bad voltage nodes"),
                new Column("Pct bad voltage nodes"),
                new Column("Model")
            };
            TableFormatter formatter = factory.create(w, "BEST INTERPRETATION", config, columns);
            interpretations.entrySet().forEach(e -> {
                generateModelReport(e.getKey(), e.getValue(), formatter);
            });
        }
    }

    private void generateModelReport(String model, InterpretationResult interpretation, TableFormatter formatter) {

        if (interpretation.exception != null) {
            return;
        }

        Map<CgmesEquipmentModelMapping, ValidationData> mappingConfigurationData = interpretation.validationDataForAllModelMapping;
        Comparator<Map.Entry<CgmesEquipmentModelMapping, ValidationData>> byBalance = (
                Entry<CgmesEquipmentModelMapping, ValidationData> o1,
                Entry<CgmesEquipmentModelMapping, ValidationData> o2) -> {
            int cp = Double.compare(o1.getValue().balance, o2.getValue().balance);
            if (cp == 0) {
                return Integer.compare(o1.getKey().length(), o2.getKey().length());
            }
            return cp;
        };

        Map<CgmesEquipmentModelMapping, ValidationData> sortedMappingConfigurationData = mappingConfigurationData
                .entrySet().stream().sorted(byBalance)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));

        sortedMappingConfigurationData.keySet().stream().limit(1)
                .forEach(mappingConfiguration -> {
                    try {
                        interpretationReport(model, mappingConfiguration,
                                mappingConfigurationData.get(mappingConfiguration), formatter);
                    } catch (IOException e) {
                        // Ignored
                    }
                });
    }

    private void interpretationReport(String model, CgmesEquipmentModelMapping mappingConfiguration,
            ValidationData validationData, TableFormatter formatter) throws IOException {

        long totalNodes = validationData.balanceData.values().size();
        long notCalculatedNodes = validationData.balanceData.entrySet().stream()
                .filter(entry -> !entry.getValue().asBoolean("calculated", false)
                        && !entry.getValue().asBoolean("isolated", false))
                .count();
        long okNodes = validationData.balanceData.values().stream().filter(pb -> pb.asBoolean("calculated", false))
                .filter(pb -> (Math.abs(pb.asDouble("balanceP"))
                        + Math.abs(pb.asDouble("balanceQ"))) <= BALANCE_TOLERANCE)
                .count();
        long badNodes = validationData.balanceData.values().stream()
                .filter(pb -> pb.asBoolean("calculated", false) && !pb.asBoolean("badVoltage", false))
                .filter(pb -> (Math.abs(pb.asDouble("balanceP"))
                        + Math.abs(pb.asDouble("balanceQ"))) > BALANCE_TOLERANCE)
                .count();
        long isolatedNodes = validationData.balanceData.values().stream().filter(pb -> pb.asBoolean("isolated", false))
                .count();
        long badVoltageNodes = validationData.balanceData.values().stream()
                .filter(pb -> pb.asBoolean("calculated", false) && pb.asBoolean("badVoltage", false))
                .filter(pb -> (Math.abs(pb.asDouble("balanceP"))
                        + Math.abs(pb.asDouble("balanceQ"))) > BALANCE_TOLERANCE)
                .count();
        double badNodesError = validationData.balanceData.values().stream()
                .filter(pb -> pb.asBoolean("calculated", false) && !pb.asBoolean("badVoltage", false))
                .filter(pb -> (Math.abs(pb.asDouble("balanceP"))
                        + Math.abs(pb.asDouble("balanceQ"))) > BALANCE_TOLERANCE)
                .map(pb -> Math.abs(pb.asDouble("balanceP")) + Math.abs(pb.asDouble("balanceQ")))
                .mapToDouble(Double::doubleValue).sum();
        double badVoltageNodesError = validationData.balanceData.values().stream()
                .filter(pb -> pb.asBoolean("calculated", false) && pb.asBoolean("badVoltage", false))
                .filter(pb -> (Math.abs(pb.asDouble("balanceP"))
                        + Math.abs(pb.asDouble("balanceQ"))) > BALANCE_TOLERANCE)
                .map(pb -> Math.abs(pb.asDouble("balanceP")) + Math.abs(pb.asDouble("balanceQ")))
                .mapToDouble(Double::doubleValue).sum();

        try {
            formatter
                    .writeCell(tsoName(model))
                    .writeCell(mappingConfiguration.toString())
                    .writeCell((int) totalNodes)
                    .writeCell(Long.valueOf(badNodes).doubleValue()
                            / Long.valueOf(totalNodes - isolatedNodes).doubleValue())
                    .writeCell((int) badNodes)
                    .writeCell(badNodesError)
                    .writeCell(validationData.balance)
                    .writeCell((int) isolatedNodes)
                    .writeCell((int) notCalculatedNodes)
                    .writeCell((int) okNodes)
                    .writeCell(badVoltageNodesError)
                    .writeCell((int) badVoltageNodes)
                    .writeCell(Long.valueOf(badVoltageNodes).doubleValue()
                            / Long.valueOf(totalNodes - isolatedNodes).doubleValue())
                    .writeCell(model);
        } catch (IOException x) {
            // Ignored
        }
    }

    public String tsoName(String model) {
        int i = model.indexOf("_1D_") + 4;
        if (model.indexOf("_1D_") == -1) {
            i = model.indexOf("_2D_") + 4;
        }
        int j = model.indexOf("_", i);
        if (model.indexOf("_", i) > model.indexOf("\\", i)) {
            j = model.indexOf("\\", i);
        }
        if (j > i) {
            return model.substring(i, j);
        } else {
            return model.substring(i);
        }
    }

    private final Path  output;

    static final Logger LOG = LoggerFactory.getLogger(BestInterpretationReport.class);
}
