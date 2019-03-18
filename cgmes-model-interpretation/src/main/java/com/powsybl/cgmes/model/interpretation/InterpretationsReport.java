/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

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
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.interpretation.InterpretationResult.ValidationData;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.CsvTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author José Antonio Marqués <marquesja at aia.es>, Marcos de Miguel <demiguelm at aia.es>
 */
public class InterpretationsReport {

    private static final double BALANCE_TOLERANCE = 1.0;
    private static final int    SHOW_NODES        = 5;

    public InterpretationsReport(Path output) {
        this.output = output;
    }

    public void report(Map<String, InterpretationResult> interpretations) throws IOException {
        Comparator<Map.Entry<String, InterpretationResult>> byError = (
                Entry<String, InterpretationResult> o1,
                Entry<String, InterpretationResult> o2) -> {
            return Double.compare(o1.getValue().error, o2.getValue().error);
        };
        Map<String, InterpretationResult> sortedResults = interpretations.entrySet().stream()
                .sorted(byError.reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));
        dump(sortedResults);
    }

    private void dump(Map<String, InterpretationResult> interpretations) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime dateTime = LocalDateTime.now();
        String formattedDateTime = dateTime.format(formatter);
        try (BufferedWriter w = Files.newBufferedWriter(
                output.resolve("CGMESModelInterpretationReport." + formattedDateTime), StandardCharsets.UTF_8)) {
            interpretations.entrySet().forEach(e -> {
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

    private String generateModelReport(String model, InterpretationResult interpretation) {

        if (interpretation.exception != null) {
            return interpretation.exception.getMessage();
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
                .entrySet().stream()
                .sorted(byBalance)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));

        StringBuilder modelReportBuilder = new StringBuilder();
        modelReportBuilder.append(String.format("----> MODEL %s ", model));
        modelReportBuilder.append(System.getProperty("line.separator"));
        sortedMappingConfigurationData.keySet()
                .forEach(mappingConfiguration -> {
                    try {
                        interpretationReport(model, mappingConfiguration,
                                mappingConfigurationData.get(mappingConfiguration), modelReportBuilder);
                    } catch (IOException e) {
                        // Ignored
                    }
                });
        return modelReportBuilder.toString();
    }

    private void interpretationReport(String model, CgmesEquipmentModelMapping mappingConfiguration,
            ValidationData validationData, StringBuilder modelReportBuilder) throws IOException {
        interpretationReportHeaderSection(model, mappingConfiguration, validationData,
                modelReportBuilder);
        interpretationReportBalanceSection(validationData, modelReportBuilder);
        interpretationReportBadNodesSection(validationData, modelReportBuilder);
        interpretationReportBadVoltageNodesSection(validationData, modelReportBuilder);
        interpretationReportModelSection(validationData, modelReportBuilder);
    }

    private void interpretationReportHeaderSection(String model, CgmesEquipmentModelMapping mappingConfiguration,
            ValidationData validationData, StringBuilder modelReportBuilder) {
        LOG.debug("----> MAPPING CONFIG {}", mappingConfiguration);
        modelReportBuilder.append(String.format("----> config %s", mappingConfiguration.toString()));
        modelReportBuilder.append(System.getProperty("line.separator"));
    }

    private void interpretationReportBalanceSection(ValidationData validationData,
            StringBuilder modelReportBuilder) throws IOException {

        long notCalculatedNodes = validationData.balanceData.entrySet().stream().filter(entry -> {
            return !entry.getValue().asBoolean("calculated", false) && !entry.getValue().asBoolean("isolated", false);
        }).count();

        long totalNodes = validationData.balanceData.values().size();
        long badNodes = validationData.balanceData.values().stream().filter(pb -> {
            return pb.asBoolean("calculated", false) && !pb.asBoolean("badVoltage", false);
        }).filter(pb -> {
            return (Math.abs(pb.asDouble("balanceP"))
                    + Math.abs(pb.asDouble("balanceQ"))) > BALANCE_TOLERANCE;
        }).count();

        long badVoltageNodes = validationData.balanceData.values().stream().filter(pb -> {
            return pb.asBoolean("calculated", false) && pb.asBoolean("badVoltage", false);
        }).filter(pb -> {
            return (Math.abs(pb.asDouble("balanceP"))
                    + Math.abs(pb.asDouble("balanceQ"))) > BALANCE_TOLERANCE;
        }).count();

        long okNodes = validationData.balanceData.values().stream().filter(pb -> {
            return pb.asBoolean("calculated", false);
        }).filter(pb -> {
            return (Math.abs(pb.asDouble("balanceP"))
                    + Math.abs(pb.asDouble("balanceQ"))) <= BALANCE_TOLERANCE;
        }).count();

        long isolatedNodes = validationData.balanceData.values().stream().filter(pb -> {
            return pb.asBoolean("isolated", false);
        }).count();

        double badVoltageNodesError = validationData.balanceData.values().stream().filter(pb -> {
            return pb.asBoolean("calculated", false) && pb.asBoolean("badVoltage", false);
        }).filter(pb -> {
            return (Math.abs(pb.asDouble("balanceP"))
                    + Math.abs(pb.asDouble("balanceQ"))) > BALANCE_TOLERANCE;
        }).map(pb -> {
            return Math.abs(pb.asDouble("balanceP"))
                    + Math.abs(pb.asDouble("balanceQ"));
        }).mapToDouble(Double::doubleValue).sum();

        double badNodesError = validationData.balanceData.values().stream().filter(pb -> {
            return pb.asBoolean("calculated", false) && !pb.asBoolean("badVoltage", false);
        }).filter(pb -> {
            return (Math.abs(pb.asDouble("balanceP"))
                    + Math.abs(pb.asDouble("balanceQ"))) > BALANCE_TOLERANCE;
        }).map(pb -> {
            return Math.abs(pb.asDouble("balanceP")) + Math.abs(pb.asDouble("balanceQ"));
        }).mapToDouble(Double::doubleValue).sum();

        LOG.debug(
                "BALANCE -- total error;total nodes;isolated nodes;non-calculated nodes;ok nodes;bad error;bad nodes;pct;badVoltage error;badVoltage nodes;pct");
        LOG.debug("{};{};{};{};{};{};{};{};{};{};{}",
                validationData.balance, totalNodes, isolatedNodes, notCalculatedNodes, okNodes, badNodesError, badNodes,
                Long.valueOf(badNodes).doubleValue()
                        / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0,
                badVoltageNodesError, badVoltageNodes,
                Long.valueOf(badVoltageNodes).doubleValue()
                        / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0);

        TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, true);
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        Column[] columns = new Column[] {
            new Column("total error"),
            new Column("total nodes"),
            new Column("isolated nodes"),
            new Column("non-calculated node"),
            new Column("ok nodes"),
            new Column("bad error"),
            new Column("bad nodes"),
            new Column("pct"),
            new Column("badVoltage error"),
            new Column("badVoltage nodes"),
            new Column("pct")
        };
        try (Writer writer = new StringWriter()) {
            TableFormatter formatter = factory.create(writer, "BALANCE", config, columns);
            try {
                formatter
                        .writeCell(validationData.balance)
                        .writeCell((int) totalNodes)
                        .writeCell((int) isolatedNodes)
                        .writeCell((int) notCalculatedNodes)
                        .writeCell((int) okNodes)
                        .writeCell(badNodesError)
                        .writeCell((int) badNodes)
                        .writeCell(Long.valueOf(badNodes).doubleValue()
                                / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0)
                        .writeCell(badVoltageNodesError)
                        .writeCell((int) badVoltageNodes)
                        .writeCell(Long.valueOf(badVoltageNodes).doubleValue()
                                / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0);
            } catch (IOException x) {
                // Ignored
            }
            modelReportBuilder.append(writer.toString());
        }
    }

    private void interpretationReportBadNodesSection(ValidationData validationData,
            StringBuilder modelReportBuilder) {
        boolean showOnlyBadNodes = true;
        boolean showOnlyBadVoltageNodes = false;
        interpretationReportNodesSection("BAD NODES", validationData, modelReportBuilder, showOnlyBadVoltageNodes,
                showOnlyBadNodes);
    }

    private void interpretationReportBadVoltageNodesSection(ValidationData validationData,
            StringBuilder modelReportBuilder) {
        boolean showOnlyBadNodes = false;
        boolean showOnlyBadVoltageNodes = true;
        interpretationReportNodesSection("BAD VOLTAGE NODES", validationData, modelReportBuilder,
                showOnlyBadVoltageNodes,
                showOnlyBadNodes);
    }

    private void interpretationReportNodesSection(String prefix, ValidationData validationData,
            StringBuilder modelReportBuilder, boolean showOnlyBadVoltageNodes,
            boolean showOnlyBadNodes) {
        LOG.debug("%s -- id;balanceP;balanceQ;lines;t2xs;t3xs;nodes", prefix);

        TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, true);
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        Column[] columns = new Column[] {
            new Column("id"),
            new Column("balanceP"),
            new Column("balanceQ"),
            new Column("lines"),
            new Column("t2xs"),
            new Column("t3xs"),
            new Column("nodes")
        };
        try (Writer writer = new StringWriter()) {
            TableFormatter formatter = factory.create(writer, prefix, config, columns);
            validationData.balanceData.keySet().stream().filter(nodes -> {
                PropertyBag pb = validationData.balanceData.get(nodes);
                double nodeBalanceP = pb.asDouble("balanceP");
                double nodeBalanceQ = pb.asDouble("balanceQ");
                boolean calculatedNode = pb.asBoolean("calculated", false);
                boolean badVoltage = pb.asBoolean("badVoltage", false);
                boolean badError = calculatedNode
                        && Math.abs(nodeBalanceP) + Math.abs(nodeBalanceQ) > BALANCE_TOLERANCE;
                return showOnlyBadNodes && !badVoltage && badError || showOnlyBadVoltageNodes && badVoltage && badError;
            }).limit(SHOW_NODES).forEach(nodes -> {
                PropertyBag pb = validationData.balanceData.get(nodes);
                double nodeBalanceP = pb.asDouble("balanceP");
                double nodeBalanceQ = pb.asDouble("balanceQ");
                int lines = pb.asInt("line");
                int t2xs = pb.asInt("t2x");
                int t3xs = pb.asInt("t3x");
                LOG.debug("{},{},{},{},{},{},{}", nodes.iterator().next(), nodeBalanceP, nodeBalanceQ, lines, t2xs,
                        t3xs,
                        nodes);
                try {
                    formatter
                            .writeCell(nodes.iterator().next())
                            .writeCell(nodeBalanceP)
                            .writeCell(nodeBalanceQ)
                            .writeCell(lines)
                            .writeCell(t2xs)
                            .writeCell(t3xs)
                            .writeCell(nodes.toString());
                } catch (IOException x) {
                    // Ignored
                }
            });
            modelReportBuilder.append(writer.toString());
        } catch (IOException e) {
            // Ignored
        }
    }

    private void interpretationReportModelSection(ValidationData validationData,
            StringBuilder modelReportBuilder) {
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

        LOG.debug("DETECTED MODEL -- code,total,calculated,ok,evaluationCode");
        TableFormatterConfig config = new TableFormatterConfig(Locale.US, ',', "-", true, true);
        CsvTableFormatterFactory factory = new CsvTableFormatterFactory();
        Column[] columns = new Column[] {
            new Column("code"),
            new Column("total"),
            new Column("calculated"),
            new Column("ok"),
            new Column("evaluationCode")
        };

        sortedByModelReport.putAll(validationData.detectedModelData);
        try (Writer writer = new StringWriter()) {
            TableFormatter formatter = factory.create(writer, "DETECTED MODEL", config, columns);
            sortedByModelReport.keySet().forEach(model -> {
                LOG.debug("{},{},{},{},{}",
                        sortedByModelReport.get(model).code(),
                        sortedByModelReport.get(model).total,
                        sortedByModelReport.get(model).calculated,
                        sortedByModelReport.get(model).ok,
                        sortedByModelReport.get(model).conversionCode());
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
            modelReportBuilder.append(writer.toString());
        } catch (IOException e) {
            // Ignored
        }
    }

    private final Path  output;

    static final Logger LOG = LoggerFactory.getLogger(InterpretationsReport.class);
}
