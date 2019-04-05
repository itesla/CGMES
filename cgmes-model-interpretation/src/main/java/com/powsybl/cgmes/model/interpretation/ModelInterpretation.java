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
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr2PhaseAngleClockAlternative;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr2RatioPhaseMappingAlternative;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr2ShuntMappingAlternative;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr3PhaseAngleClockAlternative;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr3RatioPhaseMappingAlternative;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr3ShuntMappingAlternative;
import com.powsybl.cgmes.model.interpretation.InterpretationResult.ValidationData;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class ModelInterpretation {

    private static final double BALANCE_TOLERANCE = 1.0;

    public ModelInterpretation(CgmesModel m) {
        inputModel = new InterpretedModel(m);
        validationDataForAllModelMapping = new HashMap<>();
        bestError = Double.MAX_VALUE;
    }

    public void interpret() throws IOException {
        inputModel.loadModel();
        calculateBalancesForAllModelMapping();
    }

    public void setInputModel(InterpretedModel inputModel) {
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
        config.setXfmr2Ratio0(Xfmr2RatioPhaseMappingAlternative.X);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setXfmr2YShunt(Xfmr2ShuntMappingAlternative.SPLIT);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setXfmr2YShunt(Xfmr2ShuntMappingAlternative.SPLIT);
        config.setXfmr2Ratio0(Xfmr2RatioPhaseMappingAlternative.END1);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setXfmr2Ratio0(Xfmr2RatioPhaseMappingAlternative.END1);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setXfmr2Ratio0(Xfmr2RatioPhaseMappingAlternative.RTC);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setXfmr2YShunt(Xfmr2ShuntMappingAlternative.SPLIT);
        config.setXfmr3YShunt(Xfmr3ShuntMappingAlternative.SPLIT);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setXfmr2YShunt(Xfmr2ShuntMappingAlternative.SPLIT);
        config.setXfmr3YShunt(Xfmr3ShuntMappingAlternative.SPLIT);
        config.setLineRatio0(true);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setXfmr2PhaseAngleClock(Xfmr2PhaseAngleClockAlternative.END1_END2);
        config.setXfmr3PhaseAngleClock(Xfmr3PhaseAngleClockAlternative.STAR_BUS_SIDE);
        config.setXfmr2Pac2Negate(true);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setXfmr2YShunt(Xfmr2ShuntMappingAlternative.SPLIT);
        config.setXfmr3YShunt(Xfmr3ShuntMappingAlternative.SPLIT);
        config.setXfmr2PhaseAngleClock(Xfmr2PhaseAngleClockAlternative.END1_END2);
        config.setXfmr3PhaseAngleClock(Xfmr3PhaseAngleClockAlternative.STAR_BUS_SIDE);
        config.setXfmr2Pac2Negate(true);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setXfmr2YShunt(Xfmr2ShuntMappingAlternative.SPLIT);
        config.setXfmr3YShunt(Xfmr3ShuntMappingAlternative.SPLIT);
        config.setXfmr2PhaseAngleClock(Xfmr2PhaseAngleClockAlternative.END1_END2);
        config.setXfmr3PhaseAngleClock(Xfmr3PhaseAngleClockAlternative.STAR_BUS_SIDE);
        config.setXfmr2Pac2Negate(true);
        config.setLineRatio0(true);
        configs.add(config);

        config = new CgmesEquipmentModelMapping();
        config.setXfmr3Ratio0StarBusSide(Xfmr3RatioPhaseMappingAlternative.NETWORK_SIDE);
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
                Arrays.asList("balanceP", "balanceQ", "calculated", "line", "xfmr2", "xfmr3"));

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

        validationData.balanceData = balanceData.entrySet().stream().sorted(byBalance.reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));

    }

    private double calculateTotalBalanceError(ValidationData validationData) {
        double totalError = validationData.balanceData.values().stream()
                .filter(pb -> pb.asBoolean("calculated", false) && !pb.asBoolean("isolated", false))
                .map(pb -> Math.abs(pb.asDouble("balanceP")) + Math.abs(pb.asDouble("balanceQ")))
                .mapToDouble(Double::doubleValue).sum();
        return totalError;
    }

    private void calculateJoinedNodeBalance(CgmesEquipmentModelMapping config, List<String> nodes,
            PropertyBag nodeBalanceData, Map<String, DetectedEquipmentModel> nodeDetectedModelReport) {
        nodes.forEach(n -> {
            PropertyBag node = inputModel.getNodeParameters(n);
            Objects.requireNonNull(node, "node without parameters");
            if (inputModel.getEquipmentsInNode().containsKey(n)) {
                inputModel.getEquipmentsInNode().get(n).forEach(id -> {
                    boolean isLine = false;
                    boolean isXfmr2 = false;
                    boolean isXfmr3 = false;
                    FlowCalculator calcFlow = new FlowCalculator(inputModel);
                    PropertyBag line = inputModel.getLineParameters(id);
                    if (line != null) {
                        PropertyBag node1 = inputModel
                                .getNodeParameters(line.get("terminal1"));
                        Objects.requireNonNull(node1, "node1 null in line");
                        PropertyBag node2 = inputModel
                                .getNodeParameters(line.get("terminal2"));
                        Objects.requireNonNull(node2, "node2 null in line");
                        calcFlow.forLine(n, node1, node2, line, config);
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
                            calcFlow.forTwoWindingTransformer(n, node1, node2, transformer, config);
                            isXfmr2 = true;
                        } else {
                            calcFlow.forThreeWindingTransformer(n, node1, node2, node3, transformer, config);
                            isXfmr3 = true;
                        }
                    }

                    writeToNodeDetectedModelData(nodeDetectedModelReport, calcFlow.getEquipmentModel());
                    writeToNodeBalanceData(nodeBalanceData, calcFlow, isLine, isXfmr2, isXfmr3);
                });
            }

            addNodeInjectionToJoinedBusBalance(node, nodeBalanceData);
        });
    }

    private void addNodeInjectionToJoinedBusBalance(PropertyBag node, PropertyBag nodeBalanceData) {
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
        nodeBalanceReport.put("xfmr2", Integer.toString(0));
        nodeBalanceReport.put("xfmr3", Integer.toString(0));
        return nodeBalanceReport;
    }

    private void isolateNodeBalance(PropertyBag nodeBalanceData) {
        nodeBalanceData.put("isolated", "true");
        nodeBalanceData.put("calculated", "false");
        nodeBalanceData.put("badVoltage", "false");
    }

    private void writeToNodeBalanceData(PropertyBag nodeBalanceData, FlowCalculator calcFlow, boolean isLine,
            boolean isXfmr2, boolean isXfmr3) {
        if (calcFlow.getCalculated()) {
            double balanceP = nodeBalanceData.asDouble("balanceP");
            double balanceQ = nodeBalanceData.asDouble("balanceQ");
            nodeBalanceData.put("balanceP", Double.toString(balanceP + calcFlow.getP()));
            nodeBalanceData.put("balanceQ", Double.toString(balanceQ + calcFlow.getQ()));
            if (calcFlow.getBadVoltage()) {
                nodeBalanceData.put("badVoltage", Boolean.toString(calcFlow.getBadVoltage()));
            }
        } else {
            nodeBalanceData.put("calculated", Boolean.toString(calcFlow.getCalculated()));
        }
        if (isLine) {
            int lines = nodeBalanceData.asInt("line");
            nodeBalanceData.put("line", Integer.toString(lines + 1));
        }
        if (isXfmr2) {
            int xfmr2s = nodeBalanceData.asInt("xfmr2");
            nodeBalanceData.put("xfmr2", Integer.toString(xfmr2s + 1));
        }
        if (isXfmr3) {
            int xfmr3s = nodeBalanceData.asInt("xfmr3");
            nodeBalanceData.put("xfmr3", Integer.toString(xfmr3s + 1));
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

    private void writeToBalanceData(List<String> pn, Map<List<String>, PropertyBag> balanceData, List<String> nodes,
            PropertyBag nodeBalanceData) {
        boolean calculatedNodes = nodeBalanceData.asBoolean("calculated", true);
        boolean isolatedNodes = nodeBalanceData.asBoolean("isolated", true);
        boolean badVoltageNodes = nodeBalanceData.asBoolean("badVoltage", true);
        int nodeLines = nodeBalanceData.asInt("line");
        int nodeXfmr2s = nodeBalanceData.asInt("xfmr2");
        int nodeXfmr3s = nodeBalanceData.asInt("xfmr3");

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
        pb.put("xfmr2", Integer.toString(nodeXfmr2s));
        pb.put("xfmr3", Integer.toString(nodeXfmr3s));
        balanceData.put(nodes, pb);
    }

    private void writeToDetectedModelData(Map<String, DetectedEquipmentModel> detectedModelData,
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

    private InterpretedModel                                inputModel;
    private double                                          bestError;
    private Map<CgmesEquipmentModelMapping, ValidationData> validationDataForAllModelMapping;

    private static final Logger                             LOG = LoggerFactory.getLogger(ModelInterpretation.class);

}
