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

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;

public class CgmesFlowValidation {

    public CgmesFlowValidation(CgmesModel m) {
        inputModel = new PrepareModel(m);
        outputReport = new StringBuilder();
    }

    public void test(String modelname) throws IOException {
        inputModel.loadModel();
        calcBalances(modelname);
    }

    public String getReport() {
        return outputReport.toString();
    }

    private void addConfigurations(List<String> configs) {
        configs.add("default");
        configs.add("T2x_yshunt_split.T3x_yshunt_split"); // APG
        configs.add("T2x_yshunt_split.T2x_ratio0_end1"); // EMS
        configs.add("T2x_ratio0_rtc.T2x_ptc2_tabular_negate_on"); // RTE
        configs.add("T2x_clock_on.T3x_clock_on_inside.T2x_pac2_negate_on"); // NGET
        configs.add("T2x_yshunt_split"); // ELES
        configs.add("T2x_ratio0_end1"); // CGES
        configs.add(
                "T2x_clock_on.T3x_clock_on_inside.T2x_pac2_negate_on.T2x_yshunt_split.T3x_yshunt_split"); // Elia
        configs.add("T2x_yshunt_split.T3x_yshunt_split"); // 50Hz
    }

    private void calcBalances(String model) {

        String bestConfig = "none";
        double bestError = Double.MAX_VALUE;

        List<String> configs = new ArrayList<>();
        addConfigurations(configs);

        StringBuilder modelReport = new StringBuilder();
        for (String config : configs) {
            String outputText;
            Map<List<String>, PropertyBag> report = calcBalance(config);

            long totalNodes = report.values().size();
            long badNodes = report.values().stream().filter(pb -> {
                return pb.asBoolean("calculated", false);
            }).filter(pb -> {
                return (Math.abs(pb.asDouble("balanceP"))
                        + Math.abs(pb.asDouble("balanceQ"))) > BALANCE_TOLERANCE;
            }).count();

            double totalError = report.values().stream().filter(pb -> {
                return pb.asBoolean("calculated", false);
            }).map(pb -> {
                return Math.abs(pb.asDouble("balanceP")) + Math.abs(pb.asDouble("balanceQ"));
            }).mapToDouble(Double::doubleValue).sum();

            double badNodesError = report.values().stream().filter(pb -> {
                return pb.asBoolean("calculated", false);
            }).filter(pb -> {
                return (Math.abs(pb.asDouble("balanceP"))
                        + Math.abs(pb.asDouble("balanceQ"))) > BALANCE_TOLERANCE;
            }).map(pb -> {
                return Math.abs(pb.asDouble("balanceP")) + Math.abs(pb.asDouble("balanceQ"));
            }).mapToDouble(Double::doubleValue).sum();

            long okNodes = report.values().stream().filter(pb -> {
                return pb.asBoolean("calculated", false);
            }).filter(pb -> {
                return (Math.abs(pb.asDouble("balanceP"))
                        + Math.abs(pb.asDouble("balanceQ"))) <= BALANCE_TOLERANCE;
            }).count();

            if (totalError < bestError) {
                bestError = totalError;
                bestConfig = config;
            }

            LOG.debug("----> model {} config {}", model, config);
            modelReport.append(String.format("----> model %s config %s", model, config));
            modelReport.append(System.getProperty("line.separator"));

            LOG.debug(
                    "total error {} total nodes {} ok nodes {} bad error {} bad nodes {} pct {}",
                    totalError, totalNodes, okNodes, badNodesError, badNodes,
                    Long.valueOf(badNodes).doubleValue()
                            / Long.valueOf(totalNodes).doubleValue() * 100.0);
            modelReport.append(String.format(
                    "total error %f total nodes %d ok nodes %d bad error %f bad nodes %d pct %f",
                    totalError, totalNodes, okNodes, badNodesError, badNodes,
                    Long.valueOf(badNodes).doubleValue()
                            / Long.valueOf(totalNodes).doubleValue() * 100.0));
            modelReport.append(System.getProperty("line.separator"));

            show = 5;
            report.keySet().forEach(nodes -> {
                if (show > 0) {
                    PropertyBag pb = report.get(nodes);
                    double nodeBalanceP = pb.asDouble("balanceP");
                    double nodeBalanceQ = pb.asDouble("balanceQ");
                    boolean calculatedNode = pb.asBoolean("calculated", false);
                    int lines = pb.asInt("line");
                    int t2xs = pb.asInt("t2x");
                    int t3xs = pb.asInt("t3x");
                    LOG.debug("nodes {} calculated {} {} {} lines {} t2xs {} t3xs {}", nodes,
                            calculatedNode, nodeBalanceP,
                            nodeBalanceQ, lines, t2xs, t3xs);
                    modelReport.append(String.format(
                            "nodes %s calculated %b %f %f lines %d t2xs %d t3xs %d", nodes,
                            calculatedNode, nodeBalanceP, nodeBalanceQ, lines, t2xs, t3xs));
                    modelReport.append(System.getProperty("line.separator"));
                    show--;
                }
            });

            Map<String, Pair<Integer, Integer>> sortedByModelCodeReport = new TreeMap<String, Pair<Integer, Integer>>(
                    new Comparator<String>() {
                        @Override
                        public int compare(String s1, String s2) {
                            if (s1.length() == s2.length()) {
                                return s1.compareTo(s2);
                            }
                            return Integer.compare(s1.length(), s2.length());
                        }
                    });

            sortedByModelCodeReport.putAll(modelCodeReport);
            sortedByModelCodeReport.keySet().forEach(code -> {
                LOG.debug("total {} ok {} code {} --- {}",
                        sortedByModelCodeReport.get(code).getKey(),
                        sortedByModelCodeReport.get(code).getValue(),
                        code, evaluateCode((String) code));
                modelReport.append(String.format("total %d ok %d code %s --- %s",
                        sortedByModelCodeReport.get(code).getKey(),
                        sortedByModelCodeReport.get(code).getValue(),
                        code, evaluateCode((String) code)));
                modelReport.append(System.getProperty("line.separator"));
            });
        }

        outputReport.append(String.format("------> Model %s bestConfig %s bestError %f", model,
                bestConfig, bestError==Double.MAX_VALUE?Double.NaN:bestError));
        outputReport.append(System.getProperty("line.separator"));
        outputReport.append(modelReport.toString());
    }

    private Map<List<String>, PropertyBag> calcBalance(String config) {

        modelCodeReport = new HashMap<>();
        Map<List<String>, PropertyBag> balanceReport = new HashMap<>();
        List<String> pn = new ArrayList<>(
                Arrays.asList("balanceP", "balanceQ", "calculated", "line", "t2x", "t3x"));

        inputModel.getJoinedNodes().forEach(nodes -> {
            Map<String, Integer> nodeModelCodeReport = new HashMap<>();
            PropertyBag nodeBalanceData = initNodeBalanceData(pn);
            nodes.forEach(n -> {
                if (n == null) {
                    return;
                }
                PropertyBag node = inputModel.getNodeParameters(n);
                Objects.requireNonNull(node, "node without parameters");
                LOG.debug("------  node ----------> {}", n);
                if (n.startsWith("_c4e78550") || n.startsWith("_59f72142")) {
                    LOG.debug("node {}", node);
                }
                if (inputModel.getEquipmentsInNode().containsKey(n)) {
                    inputModel.getEquipmentsInNode().get(n).forEach(id -> {
                        boolean isLine = false;
                        boolean isT2x = false;
                        boolean isT3x = false;
                        CalcFlow calcFlow = new CalcFlow(inputModel);
                        PropertyBag line = inputModel.getLineParameters(id);
                        if (line != null) {
                            PropertyBag node1 = inputModel.getNodeParameters(line.get("terminal1"));
                            Objects.requireNonNull(node1, "node1 null in line");
                            PropertyBag node2 = inputModel.getNodeParameters(line.get("terminal2"));
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
                                calcFlow.calcFlowT3x(n, node1, node2, node3, transformer, config);
                                isT3x = true;
                            }
                        }

                        writeToNodeModelCodeReport(nodeModelCodeReport, calcFlow.getModelCode());
                        writeToNodeBalanceReport(nodeBalanceData, calcFlow, isLine, isT2x, isT3x);

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

            writeToModelCodeReport(modelCodeReport, nodeModelCodeReport, nodeBalanceData);
            writeToBalanceReport(pn, balanceReport, nodes, nodeBalanceData);
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

        Map<List<String>, PropertyBag> sortedByBalanceReport = balanceReport.entrySet().stream()
                .sorted(byBalance.reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));

        return sortedByBalanceReport;
    }

    private void addNodeInjectionToJoinedBusBalance(PropertyBag node,
            PropertyBag nodeBalanceReport) {
        double p = node.asDouble("p");
        double q = node.asDouble("q");
        double nodeBalanceP = nodeBalanceReport.asDouble("balanceP");
        double nodeBalanceQ = nodeBalanceReport.asDouble("balanceQ");
        nodeBalanceReport.put("balanceP", Double.toString(nodeBalanceP + p));
        nodeBalanceReport.put("balanceQ", Double.toString(nodeBalanceQ + q));
    }

    private PropertyBag initNodeBalanceData(List<String> pn) {
        PropertyBag nodeBalanceReport = new PropertyBag(pn);
        nodeBalanceReport.put("balanceP", Double.toString(0.0));
        nodeBalanceReport.put("balanceQ", Double.toString(0.0));
        nodeBalanceReport.put("calculated", "true");
        nodeBalanceReport.put("line", Integer.toString(0));
        nodeBalanceReport.put("t2x", Integer.toString(0));
        nodeBalanceReport.put("t3x", Integer.toString(0));
        return nodeBalanceReport;
    }

    private void writeToNodeBalanceReport(PropertyBag nodeBalanceReport, CalcFlow calcFlow,
            boolean isLine, boolean isT2x, boolean isT3x) {
        if (calcFlow.getCalculated()) {
            double balanceP = nodeBalanceReport.asDouble("balanceP");
            double balanceQ = nodeBalanceReport.asDouble("balanceQ");
            nodeBalanceReport.put("balanceP",
                    Double.toString(balanceP + calcFlow.getP()));
            nodeBalanceReport.put("balanceQ",
                    Double.toString(balanceQ + calcFlow.getQ()));
        } else {
            nodeBalanceReport.put("calculated",
                    Boolean.toString(calcFlow.getCalculated()));
        }
        if (isLine) {
            int lines = nodeBalanceReport.asInt("line");
            nodeBalanceReport.put("line", Integer.toString(lines + 1));
        }
        if (isT2x) {
            int t2xs = nodeBalanceReport.asInt("t2x");
            nodeBalanceReport.put("t2x", Integer.toString(t2xs + 1));
        }
        if (isT3x) {
            int t3xs = nodeBalanceReport.asInt("t3x");
            nodeBalanceReport.put("t3x", Integer.toString(t3xs + 1));
        }
    }

    private void writeToNodeModelCodeReport(Map<String, Integer> report, String code) {
        if (code.isEmpty()) {
            return;
        }
        Integer total = report.get(code);
        if (total == null) {
            total = new Integer(0);
        }
        report.put(code, total + 1);
    }

    private void writeToBalanceReport(List<String> pn,
            Map<List<String>, PropertyBag> balanceReport, List<String> nodes,
            PropertyBag nodeBalanceReport) {
        boolean calculatedNodes = nodeBalanceReport.asBoolean("calculated", true);
        int nodeLines = nodeBalanceReport.asInt("line");
        int nodeT2xs = nodeBalanceReport.asInt("t2x");
        int nodeT3xs = nodeBalanceReport.asInt("t3x");

        PropertyBag pb = new PropertyBag(pn);

        if (calculatedNodes) {
            double nodeBalanceP = nodeBalanceReport.asDouble("balanceP");
            double nodeBalanceQ = nodeBalanceReport.asDouble("balanceQ");
            pb.put("balanceP", Double.toString(nodeBalanceP));
            pb.put("balanceQ", Double.toString(nodeBalanceQ));
        } else {
            pb.put("balanceP", Double.toString(0.0));
            pb.put("balanceQ", Double.toString(0.0));
        }
        pb.put("calculated", Boolean.toString(calculatedNodes));
        pb.put("line", Integer.toString(nodeLines));
        pb.put("t2x", Integer.toString(nodeT2xs));
        pb.put("t3x", Integer.toString(nodeT3xs));
        balanceReport.put(nodes, pb);
    }

    private void writeToModelCodeReport(Map<String, Pair<Integer, Integer>> modelCodeReport,
            Map<String, Integer> nodeModelCodeReport, PropertyBag nodeBalanceReport) {

        nodeModelCodeReport.keySet().forEach(code -> {
            int totalCode = nodeModelCodeReport.get(code);
            Pair<Integer, Integer> value = modelCodeReport.get(code);
            if (value == null) {
                value = new MutablePair<Integer, Integer>(0, 0);
            }
            int total = value.getKey();
            int ok = value.getValue();
            double nodeBalanceP = nodeBalanceReport.asDouble("balanceP");
            double nodeBalanceQ = nodeBalanceReport.asDouble("balanceQ");
            boolean calculatedNodes = nodeBalanceReport.asBoolean("calculated", true);
            if (calculatedNodes
                    && Math.abs(nodeBalanceP) + Math.abs(nodeBalanceQ) <= BALANCE_TOLERANCE) {
                value = new MutablePair<Integer, Integer>(total + totalCode, ok + totalCode);
            } else {
                value = new MutablePair<Integer, Integer>(total + totalCode, ok);
            }
            modelCodeReport.put(code, value);
        });
    }

    private String evaluateCode(String code) {
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

    private static final double                        BALANCE_TOLERANCE = 1.0;
    private static int                                 show              = 5;
    private static PrepareModel                        inputModel;
    private static Map<String, Pair<Integer, Integer>> modelCodeReport;
    private StringBuilder                              outputReport;

    private static final Logger                        LOG               = LoggerFactory
            .getLogger(CgmesFlowValidation.class);
}
