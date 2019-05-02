/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.conversion.validation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
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

import com.powsybl.cgmes.conversion.validation.ConversionValidationResult.VerificationData;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class ConversionValidationReport {

    private static final int SHOW_BRANCH_ENDS = 5;

    public ConversionValidationReport(Path output) {
        this.output = output;
    }

    public void report(Map<String, ConversionValidationResult> conversionValidationResults) throws IOException {
        Comparator<Map.Entry<String, ConversionValidationResult>> byFailed = (
                Entry<String, ConversionValidationResult> o1,
                Entry<String, ConversionValidationResult> o2) -> {
            return Integer.compare(o1.getValue().failedCount, o2.getValue().failedCount);
        };
        Map<String, ConversionValidationResult> sortedResults = conversionValidationResults.entrySet().stream()
                .sorted(byFailed.reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));
        dump(sortedResults);
    }

    private void dump(Map<String, ConversionValidationResult> conversionValidationResults) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime dateTime = LocalDateTime.now();
        String formattedDateTime = dateTime.format(formatter);
        try (BufferedWriter w = Files.newBufferedWriter(
                output.resolve("conversionValidationReport." + formattedDateTime), StandardCharsets.UTF_8)) {
            conversionValidationResults.entrySet().forEach(e -> {
                try {
                    w.write(generateModelReport(e.getKey(), e.getValue()));
                    w.newLine();
                    w.flush();
                } catch (IOException x) {
                    LOG.warn("Error writing report for model {} {}", e.getKey(), x.getMessage());
                }
            });
        }
    }

    private String generateModelReport(String model, ConversionValidationResult conversionValidationResult) {

        if (conversionValidationResult.exception != null) {
            return conversionValidationResult.exception.getMessage();
        }

        Map<CgmesEquipmentModelMapping, VerificationData> mappingConfigurationData = conversionValidationResult.verificationDataForAllModelMapping;
        Comparator<Map.Entry<CgmesEquipmentModelMapping, VerificationData>> byFailedCount = (
                Entry<CgmesEquipmentModelMapping, VerificationData> o1,
                Entry<CgmesEquipmentModelMapping, VerificationData> o2) -> {
            return Integer.compare(o1.getValue().failedCount(), o2.getValue().failedCount());
        };

        Map<CgmesEquipmentModelMapping, VerificationData> sortedMappingConfigurationData = mappingConfigurationData
                .entrySet().stream().sorted(byFailedCount.reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));

        StringBuilder modelReportBuilder = new StringBuilder();
        modelReportBuilder.append(String.format("----> MODEL %s ", model));
        modelReportBuilder.append(System.getProperty("line.separator"));
        sortedMappingConfigurationData.keySet()
                .forEach(mappingConfiguration -> {
                    try {
                        conversionValidationReport(model, mappingConfiguration,
                                mappingConfigurationData.get(mappingConfiguration), modelReportBuilder);
                    } catch (IOException e) {
                        // Ignored
                    }
                });
        return modelReportBuilder.toString();
    }

    private void conversionValidationReport(String model, CgmesEquipmentModelMapping mappingConfiguration,
            VerificationData verificationData, StringBuilder modelReportBuilder) throws IOException {
        conversionValidationReportHeaderSection(mappingConfiguration, modelReportBuilder);
        conversionValidationReportFlowSection(verificationData, modelReportBuilder);
        conversionValidationReportBranchEndSection(verificationData, modelReportBuilder);
    }

    private void conversionValidationReportHeaderSection(CgmesEquipmentModelMapping mappingConfiguration,
            StringBuilder modelReportBuilder) {
        LOG.debug("----> MAPPING CONFIG {}", mappingConfiguration);
        modelReportBuilder.append(String.format("----> config %s", mappingConfiguration.toString()));
        modelReportBuilder.append(System.getProperty("line.separator"));
    }

    private void conversionValidationReportFlowSection(VerificationData verificationData,
            StringBuilder modelReportBuilder)
            throws IOException {

        long totalBranchEnds = verificationData.flowData.values().size();
        long nonCalculated = verificationData.nonCalculated();
        long ko = verificationData.failedCount();
        long ok = totalBranchEnds - (nonCalculated + ko);

        LOG.debug("FLOW -- total branch ends;non-calculated;ko;ok");
        LOG.debug("{};{};{};{}", totalBranchEnds, nonCalculated, ko, ok);

        TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, true);
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        Column[] columns = new Column[] {
            new Column("total branch ends"),
            new Column("non-calculated"),
            new Column("ko"),
            new Column("ok")
        };
        try (Writer writer = new StringWriter()) {
            TableFormatter formatter = factory.create(writer, "FLOW", config, columns);
            try {
                formatter
                        .writeCell((int) totalBranchEnds)
                        .writeCell((int) nonCalculated)
                        .writeCell((int) ko)
                        .writeCell((int) ok);
            } catch (IOException x) {
                // Ignored
            }
            modelReportBuilder.append(writer.toString());
        }
    }

    private void conversionValidationReportBranchEndSection(VerificationData verificationData,
            StringBuilder modelReportBuilder) {

        if (verificationData.failedCount() == 0) {
            return;
        }

        Comparator<Map.Entry<String, FlowData>> byFlowError = (
                Entry<String, FlowData> o1,
                Entry<String, FlowData> o2) -> {
            return Double.compare(o1.getValue().flowError(), o2.getValue().flowError());
        };

        Map<String, FlowData> sortedByModelReport = verificationData.flowData.entrySet().stream()
                .sorted(byFlowError.reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));

        LOG.debug("BRANCH END -- id;type;pCgmes;qCgmes;pIidm;qIidm");
        TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, true);
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        Column[] columns = new Column[] {
            new Column("id"),
            new Column("type"),
            new Column("pCgmes"),
            new Column("qCgmes"),
            new Column("pIidm"),
            new Column("qIidm")
        };

        try (Writer writer = new StringWriter()) {
            TableFormatter formatter = factory.create(writer, "BRANCH END", config, columns);
            sortedByModelReport.entrySet().stream().filter(e -> e.getValue().flowError() > VerificationData.FLOW_THRESHOLD).limit(SHOW_BRANCH_ENDS).forEach(e -> {
                LOG.debug("{},{},{},{},{}",
                        e.getValue().id,
                        e.getValue().code(),
                        e.getValue().pCgmes,
                        e.getValue().qCgmes,
                        e.getValue().pIidm,
                        e.getValue().qIidm);
                try {
                    formatter
                            .writeCell(e.getValue().id)
                            .writeCell(e.getValue().code())
                            .writeCell(e.getValue().pCgmes)
                            .writeCell(e.getValue().qCgmes)
                            .writeCell(e.getValue().pIidm)
                            .writeCell(e.getValue().qIidm);
                } catch (IOException x) {
                    // Ignored
                }
            });
            modelReportBuilder.append(writer.toString());
        } catch (IOException e) {
            // Ignored
        }
    }

    private final Path  output;

    static final Logger LOG = LoggerFactory.getLogger(ConversionValidationReport.class);
}
