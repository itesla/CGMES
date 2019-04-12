/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping;
import com.powsybl.cgmes.model.interpretation.InterpretationResult;
import com.powsybl.cgmes.model.interpretation.InterpretedModel;
import com.powsybl.cgmes.model.interpretation.ModelInterpretation;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class TestKronAdmittance {

    public static final String  CIM_16_NAMESPACE  = "http://iec.ch/TC57/2013/CIM-schema-cim16#";
    private static final double BALANCE_TOLERANCE = 1.0;
    private static final double VBASE             = 400.0;
    private static final double SBASE             = 100.0;

    @BeforeClass
    public static void setUp() throws IOException {
        cgmes = new CgmesModelTripleStore(CIM_16_NAMESPACE, TripleStoreFactory.create());
    }

    private InterpretedModel model(boolean lineFrom2To5open, boolean lineFrom5To2open,
            boolean xfmr2From2To6open, boolean xfmr2From6To2open, boolean xfmr3open) {
        Map<String, PropertyBag> nodeParameters = nodeModel();
        Map<String, PropertyBag> lineParameters = lineModel(lineFrom2To5open, lineFrom5To2open);
        Map<String, PropertyBag> xfmrParameters = xfmrModel(xfmr2From2To6open, xfmr2From6To2open, xfmr3open);
        Map<String, List<String>> equipmentsInNode = equipmentsInNodeModel(lineParameters, xfmrParameters);

        List<List<String>> joinedNodes = new ArrayList<>();
        nodeParameters.keySet().forEach(k -> {
            List<String> joinNodes = new ArrayList<>();
            joinNodes.add(k);
            joinedNodes.add(joinNodes);
        });

        Map<List<String>, Boolean> isolatedNodes = new HashMap<>();
        joinedNodes.forEach(joinNodes -> {
            isolatedNodes.put(joinNodes, Boolean.valueOf("true"));
            for (String n : joinNodes) {
                if (equipmentsInNode.containsKey(n)) {
                    isolatedNodes.put(joinNodes, Boolean.valueOf("false"));
                    return;
                }
            }
        });

        InterpretedModel model = new InterpretedModel(cgmes);
        model.setNodeParameters(nodeParameters);
        model.setJoinedNodes(joinedNodes);
        model.setLineParameters(lineParameters);
        model.setTransformerParameters(xfmrParameters);
        model.setEquipmentsInNode(equipmentsInNode);
        model.setIsolatedNodes(isolatedNodes);

        return model;

    }

    private Map<String, PropertyBag> nodeModel() {
        Map<String, PropertyBag> nodeParameters = new HashMap<>();
        String id = "Slack";
        propertyNames = new ArrayList<>(Arrays.asList("v", "nominalV", "angle", "p", "q"));
        PropertyBag node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.0 * VBASE));
        node.put("angle", Double.toString(0.0));
        node.put("p", "-39.104635");
        node.put("q", "192.513642");
        node.put("nominalV", Double.toString(VBASE));

        id = "2";
        node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.00290856 * VBASE));
        node.put("angle", Double.toString(Math.toDegrees(-0.01056277)));
        node.put("p", "24.0");
        node.put("q", "50.0");
        node.put("nominalV", Double.toString(VBASE));

        id = "3";
        node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.02660368 * VBASE));
        node.put("angle", Double.toString(Math.toDegrees(-0.02365032)));
        node.put("p", "13.0");
        node.put("q", "5.0");
        node.put("nominalV", Double.toString(VBASE));

        id = "4";
        node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.02752795 * VBASE));
        node.put("angle", Double.toString(Math.toDegrees(-0.01809537)));
        node.put("p", "0.0");
        node.put("q", "0.0");
        node.put("nominalV", Double.toString(VBASE));

        id = "5";
        node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.02034573 * VBASE));
        node.put("angle", Double.toString(Math.toDegrees(-0.01258495)));
        node.put("p", "0.0");
        node.put("q", "0.0");
        node.put("nominalV", Double.toString(VBASE));

        id = "6";
        node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.02102662 * VBASE));
        node.put("angle", Double.toString(Math.toDegrees(-0.01088041)));
        node.put("p", "0.0");
        node.put("q", "0.0");
        node.put("nominalV", Double.toString(VBASE));

        return nodeParameters;
    }

    private Map<String, PropertyBag> lineModel(boolean lineFrom2To5open, boolean lineFrom5To2open) {

        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = SBASE / Math.pow(VBASE, 2);

        Map<String, PropertyBag> lineParameters = new HashMap<>();

        String id = "LineFromSlackTo2";
        PropertyBag line = lineParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        line.put("r", Double.toString(0.0050 / zpu));
        line.put("x", Double.toString(0.0025 / zpu));
        line.put("bch", Double.toString(0.004 * zpu));
        String nodeId1 = "Slack";
        boolean t1connected = true;
        line.put("terminal1", nodeId1);
        line.put("connected1", Boolean.toString(t1connected));
        String nodeId2 = "2";
        boolean t2connected = true;
        line.put("terminal2", nodeId2);
        line.put("connected2", Boolean.toString(t2connected));

        id = "LineFrom2To5";
        line = lineParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        line.put("r", Double.toString(0.0075 / zpu));
        line.put("x", Double.toString(0.064 / zpu));
        line.put("bch", Double.toString(0.53 * zpu));
        nodeId1 = "2";
        t1connected = true;
        line.put("terminal1", nodeId1);
        line.put("connected1", Boolean.toString(t1connected));
        nodeId2 = "5";
        t2connected = !lineFrom2To5open;
        line.put("terminal2", nodeId2);
        line.put("connected2", Boolean.toString(t2connected));

        id = "LineFrom5To2";
        line = lineParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        line.put("r", Double.toString(0.0075 / zpu));
        line.put("x", Double.toString(0.065 / zpu));
        line.put("bch", Double.toString(0.53 * zpu));
        nodeId1 = "5";
        t1connected = !lineFrom5To2open;
        line.put("terminal1", nodeId1);
        line.put("connected1", Boolean.toString(t1connected));
        nodeId2 = "2";
        t2connected = true;
        line.put("terminal2", nodeId2);
        line.put("connected2", Boolean.toString(t2connected));

        return lineParameters;
    }

    private Map<String, PropertyBag> xfmrModel(boolean xfmr2From2To6open, boolean xfmr2From6To2open,
            boolean xfmr3open) {
        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = SBASE / Math.pow(VBASE, 2);

        Map<String, PropertyBag> xfmrParameters = new HashMap<>();

        String id = "Xfmr2From2To6";
        PropertyBag xfmr2 = xfmrParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        xfmr2.put("r1", Double.toString(0.0016 / zpu));
        xfmr2.put("x1", Double.toString(0.091 / zpu));
        xfmr2.put("b1", Double.toString(0.39 * zpu));
        xfmr2.put("g1", Double.toString(0.0));
        xfmr2.put("ratedU1", Double.toString(VBASE));
        xfmr2.put("r2", Double.toString(0.0));
        xfmr2.put("x2", Double.toString(0.0));
        xfmr2.put("b2", Double.toString(0.0));
        xfmr2.put("g2", Double.toString(0.0));
        xfmr2.put("ratedU2", Double.toString(VBASE));
        String nodeId1 = "2";
        boolean t1connected = true;
        xfmr2.put("terminal1", nodeId1);
        xfmr2.put("connected1", Boolean.toString(t1connected));
        String nodeId2 = "6";
        boolean t2connected = !xfmr2From2To6open;
        xfmr2.put("terminal2", nodeId2);
        xfmr2.put("connected2", Boolean.toString(t2connected));

        id = "Xfmr2From6To2";
        xfmr2 = xfmrParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        xfmr2.put("r1", Double.toString(0.0016 / zpu));
        xfmr2.put("x1", Double.toString(0.091 / zpu));
        xfmr2.put("b1", Double.toString(0.39 * zpu));
        xfmr2.put("g1", Double.toString(0.0));
        xfmr2.put("ratedU1", Double.toString(VBASE));
        xfmr2.put("r2", Double.toString(0.0));
        xfmr2.put("x2", Double.toString(0.0));
        xfmr2.put("b2", Double.toString(0.0));
        xfmr2.put("g2", Double.toString(0.0));
        xfmr2.put("ratedU2", Double.toString(VBASE));
        nodeId1 = "6";
        t1connected = !xfmr2From2To6open;
        xfmr2.put("terminal1", nodeId1);
        xfmr2.put("connected1", Boolean.toString(t1connected));
        nodeId2 = "2";
        t2connected = true;
        xfmr2.put("terminal2", nodeId2);
        xfmr2.put("connected2", Boolean.toString(t2connected));

        id = "Xfmr3From2To3To4";
        PropertyBag xfmr3 = xfmrParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        xfmr3.put("r1", Double.toString(0.0025 / zpu));
        xfmr3.put("x1", Double.toString(0.050 / zpu));
        xfmr3.put("b1", Double.toString(0.25 * zpu));
        xfmr3.put("g1", Double.toString(0.0));
        xfmr3.put("ratedU1", Double.toString(VBASE));
        xfmr3.put("r2", Double.toString(0.0020 / zpu));
        xfmr3.put("x2", Double.toString(0.045 / zpu));
        xfmr3.put("b2", Double.toString(0.20 * zpu));
        xfmr3.put("g2", Double.toString(0.0));
        xfmr3.put("ratedU2", Double.toString(VBASE));
        xfmr3.put("r3", Double.toString(0.0015 / zpu));
        xfmr3.put("x3", Double.toString(0.040 / zpu));
        xfmr3.put("b3", Double.toString(0.15 * zpu));
        xfmr3.put("g3", Double.toString(0.0));
        xfmr3.put("ratedU3", Double.toString(VBASE));
        nodeId1 = "2";
        t1connected = true;
        xfmr3.put("terminal1", nodeId1);
        xfmr3.put("connected1", Boolean.toString(t1connected));
        nodeId2 = "3";
        t2connected = true;
        xfmr3.put("terminal2", nodeId2);
        xfmr3.put("connected2", Boolean.toString(t2connected));
        String nodeId3 = "4";
        boolean t3connected = !xfmr3open;
        xfmr3.put("terminal3", nodeId3);
        xfmr3.put("connected3", Boolean.toString(t3connected));

        return xfmrParameters;
    }

    private Map<String, List<String>> equipmentsInNodeModel(
            Map<String, PropertyBag> lineParameters, Map<String, PropertyBag> xfmrParameters) {
        Map<String, List<String>> equipmentsInNode = new HashMap<>();
        propertyNames = new ArrayList<>(
                Arrays.asList("r", "x", "bch", "terminal1", "terminal2", "connected1", "connected2"));

        lineParameters.keySet().forEach(id -> {
            PropertyBag line = lineParameters.get(id);
            String nodeId1 = line.get("terminal1");
            boolean t1connected = line.asBoolean("connected1", true);
            if (t1connected) {
                List<String> idLines = equipmentsInNode.computeIfAbsent(nodeId1, z -> new ArrayList<>());
                idLines.add(id);
            }
            String nodeId2 = line.get("terminal2");
            boolean t2connected = line.asBoolean("connected2", true);
            if (t2connected) {
                List<String> idLines = equipmentsInNode.computeIfAbsent(nodeId2, z -> new ArrayList<>());
                idLines.add(id);
            }
        });

        xfmrParameters.keySet().forEach(id -> {
            PropertyBag xfmr = xfmrParameters.get(id);
            String nodeId1 = xfmr.get("terminal1");
            boolean t1connected = xfmr.asBoolean("connected1", true);
            if (t1connected) {
                List<String> idXfmrs = equipmentsInNode.computeIfAbsent(nodeId1, z -> new ArrayList<>());
                idXfmrs.add(id);
            }
            String nodeId2 = xfmr.get("terminal2");
            boolean t2connected = xfmr.asBoolean("connected2", true);
            if (t2connected) {
                List<String> idXfmrs = equipmentsInNode.computeIfAbsent(nodeId2, z -> new ArrayList<>());
                idXfmrs.add(id);
            }
            if (xfmr.containsKey("terminal3")) {
                String nodeId3 = xfmr.get("terminal3");
                boolean t3connected = xfmr.asBoolean("connected3", true);
                if (t3connected) {
                    List<String> idXfmrs = equipmentsInNode.computeIfAbsent(nodeId3, z -> new ArrayList<>());
                    idXfmrs.add(id);
                }
            }
        });
        return equipmentsInNode;
    }

    private List<CgmesEquipmentModelMapping> interpretationConfiguration() {
        List<CgmesEquipmentModelMapping> configs = new ArrayList<>();
        CgmesEquipmentModelMapping config = new CgmesEquipmentModelMapping();
        config.setLineBshunt(CgmesEquipmentModelMapping.LineShuntMappingAlternative.SPLIT);
        config.setXfmr2YShunt(CgmesEquipmentModelMapping.Xfmr2ShuntMappingAlternative.SPLIT);
        config.setXfmr3YShunt(CgmesEquipmentModelMapping.Xfmr3ShuntMappingAlternative.SPLIT);
        configs.add(config);

        return configs;
    }

    @Test
    public void allConnectedTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(model(false, false, false, false, false));
        flowValidation.interpret(interpretationConfiguration());
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
    }

    @Test
    public void kronAntennaLineFromToTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(model(true, false, false, false, false));
        flowValidation.interpret(interpretationConfiguration());
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
        interpretation.validationDataForAllModelMapping.values().forEach(validationData -> {
            Assert.assertEquals(0, getBadNodes(validationData.getBalanceData()));
        });
    }

    @Test
    public void kronAntennaLineToFromTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(model(false, true, false, false, false));
        flowValidation.interpret(interpretationConfiguration());
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
        interpretation.validationDataForAllModelMapping.values().forEach(validationData -> {
            Assert.assertEquals(0, getBadNodes(validationData.getBalanceData()));
        });
    }

    @Test
    public void kronAntennaXfmr2FromToTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(model(false, false, true, false, false));
        flowValidation.interpret(interpretationConfiguration());
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
        interpretation.validationDataForAllModelMapping.values().forEach(validationData -> {
            Assert.assertEquals(0, getBadNodes(validationData.getBalanceData()));
        });
    }

    @Test
    public void kronAntennaXfmr2ToFromTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(model(false, false, false, true, false));
        flowValidation.interpret(interpretationConfiguration());
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
        interpretation.validationDataForAllModelMapping.values().forEach(validationData -> {
            Assert.assertEquals(0, getBadNodes(validationData.getBalanceData()));
        });
    }

    @Test
    public void kronAntennaXfmr3Test() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(model(false, false, false, false, true));
        flowValidation.interpret(interpretationConfiguration());
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
        interpretation.validationDataForAllModelMapping.values().forEach(validationData -> {
            Assert.assertEquals(0, getBadNodes(validationData.getBalanceData()));
        });
    }

    @Test
    public void kronAntennaAllDisconnectedTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(model(true, true, true, true, true));
        flowValidation.interpret(interpretationConfiguration());
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
        interpretation.validationDataForAllModelMapping.values().forEach(validationData -> {
            Assert.assertEquals(0, getBadNodes(validationData.getBalanceData()));
        });
    }

    private long getBadNodes(Map<List<String>, PropertyBag> balanceData) {
        long badNodes = balanceData.values().stream()
                .filter(pb -> pb.asBoolean("calculated", false) && !pb.asBoolean("badVoltage", false))
                .filter(pb -> (Math.abs(pb.asDouble("balanceP"))
                        + Math.abs(pb.asDouble("balanceQ"))) > BALANCE_TOLERANCE)
                .count();

        return badNodes;
    }

    private static CgmesModel   cgmes;
    private static List<String> propertyNames;
}
