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
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;

public class CgmesFlowValidation {

    public CgmesFlowValidation(CgmesModel m) {
        inputModel = new PrepareModel(m);
    }

    public void test(String modelname) throws IOException {
        inputModel.loadModel();
        calcBalances(modelname);
    }

    private void addConfigurations(List<String> configs) {
        configs.add("default");
        // configs.add("T2x_yshunt_split.T3x_yshunt_split"); // APG
        // configs.add("T2x_yshunt_split.T2x_ratio0_end1"); // EMS
        // configs.add("T2x_ratio0_rtc.T2x_ptc2_tabular_negate_on"); // RTE
        // configs.add("T2x_clock_on.T3x_clock_on_inside.T2x_pac2_negate_on"); // NGET
        // configs.add("T2x_yshunt_split"); // ELES
        // configs.add("T2x_ratio0_end1"); // CGES
        // configs.add("T2x_clock_on.T3x_clock_on_inside.T2x_pac2_negate_on.T2x_yshunt_split.T3x_yshunt_split");
        // // Elia
        // configs.add("T2x_yshunt_split.T3x_yshunt_split"); // 50Hz
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

    private void calcBalances(String model) {

        List<String> configs = new ArrayList<>();
        addConfigurations(configs);
        configs.forEach(config -> {
            Map<List<String>, PropertyBag> report = calcBalance(config);

            double balanceTolerance = 1.0;
            long totalNodes = report.values().size();
            long badNodes = report.values().stream().filter(pb -> {
                return pb.asBoolean("calculated", false);
            }).filter(pb -> {
                return (Math.abs(pb.asDouble("balanceP"))
                        + Math.abs(pb.asDouble("balanceQ"))) > balanceTolerance;
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
                        + Math.abs(pb.asDouble("balanceQ"))) > balanceTolerance;
            }).map(pb -> {
                return Math.abs(pb.asDouble("balanceP")) + Math.abs(pb.asDouble("balanceQ"));
            }).mapToDouble(Double::doubleValue).sum();

            long okNodes = report.values().stream().filter(pb -> {
                return pb.asBoolean("calculated", false);
            }).filter(pb -> {
                return (Math.abs(pb.asDouble("balanceP"))
                        + Math.abs(pb.asDouble("balanceQ"))) <= balanceTolerance;
            }).count();

            LOG.info("----> model {} config {}", model, config);

            LOG.info(
                    "total error {} total nodes {} ok nodes {} bad error {} bad nodes {} pct {}",
                    totalError, totalNodes, okNodes, badNodesError, badNodes,
                    Long.valueOf(badNodes).doubleValue()
                            / Long.valueOf(totalNodes).doubleValue() * 100.0);

            show = 5;
            report.keySet().forEach(nodes -> {
                if (nodes.contains("_09115273-16af-4309-8695-e77ab8de2f79_TP")) {
                    PropertyBag pb = report.get(nodes);
                    double nodeBalanceP = pb.asDouble("balanceP");
                    double nodeBalanceQ = pb.asDouble("balanceQ");
                    LOG.info("nodes {} {} {}", nodes, nodeBalanceP, nodeBalanceQ);
                }
                if (show > 0) {
                    PropertyBag pb = report.get(nodes);
                    double nodeBalanceP = pb.asDouble("balanceP");
                    double nodeBalanceQ = pb.asDouble("balanceQ");
                    boolean calculatedNode = pb.asBoolean("calculated", false);
                    LOG.info("nodes {} calculated {} {} {}", nodes, calculatedNode, nodeBalanceP, nodeBalanceQ);
                    show--;
                }
            });

            report.keySet().forEach(nodes -> {
                PropertyBag pb = report.get(nodes);
                if (!pb.asBoolean("calculated", false)) {
                    return;
                }
                nodes.forEach(n -> {
                    if (!inputModel.getEquipmentsInNode().containsKey(n)) {
                        return;
                    }
                    inputModel.getEquipmentsInNode().get(n).forEach(id -> {
                        PropertyBag line = inputModel.getLineParameters(id);
                        if (line != null) {
                            LOG.debug("line code {}", line.get("code"));
                        }
                        PropertyBag transformer = inputModel.getTransformerParameters(id);
                        if (transformer != null) {
                            LOG.debug("transformer code {}", transformer.get("code"));
                        }
                    });
                });
            });

            Map<String, Integer> sortedByCodeEquipmentsReport = new TreeMap<String, Integer>(
                    new Comparator<String>() {
                        @Override
                        public int compare(String s1, String s2) {
                            if (s1.length() == s2.length()) {
                                return s1.compareTo(s2);
                            }
                            return Integer.compare(s1.length(), s2.length());
                        }
                    });

            sortedByCodeEquipmentsReport.putAll(equipmentsReport);
            sortedByCodeEquipmentsReport.keySet().forEach(code -> {
                LOG.info("total {} code {} --- {}", sortedByCodeEquipmentsReport.get(code), code,
                        evaluateCode((String) code));
            });
        });
    }

    private Map<List<String>, PropertyBag> calcBalance(String config) {

        equipmentsReport = new HashMap<>();
        Map<List<String>, PropertyBag> report = new HashMap<>();
        List<String> pn = new ArrayList<>(Arrays.asList("balanceP", "balanceQ"));

        inputModel.getJoinedNodes().forEach(nodes -> {
            nodes.forEach(n -> {
                if (n == null) {
                    return;
                }
                PropertyBag node = inputModel.getNodeParameters(n);
                LOG.debug("------  equipment node ----------> {}", n);
                if (n.startsWith("_c4e78550") || n.startsWith("_59f72142")) {
                    LOG.info("node {}", node);
                }
                if (!inputModel.getEquipmentsInNode().containsKey(n)) {
                    double p = node.asDouble("p");
                    double q = node.asDouble("q");
                    double balanceP = node.asDouble("balanceP", 0.0);
                    double balanceQ = node.asDouble("balanceQ", 0.0);
                    node.put("balanceP", Double.toString(balanceP + p));
                    node.put("balanceQ", Double.toString(balanceQ + q));
                    return;
                }
                inputModel.getEquipmentsInNode().get(n).forEach(id -> {
                    PropertyBag line = inputModel.getLineParameters(id);
                    if (line != null) {
                        PropertyBag node1 = inputModel.getNodeParameters(line.get("terminal1"));
                        PropertyBag node2 = inputModel.getNodeParameters(line.get("terminal2"));
                        CalcFlow calcFlow = new CalcFlow(inputModel);
                        calcFlow.calcFlowLine(n, node1, node2, line, config);
                        registerCode(calcFlow.getModelCode());
                        double balanceP = node.asDouble("balanceP", 0.0);
                        double balanceQ = node.asDouble("balanceQ", 0.0);
                        boolean calculated = node.asBoolean("calculated", true);
                        node.put("balanceP", Double.toString(balanceP + calcFlow.getP()));
                        node.put("balanceQ", Double.toString(balanceQ + calcFlow.getQ()));
                        node.put("calculated", Boolean.toString(calculated && calcFlow.getCalculated()));
                        LOG.debug("Line {}  Line P {} Q {} balanceP {} balanceQ {}", line,
                                line.asDouble("p"), line.asDouble("q"),
                                node.asDouble("balanceP", 0.0),
                                node.asDouble("balanceQ", 0.0));
                    }
                    PropertyBag transformer = inputModel.getTransformerParameters(id);
                    if (transformer != null) {
                        PropertyBag node1 = inputModel
                                .getNodeParameters(transformer.get("terminal1"));
                        PropertyBag node2 = inputModel
                                .getNodeParameters(transformer.get("terminal2"));
                        PropertyBag node3 = inputModel
                                .getNodeParameters(transformer.get("terminal3"));
                        CalcFlow calcFlow = new CalcFlow(inputModel);
                        if (node3 == null) {
                            calcFlow.calcFlowT2x(n, node1, node2, transformer, config);
                        } else {
                            calcFlow.calcFlowT3x(n, node1, node2, node3, transformer, config);
                        }
                        registerCode(calcFlow.getModelCode());
                        double balanceP = node.asDouble("balanceP", 0.0);
                        double balanceQ = node.asDouble("balanceQ", 0.0);
                        boolean calculated = node.asBoolean("calculated", true);
                        node.put("balanceP", Double.toString(balanceP + calcFlow.getP()));
                        node.put("balanceQ", Double.toString(balanceQ + calcFlow.getQ()));
                        node.put("calculated", Boolean.toString(calculated && calcFlow.getCalculated()));
                        LOG.debug("Transformer {} P {} Q {} ", transformer,
                                transformer.asDouble("p"),
                                transformer.asDouble("q"));
                    }
                });
                double p = node.asDouble("p");
                double q = node.asDouble("q");
                double nodeBalanceP = node.asDouble("balanceP", 0.0);
                double nodeBalanceQ = node.asDouble("balanceQ", 0.0);
                node.put("balanceP", Double.toString(nodeBalanceP + p));
                node.put("balanceQ", Double.toString(nodeBalanceQ + q));

                LOG.debug("equipment {} ,  {}", n, inputModel.getEquipmentsInNode().get(n));
                LOG.debug("node {} P {} Q {} balanceP {} balanceQ {}", n, p, q,
                        inputModel.getNodeParameters(n).asDouble("balanceP"),
                        inputModel.getNodeParameters(n).asDouble("balanceQ"));
            });

            double nodeBalanceP = nodes.stream().map(n -> {
                PropertyBag node = inputModel.getNodeParameters(n);
                return node.asDouble("balanceP", 0.0);
            }).mapToDouble(Double::doubleValue).sum();
            double nodeBalanceQ = nodes.stream().map(n -> {
                PropertyBag node = inputModel.getNodeParameters(n);
                return node.asDouble("balanceQ", 0.0);
            }).mapToDouble(Double::doubleValue).sum();

            boolean calculatedNodes = nodes.stream().filter(n -> {
                PropertyBag node = inputModel.getNodeParameters(n);
                return !node.asBoolean("calculated", false);
            }).count() == 0;
            if (!calculatedNodes) {
                nodeBalanceP = 0.0;
                nodeBalanceQ = 0.0;
            }
            LOG.debug("nodes {} {} {}", nodes, nodeBalanceP, nodeBalanceQ);

            PropertyBag pb = new PropertyBag(pn);
            pb.put("balanceP", "" + nodeBalanceP);
            pb.put("balanceQ", "" + nodeBalanceQ);
            pb.put("calculated", Boolean.toString(calculatedNodes));
            report.put(nodes, pb);
        });

        // Reset node balance before calculate next configuration
        inputModel.getNodeParametersValues().forEach(node -> {
            node.put("balanceP", Double.toString(0.0));
            node.put("balanceQ", Double.toString(0.0));
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

        Map<List<String>, PropertyBag> sortedByBalanceReport = report.entrySet().stream()
                .sorted(byBalance.reversed()).collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (a, b) -> {
                        throw new AssertionError();
                    },
                    LinkedHashMap::new));

        return sortedByBalanceReport;
    }

    private void registerCode(String code) {
        Integer total = equipmentsReport.get(code);
        if (total == null) {
            total = new Integer(0);
        }
        equipmentsReport.put(code, total + 1);
    }

    private static int                  show = 5;
    private static PrepareModel         inputModel;
    private static Map<String, Integer> equipmentsReport;

    private static final Logger         LOG  = LoggerFactory
            .getLogger(CgmesFlowValidation.class);
}
