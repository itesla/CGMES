package com.powsybl.cgmes.validation.test.flow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;

public class FlowValidation {

    public FlowValidation(CgmesModel m) {
        LOG.debug("Time {} PrepareModel", DateTime.now());
        inputModel = new PrepareModel(m);
        modelReport = new StringBuilder();
        bestError = Double.MAX_VALUE;
    }

    public void modelValidation(String modelname) throws IOException {
        inputModel.loadModel();
        Map<String, ValidationData> mappingConfigurationData = calculateAllMappingConfigurationBalances();
        generateModelReport(modelname, mappingConfigurationData);
    }

    public void testBalances(String modelname) throws IOException {
        Map<String, ValidationData> mappingConfigurationData = calculateAllMappingConfigurationBalances();
        generateModelReport(modelname, mappingConfigurationData);
    }

    public void setInputModel(PrepareModel inputModel) {
        this.inputModel = inputModel;
    }

    public String getReport() {
        return modelReport.toString();
    }

    public double getBestError() {
        return bestError;
    }

    private void addMappingConfigurations(List<String> configs) {
        configs.add("default");
        configs.add("T2x_yshunt_split");
        configs.add("T2x_yshunt_split.T2x_ratio0_end1");
        configs.add("T2x_ratio0_end1");
        configs.add("T2x_ratio0_rtc.T2x_ptc2_tabular_negate_on");
        configs.add("T2x_yshunt_split.T3x_yshunt_split");
        configs.add("T2x_yshunt_split.T3x_yshunt_split.Line_ratio0_on");
        configs.add("T2x_clock_on.T3x_clock_on_inside.T2x_pac2_negate_on");
        configs.add("T2x_yshunt_split.T3x_yshunt_split.T2x_clock_on.T3x_clock_on_inside.T2x_pac2_negate_on");
        configs.add("T2x_yshunt_split.T3x_yshunt_split.T2x_clock_on.T3x_clock_on_inside.T2x_pac2_negate_on.Line_ratio0_on");
        configs.add("T3x_ratio0_outside");

    }

    private void generateModelReport(String model,
            Map<String, ValidationData> mappingConfigurationData) {

        Comparator<Map.Entry<String, ValidationData>> byBalance = (
                Entry<String, ValidationData> o1,
                Entry<String, ValidationData> o2) -> {
            return Double.compare(o1.getValue().balance, o2.getValue().balance);
        };

        Map<String, ValidationData> sortedMappingConfigurationData = mappingConfigurationData
                .entrySet().stream()
                .sorted(byBalance)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));

        ValidationData bestConfig = sortedMappingConfigurationData.values().iterator().next();
        bestError = bestConfig.balance;

        StringBuilder modelReportBuilder = new StringBuilder();
        sortedMappingConfigurationData.keySet()
                .forEach(mappingConfiguration -> validationReport(model, mappingConfiguration,
                        mappingConfigurationData.get(mappingConfiguration), modelReportBuilder));
        modelReport.append(modelReportBuilder.toString());
    }

    private void validationReport(String model, String mappingConfiguration,
            ValidationData validationData, StringBuilder modelReportBuilder) {
        validationReportHeaderSection(model, mappingConfiguration, validationData,
                modelReportBuilder);
        validationReportBalanceSection(validationData, modelReportBuilder);
        validationReportNodesSection(validationData, modelReportBuilder);
        validationReportModelCodeSection(validationData, modelReportBuilder);
    }

    private void validationReportHeaderSection(String model, String mappingConfiguration,
            ValidationData validationData, StringBuilder modelReportBuilder) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("HH:mm");
        LOG.debug("----> model {} time {} config {}", model,
                fmt.print(inputModel.getCgmes().scenarioTime()), mappingConfiguration);
        modelReportBuilder.append(String.format("----> model %s time %s config %s", model,
                fmt.print(inputModel.getCgmes().scenarioTime()), mappingConfiguration));
        modelReportBuilder.append(System.getProperty("line.separator"));
    }

    private void validationReportBalanceSection(ValidationData validationData,
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
                "total error {} total nodes {} isolated nodes {} notCalculated nodes {} ok nodes {} bad error {} bad nodes {} pct {} badVoltage error {} badVoltage nodes {} pct {}",
                validationData.balance, totalNodes, isolatedNodes, notCalculatedNodes, okNodes, badNodesError, badNodes,
                Long.valueOf(badNodes).doubleValue()
                        / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0,
                badVoltageNodesError, badVoltageNodes,
                Long.valueOf(badVoltageNodes).doubleValue()
                        / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0);
        modelReportBuilder.append(String.format(
                "total error %f total nodes %d isolated nodes %d notCalculated nodes %d ok nodes %d bad error %f bad nodes %d pct %f badVoltage error %f badVoltage nodes %d pct %f",
                validationData.balance, totalNodes, isolatedNodes, notCalculatedNodes, okNodes, badNodesError, badNodes,
                Long.valueOf(badNodes).doubleValue()
                        / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0,
                badVoltageNodesError, badVoltageNodes,
                Long.valueOf(badVoltageNodes).doubleValue()
                        / Long.valueOf(totalNodes - isolatedNodes).doubleValue() * 100.0));
        modelReportBuilder.append(System.getProperty("line.separator"));
    }

    private void validationReportNodesSection(ValidationData validationData,
            StringBuilder modelReportBuilder) {
        show = 5;
        validationData.balanceData.keySet().forEach(nodes -> {
            if (show > 0) {
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
                boolean badNode = calculatedNode && !badVoltage && Math.abs(nodeBalanceP) + Math.abs(nodeBalanceQ) > BALANCE_TOLERANCE;
                boolean badVoltageNode = calculatedNode && badVoltage && Math.abs(nodeBalanceP) + Math.abs(nodeBalanceQ) > BALANCE_TOLERANCE;
                LOG.debug(
                        "id {} isolated {} calculated {} ok {} bad {} badVoltage {} balance {} {} lines {} t2xs {} t3xs {} nodes {}",
                        nodes.iterator().next(), isolatedNode, calculatedNode, okNode, badNode, badVoltageNode, nodeBalanceP,
                        nodeBalanceQ, lines, t2xs, t3xs, nodes);
                modelReportBuilder.append(String.format(
                        "id %s isolated %b calculated %b ok %b bad %b badVoltage %b balance %f %f lines %d t2xs %d t3xs %d nodes %s",
                        nodes.iterator().next(), isolatedNode, calculatedNode, okNode, badNode, badVoltageNode, nodeBalanceP,
                        nodeBalanceQ, lines, t2xs, t3xs, nodes));
                modelReportBuilder.append(System.getProperty("line.separator"));
                show--;
            }
        });

    }

    private void validationReportModelCodeSection(ValidationData validationData,
            StringBuilder modelReportBuilder) {
        Map<String, Triple<Integer, Integer, Integer>> sortedByModelCodeReport = new TreeMap<String, Triple<Integer, Integer, Integer>>(
                new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        if (s1.length() == s2.length()) {
                            return s1.compareTo(s2);
                        }
                        return Integer.compare(s1.length(), s2.length());
                    }
                });

        sortedByModelCodeReport.putAll(validationData.modelCodeData);
        sortedByModelCodeReport.keySet().forEach(code -> {
            LOG.debug("total {} calculated {} ok {} code {} --- {}",
                    sortedByModelCodeReport.get(code).getLeft(),
                    sortedByModelCodeReport.get(code).getMiddle(),
                    sortedByModelCodeReport.get(code).getRight(),
                    code, evaluateCode((String) code));
            modelReportBuilder.append(String.format("total %d calculated %d ok %d code %s --- %s",
                    sortedByModelCodeReport.get(code).getLeft(),
                    sortedByModelCodeReport.get(code).getMiddle(),
                    sortedByModelCodeReport.get(code).getRight(),
                    code, evaluateCode((String) code)));
            modelReportBuilder.append(System.getProperty("line.separator"));
        });
    }

    private Map<String, ValidationData> calculateAllMappingConfigurationBalances() {

        List<String> configs = new ArrayList<>();
        addMappingConfigurations(configs);

        Map<String, ValidationData> mappingConfigurationData = new HashMap<>();
        configs.forEach(config -> {
            ValidationData validationData = calculateBalance(config);
            mappingConfigurationData.put(config, validationData);
        });
        return mappingConfigurationData;
    }

    private ValidationData calculateBalance(String config) {

        ValidationData validationData = new ValidationData();
        Map<List<String>, PropertyBag> balanceData = new HashMap<>();
        List<String> pn = new ArrayList<>(
                Arrays.asList("balanceP", "balanceQ", "calculated", "line", "t2x", "t3x"));

        inputModel.getJoinedNodes().forEach(nodes -> {
            Map<String, Integer> nodeModelCodeData = new HashMap<>();
            PropertyBag nodeBalanceData = initNodeBalanceData(pn);
            Boolean isIsolatedNodes = inputModel.getIsolatedNodes().get(nodes);
            if (isIsolatedNodes != null && isIsolatedNodes) {
                isolateNodeBalance(nodeBalanceData);
            } else {
                calculateJoinedNodeBalance(config, nodes, nodeBalanceData, nodeModelCodeData);
            }
            writeToModelCodeData(validationData.modelCodeData, nodeModelCodeData,
                    nodeBalanceData);
            writeToBalanceData(pn, balanceData, nodes, nodeBalanceData);
        });

        Comparator<Map.Entry<List<String>, PropertyBag>> byBalance = (
                Entry<List<String>, PropertyBag> o1,
                Entry<List<String>, PropertyBag> o2) -> {
            return Double.compare(
                    Math.abs(o1.getValue().asDouble("balanceP", 0.0))
                            + Math.abs(o1.getValue().asDouble("balanceQ", 0.0)),
                    Math.abs(o2.getValue().asDouble("balanceP", 0.0))
                            + Math.abs(o2.getValue().asDouble("balanceQ", 0.0)));
        };

        validationData.balanceData = balanceData.entrySet().stream()
                .sorted(byBalance.reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));

        double totalError = validationData.balanceData.values().stream().filter(pb -> {
            return pb.asBoolean("calculated", false) && !pb.asBoolean("isolated", false);
        }).map(pb -> {
            return Math.abs(pb.asDouble("balanceP")) + Math.abs(pb.asDouble("balanceQ"));
        }).mapToDouble(Double::doubleValue).sum();

        validationData.balance = totalError;

        return validationData;
    }

    private void calculateJoinedNodeBalance(String config, List<String> nodes,
            PropertyBag nodeBalanceData,
            Map<String, Integer> nodeModelCodeReport) {
        nodes.forEach(n -> {
            if (n == null) {
                LOG.warn("Node null");
                return;
            }
            PropertyBag node = inputModel.getNodeParameters(n);
            Objects.requireNonNull(node, "node without parameters");
            if (inputModel.getEquipmentsInNode().containsKey(n)) {
                inputModel.getEquipmentsInNode().get(n).forEach(id -> {
                    boolean isLine = false;
                    boolean isT2x = false;
                    boolean isT3x = false;
                    CalcFlow calcFlow = new CalcFlow(inputModel);
                    PropertyBag line = inputModel.getLineParameters(id);
                    if (line != null) {
                        PropertyBag node1 = inputModel
                                .getNodeParameters(line.get("terminal1"));
                        Objects.requireNonNull(node1, "node1 null in line");
                        PropertyBag node2 = inputModel
                                .getNodeParameters(line.get("terminal2"));
                        Objects.requireNonNull(node2, "node2 null in line");
                        calcFlow.calcFlowLine(n, node1, node2, line, config);
                        isLine = true;
                    }
                    PropertyBag transformer = inputModel.getTransformerParameters(id);
                    if (transformer != null) {
                        PropertyBag node1 = inputModel
                                .getNodeParameters(transformer.get("terminal1"));
                        Objects.requireNonNull(node1, "node1 null in transformer");
                        PropertyBag node2 = inputModel
                                .getNodeParameters(transformer.get("terminal2"));
                        Objects.requireNonNull(node2, "node2 null in transformer");
                        PropertyBag node3 = inputModel
                                .getNodeParameters(transformer.get("terminal3"));
                        if (node3 == null) {
                            calcFlow.calcFlowT2x(n, node1, node2, transformer, config);
                            isT2x = true;
                        } else {
                            calcFlow.calcFlowT3x(n, node1, node2, node3, transformer,
                                    config);
                            isT3x = true;
                        }
                    }

                    writeToNodeModelCodeData(nodeModelCodeReport,
                            calcFlow.getModelCode());
                    writeToNodeBalanceData(nodeBalanceData, calcFlow, isLine, isT2x,
                            isT3x);

                    if (line != null) {
                        LOG.debug("Line {}  Line P {} Q {} balanceP {} balanceQ {}", line,
                                line.asDouble("p"), line.asDouble("q"),
                                node.asDouble("balanceP", 0.0),
                                node.asDouble("balanceQ", 0.0));
                    }
                    if (transformer != null) {
                        LOG.debug("Transformer {} P {} Q {} ", transformer,
                                transformer.asDouble("p"),
                                transformer.asDouble("q"));
                    }
                });
            }

            addNodeInjectionToJoinedBusBalance(node, nodeBalanceData);

            LOG.debug("node {} ,  {}", n, inputModel.getEquipmentsInNode().get(n));
            LOG.debug("node {} P {} Q {} balanceP {} balanceQ {}", n, node.asDouble("p"),
                    node.asDouble("q"), nodeBalanceData.asDouble("balanceP"),
                    nodeBalanceData.asDouble("balanceQ"));
        });
    }

    private void addNodeInjectionToJoinedBusBalance(PropertyBag node,
            PropertyBag nodeBalanceData) {
        double p = node.asDouble("p");
        double q = node.asDouble("q");
        double nodeBalanceP = nodeBalanceData.asDouble("balanceP");
        double nodeBalanceQ = nodeBalanceData.asDouble("balanceQ");
        nodeBalanceData.put("balanceP", Double.toString(nodeBalanceP + p));
        nodeBalanceData.put("balanceQ", Double.toString(nodeBalanceQ + q));
    }

    private PropertyBag initNodeBalanceData(List<String> pn) {
        PropertyBag nodeBalanceReport = new PropertyBag(pn);
        nodeBalanceReport.put("balanceP", Double.toString(0.0));
        nodeBalanceReport.put("balanceQ", Double.toString(0.0));
        nodeBalanceReport.put("calculated", "true");
        nodeBalanceReport.put("isolated", "false");
        nodeBalanceReport.put("badVoltage", "false");
        nodeBalanceReport.put("line", Integer.toString(0));
        nodeBalanceReport.put("t2x", Integer.toString(0));
        nodeBalanceReport.put("t3x", Integer.toString(0));
        return nodeBalanceReport;
    }

    private void isolateNodeBalance(PropertyBag nodeBalanceData) {
        nodeBalanceData.put("isolated", "true");
        nodeBalanceData.put("calculated", "false");
        nodeBalanceData.put("badVoltage", "false");
    }

    private void writeToNodeBalanceData(PropertyBag nodeBalanceData, CalcFlow calcFlow,
            boolean isLine, boolean isT2x, boolean isT3x) {
        if (calcFlow.getCalculated()) {
            double balanceP = nodeBalanceData.asDouble("balanceP");
            double balanceQ = nodeBalanceData.asDouble("balanceQ");
            nodeBalanceData.put("balanceP",
                    Double.toString(balanceP + calcFlow.getP()));
            nodeBalanceData.put("balanceQ",
                    Double.toString(balanceQ + calcFlow.getQ()));
            if (calcFlow.getBadVoltage()) {
                nodeBalanceData.put("badVoltage",
                        Boolean.toString(calcFlow.getBadVoltage()));
            }
        } else {
            nodeBalanceData.put("calculated",
                    Boolean.toString(calcFlow.getCalculated()));
        }
        if (isLine) {
            int lines = nodeBalanceData.asInt("line");
            nodeBalanceData.put("line", Integer.toString(lines + 1));
        }
        if (isT2x) {
            int t2xs = nodeBalanceData.asInt("t2x");
            nodeBalanceData.put("t2x", Integer.toString(t2xs + 1));
        }
        if (isT3x) {
            int t3xs = nodeBalanceData.asInt("t3x");
            nodeBalanceData.put("t3x", Integer.toString(t3xs + 1));
        }
    }

    private void writeToNodeModelCodeData(Map<String, Integer> modelCodeData, String code) {
        if (code.isEmpty()) {
            return;
        }
        Integer total = modelCodeData.get(code);
        if (total == null) {
            total = new Integer(0);
        }
        modelCodeData.put(code, total + 1);
    }

    private void writeToBalanceData(List<String> pn,
            Map<List<String>, PropertyBag> balanceData, List<String> nodes,
            PropertyBag nodeBalanceData) {
        boolean calculatedNodes = nodeBalanceData.asBoolean("calculated", true);
        boolean isolatedNodes = nodeBalanceData.asBoolean("isolated", true);
        boolean badVoltageNodes = nodeBalanceData.asBoolean("badVoltage", true);
        int nodeLines = nodeBalanceData.asInt("line");
        int nodeT2xs = nodeBalanceData.asInt("t2x");
        int nodeT3xs = nodeBalanceData.asInt("t3x");

        PropertyBag pb = new PropertyBag(pn);

        if (calculatedNodes && !isolatedNodes) {
            double nodeBalanceP = nodeBalanceData.asDouble("balanceP");
            double nodeBalanceQ = nodeBalanceData.asDouble("balanceQ");
            pb.put("balanceP", Double.toString(nodeBalanceP));
            pb.put("balanceQ", Double.toString(nodeBalanceQ));
        } else {
            pb.put("balanceP", Double.toString(0.0));
            pb.put("balanceQ", Double.toString(0.0));
        }
        pb.put("calculated", Boolean.toString(calculatedNodes));
        pb.put("badVoltage", Boolean.toString(badVoltageNodes));
        pb.put("isolated", Boolean.toString(isolatedNodes));
        pb.put("line", Integer.toString(nodeLines));
        pb.put("t2x", Integer.toString(nodeT2xs));
        pb.put("t3x", Integer.toString(nodeT3xs));
        balanceData.put(nodes, pb);
    }

    private void writeToModelCodeData(
            Map<String, Triple<Integer, Integer, Integer>> modelCodeData,
            Map<String, Integer> nodeModelCodeData, PropertyBag nodeBalanceData) {

        nodeModelCodeData.keySet().forEach(code -> {
            int totalCode = nodeModelCodeData.get(code);
            Triple<Integer, Integer, Integer> value = modelCodeData.get(code);
            if (value == null) {
                value = new ImmutableTriple<Integer, Integer, Integer>(0, 0, 0);
            }
            int total = value.getLeft();
            int calculated = value.getMiddle();
            int ok = value.getRight();
            double nodeBalanceP = nodeBalanceData.asDouble("balanceP");
            double nodeBalanceQ = nodeBalanceData.asDouble("balanceQ");
            boolean calculatedNodes = nodeBalanceData.asBoolean("calculated", true);
            boolean isolatedNodes = nodeBalanceData.asBoolean("isolated", false);
            if (calculatedNodes && !isolatedNodes) {
                if (Math.abs(nodeBalanceP) + Math.abs(nodeBalanceQ) <= BALANCE_TOLERANCE) {
                    value = new ImmutableTriple<Integer, Integer, Integer>(total + totalCode,
                            calculated + totalCode, ok + totalCode);
                } else {
                    value = new ImmutableTriple<Integer, Integer, Integer>(total + totalCode,
                            calculated + totalCode, ok);
                }
            } else {
                value = new ImmutableTriple<Integer, Integer, Integer>(total + totalCode,
                        calculated, ok);
            }
            modelCodeData.put(code, value);
        });
    }

    private String evaluateCode(String code) {
        // Line model code always ok
        // T2x model code
        if (code.length() == 6) {
            String code1 = code.substring(0, 3);
            String code2 = code.substring(3, 6);

            code2 = code2.replace("Y", "Y(ko)");
            code2 = code2.replace("R", "R(T)");
            code2 = code2.replace("P", "P(T)");
            String evalCode = code1 + code2;
            if (evalCode.contains("(T)") || evalCode.contains("(ko)")) {
                return evalCode;
            }
            // T3x model code
        } else if (code.length() == 20) {
            String code1 = code.substring(0, 6);
            String code11 = code1.substring(0, 3);
            String code12 = code1.substring(3, 6);
            String code2 = code.substring(7, 13);
            String code21 = code2.substring(0, 3);
            String code22 = code2.substring(3, 6);
            String code3 = code.substring(14, 20);
            String code31 = code3.substring(0, 3);
            String code32 = code3.substring(3, 6);

            code11 = code11.replace("Y", "Y(ko)");
            code21 = code21.replace("Y", "Y(T)");
            code22 = code22.replace("Y", "Y(ko)");
            code31 = code31.replace("Y", "Y(T)");
            code32 = code32.replace("Y", "Y(ko)");
            code11 = code11.replace("P", "P(ko)");
            code12 = code12.replace("P", "P(ko)");
            code21 = code21.replace("P", "P(ko)");
            code22 = code22.replace("P", "P(ko)");
            code31 = code31.replace("P", "P(ko)");
            code32 = code32.replace("P", "P(ko)");
            code21 = code21.replace("R", "R(T)");
            code31 = code31.replace("R", "R(T)");
            if (code1.contains("R")) {
                if (code2.contains("R") && code3.contains("R")) {
                    code11 = code11.replace("R", "R(ko)");
                    code12 = code12.replace("R", "R(ko)");
                } else {
                    code11 = code11.replace("R", "R(T)");
                    code12 = code12.replace("R", "R(T)");
                }
            }

            String evalCode = code11 + code12 + "." + code21 + code22 + "." + code31 + code32;
            if (evalCode.contains("(T)") || evalCode.contains("(ko)")) {
                return evalCode;
            }
        }

        return "ok";
    }

    class ValidationData {
        double                                         balance;
        Map<List<String>, PropertyBag>                 balanceData;
        Map<String, Triple<Integer, Integer, Integer>> modelCodeData;

        ValidationData() {
            balance = 0.0;
            balanceData = new HashMap<>();
            modelCodeData = new HashMap<>();
        }
    }

    private static final double BALANCE_TOLERANCE = 1.0;
    private static int          show;
    private static PrepareModel inputModel;
    private double              bestError;
    private StringBuilder       modelReport;

    private static final Logger LOG               = LoggerFactory
            .getLogger(FlowValidation.class);

}
