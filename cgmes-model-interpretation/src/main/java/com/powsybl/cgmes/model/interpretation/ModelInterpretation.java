/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.EndDistribution;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.T3xDistribution;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.T3xPhaseAngleClock;
import com.powsybl.cgmes.model.interpretation.InterpretationResult.ValidationData;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author José Antonio Marqués <marquesja at aia.es>, Marcos de Miguel <demiguelm at aia.es>
 */
public class ModelInterpretation {

    private static final double BALANCE_TOLERANCE = 1.0;

    public ModelInterpretation(CgmesModel m) {
        inputModel = new PrepareModel(m);
        validationDataForAllModelMapping = new HashMap<>();
        bestError = Double.MAX_VALUE;
    }

    public void interpret() throws IOException {
        inputModel.loadModel();
        calculateBalancesForAllModelMapping();
    }

    public void setInputModel(PrepareModel inputModel) {
        this.inputModel = inputModel;
    }

    public InterpretationResult getInterpretation() {
        InterpretationResult r = new InterpretationResult();
        r.error = bestError;
        r.validationDataForAllModelMapping = validationDataForAllModelMapping;
        return r;
    }

    private void addModelMappingConfigurations(List<CgmesEquipmentModelMapping> configs) {
        CgmesEquipmentModelMapping config = new CgmesEquipmentModelMapping();
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setLineRatio0(true);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setT2xRatio0(EndDistribution.X);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setT2xYShunt(EndDistribution.SPLIT);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setT2xYShunt(EndDistribution.SPLIT);
        config.setT2xRatio0(EndDistribution.END1);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setT2xRatio0(EndDistribution.END1);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setT2xRatio0(EndDistribution.RTC);
        config.setT2xPtc2Negate(true);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setT2xYShunt(EndDistribution.SPLIT);
        config.setT3xYShunt(T3xDistribution.SPLIT);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setT2xYShunt(EndDistribution.SPLIT);
        config.setT3xYShunt(T3xDistribution.SPLIT);
        config.setLineRatio0(true);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setT2xPhaseAngleClock(true);
        config.setT3xPhaseAngleClock(T3xPhaseAngleClock.INSIDE);
        config.setT2xPac2Negate(true);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setT2xYShunt(EndDistribution.SPLIT);
        config.setT3xYShunt(T3xDistribution.SPLIT);
        config.setT2xPhaseAngleClock(true);
        config.setT3xPhaseAngleClock(T3xPhaseAngleClock.INSIDE);
        config.setT2xPac2Negate(true);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setT2xYShunt(EndDistribution.SPLIT);
        config.setT3xYShunt(T3xDistribution.SPLIT);
        config.setT2xPhaseAngleClock(true);
        config.setT3xPhaseAngleClock(T3xPhaseAngleClock.INSIDE);
        config.setT2xPac2Negate(true);
        config.setLineRatio0(true);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setT3xRatio0Inside(false);
        configs.add(config);
    }

    private void calculateBalancesForAllModelMapping() {

        List<CgmesEquipmentModelMapping> configs = new ArrayList<>();
        addModelMappingConfigurations(configs);

        configs.forEach(config -> {
            ValidationData validationData = calculateBalance(config);
            if (validationData.balance < bestError) {
                bestError = validationData.balance;
            }
            validationDataForAllModelMapping.put(config, validationData);
        });
    }

    private ValidationData calculateBalance(CgmesEquipmentModelMapping config) {

        ValidationData validationData = new ValidationData();
        Map<List<String>, PropertyBag> balanceData = new HashMap<>();
        List<String> pn = new ArrayList<>(
                Arrays.asList("balanceP", "balanceQ", "calculated", "line", "t2x", "t3x"));

        inputModel.getJoinedNodes().forEach(nodes -> {
            Map<String, DetectedEquipmentModel> nodeModelData = new HashMap<>();
            PropertyBag nodeBalanceData = initNodeBalanceData(pn);
            Boolean isIsolatedNodes = inputModel.getIsolatedNodes().get(nodes);
            if (isIsolatedNodes != null && isIsolatedNodes) {
                isolateNodeBalance(nodeBalanceData);
            } else {
                calculateJoinedNodeBalance(config, nodes, nodeBalanceData, nodeModelData);
            }
            writeToDetectedModelData(validationData.detectedModelData, nodeModelData, nodeBalanceData);
            writeToBalanceData(pn, balanceData, nodes, nodeBalanceData);
        });

        sortValidationData(balanceData, validationData);
        validationData.balance = calculateTotalBalanceError(validationData);
        return validationData;
    }

    private void sortValidationData(Map<List<String>, PropertyBag> balanceData, ValidationData validationData) {
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

    }

    private double calculateTotalBalanceError(ValidationData validationData) {
        double totalError = validationData.balanceData.values().stream().filter(pb -> {
            return pb.asBoolean("calculated", false) && !pb.asBoolean("isolated", false);
        }).map(pb -> {
            return Math.abs(pb.asDouble("balanceP")) + Math.abs(pb.asDouble("balanceQ"));
        }).mapToDouble(Double::doubleValue).sum();
        return totalError;
    }

    private void calculateJoinedNodeBalance(CgmesEquipmentModelMapping config, List<String> nodes,
            PropertyBag nodeBalanceData, Map<String, DetectedEquipmentModel> nodeDetectedModelReport) {
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
                    CalculateFlow calcFlow = new CalculateFlow(inputModel);
                    PropertyBag line = inputModel.getLineParameters(id);
                    if (line != null) {
                        PropertyBag node1 = inputModel
                                .getNodeParameters(line.get("terminal1"));
                        Objects.requireNonNull(node1, "node1 null in line");
                        PropertyBag node2 = inputModel
                                .getNodeParameters(line.get("terminal2"));
                        Objects.requireNonNull(node2, "node2 null in line");
                        calcFlow.calculateFlowLine(n, node1, node2, line, config);
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
                            calcFlow.calculateFlowT2x(n, node1, node2, transformer, config);
                            isT2x = true;
                        } else {
                            calcFlow.calculateFlowT3x(n, node1, node2, node3, transformer, config);
                            isT3x = true;
                        }
                    }

                    writeToNodeDetectedModelData(nodeDetectedModelReport, calcFlow.getEquipmentModel());
                    writeToNodeBalanceData(nodeBalanceData, calcFlow, isLine, isT2x, isT3x);
                });
            }

            addNodeInjectionToJoinedBusBalance(node, nodeBalanceData);
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

    private void writeToNodeBalanceData(PropertyBag nodeBalanceData, CalculateFlow calcFlow,
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

    private void writeToNodeDetectedModelData(Map<String, DetectedEquipmentModel> detectedModelData,
            DetectedEquipmentModel detectedEquipmentModel) {
        if (detectedEquipmentModel == null) {
            return;
        }
        DetectedEquipmentModel aggregateModel = detectedModelData.get(detectedEquipmentModel.code());
        if (aggregateModel == null) {
            aggregateModel = new DetectedEquipmentModel(detectedEquipmentModel.detectedBranchModels);
        }
        aggregateModel.total += 1;
        detectedModelData.put(detectedEquipmentModel.code(), aggregateModel);
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

    private void writeToDetectedModelData(
            Map<String, DetectedEquipmentModel> detectedModelData,
            Map<String, DetectedEquipmentModel> nodeDetectedModelData, PropertyBag nodeBalanceData) {

        nodeDetectedModelData.keySet().forEach(detectedEquipmentModel -> {
            DetectedEquipmentModel nodeModel = nodeDetectedModelData.get(detectedEquipmentModel);
            DetectedEquipmentModel aggregateModel = detectedModelData.get(detectedEquipmentModel);
            if (aggregateModel == null) {
                aggregateModel = new DetectedEquipmentModel(nodeModel.detectedBranchModels);
            }
            double nodeBalanceP = nodeBalanceData.asDouble("balanceP");
            double nodeBalanceQ = nodeBalanceData.asDouble("balanceQ");
            boolean calculatedNodes = nodeBalanceData.asBoolean("calculated", true);
            boolean isolatedNodes = nodeBalanceData.asBoolean("isolated", false);
            if (calculatedNodes && !isolatedNodes) {
                if (Math.abs(nodeBalanceP) + Math.abs(nodeBalanceQ) <= BALANCE_TOLERANCE) {
                    aggregateModel.total += nodeModel.total;
                    aggregateModel.calculated += nodeModel.total;
                    aggregateModel.ok += nodeModel.total;
                } else {
                    aggregateModel.total += nodeModel.total;
                    aggregateModel.calculated += nodeModel.total;
                }
            } else {
                aggregateModel.total += nodeModel.total;
            }
            detectedModelData.put(detectedEquipmentModel, aggregateModel);
        });
    }

    private PrepareModel                                    inputModel;
    private double                                          bestError;
    private Map<CgmesEquipmentModelMapping, ValidationData> validationDataForAllModelMapping;

    private static final Logger                             LOG = LoggerFactory.getLogger(ModelInterpretation.class);

}
