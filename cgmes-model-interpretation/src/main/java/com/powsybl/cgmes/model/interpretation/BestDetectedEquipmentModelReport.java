package com.powsybl.cgmes.model.interpretation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.interpretation.InterpretationResult.ValidationData;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;

public class BestDetectedEquipmentModelReport {

    public BestDetectedEquipmentModelReport(Path output) {
        this.output = output;
    }

    public void report(Map<String, InterpretationResult> interpretations) throws IOException {
        Map<String, DetectedEquipmentModel> allDetectedModelData = new HashMap<>();
        prepareData(interpretations, allDetectedModelData);
        writeReport(allDetectedModelData);
    }

    private void prepareData(Map<String, InterpretationResult> interpretations,
            Map<String, DetectedEquipmentModel> allDetectedModelData) throws IOException {
        Comparator<Map.Entry<String, InterpretationResult>> byError = (
                Entry<String, InterpretationResult> o1,
                Entry<String, InterpretationResult> o2) -> {
            return Double.compare(o1.getValue().error, o2.getValue().error);
        };
        Map<String, InterpretationResult> sortedResults = interpretations.entrySet().stream().sorted(byError.reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));
        addData(sortedResults, allDetectedModelData);
    }

    private void addData(Map<String, InterpretationResult> interpretations,
            Map<String, DetectedEquipmentModel> allDetectedModelData) {
        interpretations.entrySet()
                .forEach(e -> addDetectedEquipmentModelData(e.getKey(), e.getValue(), allDetectedModelData));
    }

    private void addDetectedEquipmentModelData(String model, InterpretationResult interpretation,
            Map<String, DetectedEquipmentModel> allDetectedModelData) {

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
                .forEach(mappingConfiguration -> detectedEquipmentModelData(
                        mappingConfigurationData.get(mappingConfiguration),
                        allDetectedModelData));
    }

    private void detectedEquipmentModelData(ValidationData validationData,
            Map<String, DetectedEquipmentModel> allDetectedModelData) {
        validationData.getDetectedModelData().keySet().forEach(model -> {

            DetectedEquipmentModel bestConfigModel = validationData.getDetectedModelData().get(model);
            DetectedEquipmentModel aggregateModel = allDetectedModelData.get(model);
            if (aggregateModel == null) {
                aggregateModel = new DetectedEquipmentModel(bestConfigModel.detectedBranchModels);
            }
            aggregateModel.total += bestConfigModel.total;
            aggregateModel.calculated += bestConfigModel.calculated;
            aggregateModel.ok += bestConfigModel.ok;

            allDetectedModelData.put(model, aggregateModel);
        });
    }

    private void writeReport(Map<String, DetectedEquipmentModel> allDetectedModelData) throws IOException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime dateTime = LocalDateTime.now();
        String formattedDateTime = dateTime.format(dateFormatter);
        try (BufferedWriter w = Files.newBufferedWriter(
                output.resolve("CGMESDetectedEquipmentModelReport." + formattedDateTime + ".csv"),
                StandardCharsets.UTF_8)) {

            TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, false);
            CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
            Column[] columns = new Column[] {
                new Column("code"),
                new Column("total"),
                new Column("calculated"),
                new Column("ok"),
                new Column("evaluationCode")
            };

            Map<String, DetectedEquipmentModel> sortedByModelReport = new TreeMap<String, DetectedEquipmentModel>(
                    new Comparator<String>() {
                        @Override
                        public int compare(String s1, String s2) {
                            if (s1.length() == s2.length()) {
                                return s1.compareTo(s2);
                            }
                            return Integer.compare(s1.length(), s2.length());
                        }
                    });

            sortedByModelReport.putAll(allDetectedModelData);
            TableFormatter formatter = factory.create(w, "All Detected Equipment Model", config, columns);
            sortedByModelReport.keySet().forEach(model -> {
                try {
                    formatter
                            .writeCell(sortedByModelReport.get(model).code())
                            .writeCell(sortedByModelReport.get(model).total)
                            .writeCell(sortedByModelReport.get(model).calculated)
                            .writeCell(sortedByModelReport.get(model).ok)
                            .writeCell(sortedByModelReport.get(model).conversionCode());
                } catch (IOException x) {
                    // Ignored
                }
            });
        }
    }

    private final Path  output;

    static final Logger LOG = LoggerFactory.getLogger(BestInterpretationReport.class);
}
