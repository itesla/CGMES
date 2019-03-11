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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.interpretation.InterpretationResult.ValidationData;
import com.powsybl.triplestore.api.PropertyBag;

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
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
                .forEach(mappingConfiguration -> interpretationReport(model, mappingConfiguration,
                        mappingConfigurationData.get(mappingConfiguration), modelReportBuilder));
        return modelReportBuilder.toString();
    }

    private void interpretationReport(String model, CgmesEquipmentModelMapping mappingConfiguration,
            ValidationData validationData, StringBuilder modelReportBuilder) {
        interpretationReportHeaderSection(model, mappingConfiguration, validationData,
                modelReportBuilder);
        interpretationReportBalanceSection(validationData, modelReportBuilder);
        interpretationReportNonBadVoltageNodesSection(validationData, modelReportBuilder);
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
            StringBuilder modelReportBuilder) {

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
                "total error {} total nodes {} isolated nodes {} non-calculated nodes {} ok nodes {} bad error {} bad nodes {} pct {} badVoltage error {} badVoltage nodes {} pct {}",
                validationData.balance, totalNodes, isolatedNodes, notCalculatedNodes, okNodes, badNodesError, badNodes,
                Long.valueOf(badNodes).doubleValue()
                        / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0,
                badVoltageNodesError, badVoltageNodes,
                Long.valueOf(badVoltageNodes).doubleValue()
                        / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0);
        modelReportBuilder.append(String.format(
                "BALANCE --- total error %f total nodes %d isolated nodes %d non-calculated nodes %d ok nodes %d bad error %f bad nodes %d pct %f badVoltage error %f badVoltage nodes %d pct %f",
                validationData.balance, totalNodes, isolatedNodes, notCalculatedNodes, okNodes, badNodesError, badNodes,
                Long.valueOf(badNodes).doubleValue()
                        / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0,
                badVoltageNodesError, badVoltageNodes,
                Long.valueOf(badVoltageNodes).doubleValue()
                        / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0));
        modelReportBuilder.append(System.getProperty("line.separator"));
    }

    private void interpretationReportNonBadVoltageNodesSection(ValidationData validationData,
            StringBuilder modelReportBuilder) {
        boolean showOnlyBadVoltageNodes = false;
        interpretationReportNodesSection("NODES", validationData, modelReportBuilder, showOnlyBadVoltageNodes);
    }

    private void interpretationReportBadVoltageNodesSection(ValidationData validationData,
            StringBuilder modelReportBuilder) {
        boolean showOnlyBadVoltageNodes = true;
        interpretationReportNodesSection("BAD VOLTAGE", validationData, modelReportBuilder, showOnlyBadVoltageNodes);
    }

    private void interpretationReportNodesSection(String prefix, ValidationData validationData,
            StringBuilder modelReportBuilder, boolean showOnlyBadVoltageNodes) {
        validationData.balanceData.keySet().stream().filter(nodes -> {
            PropertyBag pb = validationData.balanceData.get(nodes);
            double nodeBalanceP = pb.asDouble("balanceP");
            double nodeBalanceQ = pb.asDouble("balanceQ");
            boolean calculatedNode = pb.asBoolean("calculated", false);
            boolean badVoltage = pb.asBoolean("badVoltage", false);
            boolean badVoltageNode = calculatedNode && badVoltage
                    && Math.abs(nodeBalanceP) + Math.abs(nodeBalanceQ) > BALANCE_TOLERANCE;
            return !showOnlyBadVoltageNodes && !badVoltageNode || showOnlyBadVoltageNodes && badVoltageNode;
        }).limit(SHOW_NODES).forEach(nodes -> {
            PropertyBag pb = validationData.balanceData.get(nodes);
            double nodeBalanceP = pb.asDouble("balanceP");
            double nodeBalanceQ = pb.asDouble("balanceQ");
            boolean calculatedNode = pb.asBoolean("calculated", false);
            boolean isolatedNode = pb.asBoolean("isolated", false);
            boolean badVoltage = pb.asBoolean("badVoltage", false);
            int lines = pb.asInt("line");
            int t2xs = pb.asInt("t2x");
            int t3xs = pb.asInt("t3x");
            boolean okNode = calculatedNode && Math.abs(nodeBalanceP) + Math.abs(nodeBalanceQ) <= BALANCE_TOLERANCE;
            boolean badNode = calculatedNode && !badVoltage
                    && Math.abs(nodeBalanceP) + Math.abs(nodeBalanceQ) > BALANCE_TOLERANCE;
            boolean badVoltageNode = calculatedNode && badVoltage
                    && Math.abs(nodeBalanceP) + Math.abs(nodeBalanceQ) > BALANCE_TOLERANCE;
            LOG.debug(
                    "id {} isolated {} calculated {} ok {} bad {} badVoltage {} balance {} {} lines {} t2xs {} t3xs {} nodes {}",
                    nodes.iterator().next(), isolatedNode, calculatedNode, okNode, badNode, badVoltageNode,
                    nodeBalanceP,
                    nodeBalanceQ, lines, t2xs, t3xs, nodes);
            modelReportBuilder.append(String.format(
                    "%s --- id %s isolated %b calculated %b ok %b bad %b badVoltage %b balance %f %f lines %d t2xs %d t3xs %d nodes %s",
                    prefix, nodes.iterator().next(), isolatedNode, calculatedNode, okNode, badNode, badVoltageNode,
                    nodeBalanceP,
                    nodeBalanceQ, lines, t2xs, t3xs, nodes));
            modelReportBuilder.append(System.getProperty("line.separator"));
        });
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

        sortedByModelReport.putAll(validationData.detectedModelData);
        sortedByModelReport.keySet().forEach(model -> {
            LOG.debug("total {} calculated {} ok {} model {} evaluation {}",
                    sortedByModelReport.get(model).total,
                    sortedByModelReport.get(model).calculated,
                    sortedByModelReport.get(model).ok,
                    sortedByModelReport.get(model).code(),
                    sortedByModelReport.get(model).conversionCode());
            modelReportBuilder.append(String.format("DETECTED MODEL --- total %d calculated %d ok %d model %s evaluation %s",
                    sortedByModelReport.get(model).total,
                    sortedByModelReport.get(model).calculated,
                    sortedByModelReport.get(model).ok,
                    sortedByModelReport.get(model).code(),
                    sortedByModelReport.get(model).conversionCode()));
            modelReportBuilder.append(System.getProperty("line.separator"));
        });
    }

    private final Path  output;

    static final Logger LOG = LoggerFactory.getLogger(InterpretationsReport.class);
}
