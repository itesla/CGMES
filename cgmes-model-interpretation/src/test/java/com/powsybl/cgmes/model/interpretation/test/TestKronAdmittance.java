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
import com.powsybl.cgmes.model.interpretation.InterpretationResult;
import com.powsybl.cgmes.model.interpretation.ModelInterpretation;
import com.powsybl.cgmes.model.interpretation.InterpretedModel;
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

    @BeforeClass
    public static void setUp() throws IOException {
        cgmes = new CgmesModelTripleStore(CIM_16_NAMESPACE, TripleStoreFactory.create());
    }

    private Map<String, PropertyBag> nodeModel(double vbase) {

        Map<String, PropertyBag> nodeParameters = new HashMap<>();
        String id = "Slack";
        propertyNames = new ArrayList<>(Arrays.asList("v", "nominalV", "angle", "p", "q"));
        PropertyBag node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.0 * vbase));
        node.put("angle", Double.toString(0.0));
        node.put("p", "-37.685531");
        node.put("q", "13.094454");
        node.put("nominalV", "400.0");

        id = "1";
        node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.00982854 * vbase));
        node.put("angle", Double.toString(Math.toDegrees(-0.03386869)));
        node.put("p", "24.0");
        node.put("q", "102.0");
        node.put("nominalV", "400.0");

        id = "2";
        node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.08423359 * vbase));
        node.put("angle", Double.toString(Math.toDegrees(-0.04845154)));
        node.put("p", "0.0");
        node.put("q", "0.0");
        node.put("nominalV", "400.0");

        id = "3";
        node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.06519040 * vbase));
        node.put("angle", Double.toString(Math.toDegrees(-0.04950536)));
        node.put("p", "13.0");
        node.put("q", "5.0");
        node.put("nominalV", "400.0");

        return nodeParameters;
    }

    private Map<String, PropertyBag> lineModel(double sbase, double vbase) {

        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = sbase / Math.pow(vbase, 2);

        Map<String, PropertyBag> lineParameters = new HashMap<>();

        String id = "LSlack1";
        PropertyBag line = lineParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        line.put("r", Double.toString(0.0052 / zpu));
        line.put("x", Double.toString(0.089 / zpu));
        line.put("bch", Double.toString(0.01 * zpu));
        String nodeId1 = "Slack";
        boolean t1connected = true;
        line.put("terminal1", nodeId1);
        line.put("connected1", Boolean.toString(t1connected));
        String nodeId2 = "1";
        boolean t2connected = true;
        line.put("terminal2", nodeId2);
        line.put("connected2", Boolean.toString(t2connected));

        return lineParameters;
    }

    private Map<String, List<String>> equipmentsInNodeModel(
            Map<String, PropertyBag> lineParameters) {
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
        return equipmentsInNode;
    }

    private InterpretedModel testLineModel(boolean lineT2Connected) {
        double sbase = 100.0;
        double vbase = 400.0;
        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = sbase / Math.pow(vbase, 2);

        Map<String, PropertyBag> nodeParameters = nodeModel(vbase);

        String id = "T";
        PropertyBag node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.06499392 * vbase));
        node.put("angle", Double.toString(Math.toDegrees(-0.04813391)));
        node.put("p", "0.0");
        node.put("q", "0.0");
        node.put("nominalV", "400.0");

        List<List<String>> joinedNodes = new ArrayList<>();
        nodeParameters.keySet().forEach(k -> {
            List<String> joinNodes = new ArrayList<>();
            joinNodes.add(k);
            joinedNodes.add(joinNodes);
        });

        Map<String, PropertyBag> lineParameters = lineModel(sbase, vbase);

        id = "1T";
        PropertyBag line = lineParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        line.put("r", Double.toString(0.0075 / zpu));
        line.put("x", Double.toString(0.064 / zpu));
        line.put("bch", Double.toString(0.53 * zpu));
        String nodeId1 = "1";
        boolean t1connected = true;
        line.put("terminal1", nodeId1);
        line.put("connected1", Boolean.toString(t1connected));
        String nodeId2 = "T";
        boolean t2connected = true;
        line.put("terminal2", nodeId2);
        line.put("connected2", Boolean.toString(t2connected));

        id = "T3";
        line = lineParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        line.put("r", Double.toString(0.0037 / zpu));
        line.put("x", Double.toString(0.01 / zpu));
        line.put("bch", Double.toString(0.21 * zpu));
        nodeId1 = "T";
        t1connected = true;
        line.put("terminal1", nodeId1);
        line.put("connected1", Boolean.toString(t1connected));
        nodeId2 = "3";
        t2connected = true;
        line.put("terminal2", nodeId2);
        line.put("connected2", Boolean.toString(t2connected));

        id = "T2";
        line = lineParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        line.put("r", Double.toString(0.0016 / zpu));
        line.put("x", Double.toString(0.091 / zpu));
        line.put("bch", Double.toString(0.39 * zpu));
        nodeId1 = "T";
        t1connected = true;
        line.put("terminal1", nodeId1);
        line.put("connected1", Boolean.toString(t1connected));
        nodeId2 = "2";
        t2connected = lineT2Connected;
        line.put("terminal2", nodeId2);
        line.put("connected2", Boolean.toString(t2connected));

        Map<String, List<String>> equipmentsInNode = equipmentsInNodeModel(lineParameters);

        Map<String, PropertyBag> transformerParameters = new HashMap<>();
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
        model.setTransformerParameters(transformerParameters);
        model.setEquipmentsInNode(equipmentsInNode);
        model.setIsolatedNodes(isolatedNodes);

        return model;
    }

    private InterpretedModel testXfmr2Model(boolean xfmr2T2Connected) {
        double sbase = 100.0;
        double vbase = 400.0;
        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = sbase / Math.pow(vbase, 2);

        Map<String, PropertyBag> nodeParameters = nodeModel(vbase);

        String id = "T";
        PropertyBag node = nodeParameters.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
        node.put("v", Double.toString(1.06499392 * vbase));
        node.put("angle", Double.toString(Math.toDegrees(-0.04813391)));
        node.put("p", "0.0");
        node.put("q", "0.0");
        node.put("nominalV", "400.0");

        List<List<String>> joinedNodes = new ArrayList<>();
        nodeParameters.keySet().forEach(k -> {
            List<String> joinNodes = new ArrayList<>();
            joinNodes.add(k);
            joinedNodes.add(joinNodes);
        });

        Map<String, PropertyBag> lineParameters = lineModel(sbase, vbase);

        id = "1T";
        PropertyBag line = lineParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        line.put("r", Double.toString(0.0075 / zpu));
        line.put("x", Double.toString(0.064 / zpu));
        line.put("bch", Double.toString(0.53 * zpu));
        String nodeId1 = "1";
        boolean t1connected = true;
        line.put("terminal1", nodeId1);
        line.put("connected1", Boolean.toString(t1connected));
        String nodeId2 = "T";
        boolean t2connected = true;
        line.put("terminal2", nodeId2);
        line.put("connected2", Boolean.toString(t2connected));

        id = "T3";
        line = lineParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        line.put("r", Double.toString(0.0037 / zpu));
        line.put("x", Double.toString(0.01 / zpu));
        line.put("bch", Double.toString(0.21 * zpu));
        nodeId1 = "T";
        t1connected = true;
        line.put("terminal1", nodeId1);
        line.put("connected1", Boolean.toString(t1connected));
        nodeId2 = "3";
        t2connected = true;
        line.put("terminal2", nodeId2);
        line.put("connected2", Boolean.toString(t2connected));

        Map<String, List<String>> equipmentsInNode = equipmentsInNodeModel(lineParameters);

        Map<String, PropertyBag> transformerParameters = new HashMap<>();

        id = "T2";
        PropertyBag xfmr2 = transformerParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        xfmr2.put("r1", Double.toString(0.0016 / zpu));
        xfmr2.put("x1", Double.toString(0.091 / zpu));
        xfmr2.put("b1", Double.toString(0.39 * zpu));
        xfmr2.put("g1", Double.toString(0.0));
        xfmr2.put("ratedU1", Double.toString(400.0));
        xfmr2.put("r2", Double.toString(0.0));
        xfmr2.put("x2", Double.toString(0.0));
        xfmr2.put("b2", Double.toString(0.0));
        xfmr2.put("g2", Double.toString(0.0));
        xfmr2.put("ratedU2", Double.toString(400.0));
        nodeId1 = "T";
        t1connected = true;
        xfmr2.put("terminal1", nodeId1);
        xfmr2.put("connected1", Boolean.toString(t1connected));
        if (t1connected) {
            List<String> idXfmr2 = equipmentsInNode.computeIfAbsent(nodeId1, z -> new ArrayList<>());
            idXfmr2.add(id);
        }
        nodeId2 = "2";
        t2connected = xfmr2T2Connected;
        xfmr2.put("terminal2", nodeId2);
        xfmr2.put("connected2", Boolean.toString(t2connected));
        if (t2connected) {
            List<String> idXfmr2 = equipmentsInNode.computeIfAbsent(nodeId2, z -> new ArrayList<>());
            idXfmr2.add(id);
        }

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
        model.setTransformerParameters(transformerParameters);
        model.setEquipmentsInNode(equipmentsInNode);
        model.setIsolatedNodes(isolatedNodes);

        return model;
    }

    private InterpretedModel testXfmr3Model(boolean xfmr3T2Connected) {
        double sbase = 100.0;
        double vbase = 400.0;
        // Impedances are expressed in per-unit values,
        // we convert to engineering units when adding elements to IIDM
        double zpu = sbase / Math.pow(vbase, 2);

        Map<String, PropertyBag> nodeParameters = nodeModel(vbase);
        List<List<String>> joinedNodes = new ArrayList<>();
        nodeParameters.keySet().forEach(k -> {
            List<String> joinNodes = new ArrayList<>();
            joinNodes.add(k);
            joinedNodes.add(joinNodes);
        });

        Map<String, PropertyBag> lineParameters = lineModel(sbase, vbase);
        Map<String, List<String>> equipmentsInNode = equipmentsInNodeModel(lineParameters);

        Map<String, PropertyBag> transformerParameters = new HashMap<>();

        String id = "123";
        PropertyBag xfmr3 = transformerParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        xfmr3.put("r1", Double.toString(0.0016 / zpu));
        xfmr3.put("x1", Double.toString(0.091 / zpu));
        xfmr3.put("b1", Double.toString(0.39 * zpu));
        xfmr3.put("g1", Double.toString(0.0));
        xfmr3.put("ratedU1", Double.toString(400.0));
        xfmr3.put("r2", Double.toString(0.0));
        xfmr3.put("x2", Double.toString(0.0));
        xfmr3.put("b2", Double.toString(0.0));
        xfmr3.put("g2", Double.toString(0.0));
        xfmr3.put("ratedU2", Double.toString(400.0));
        xfmr3.put("r3", Double.toString(0.0));
        xfmr3.put("x3", Double.toString(0.0));
        xfmr3.put("b3", Double.toString(0.0));
        xfmr3.put("g3", Double.toString(0.0));
        xfmr3.put("ratedU3", Double.toString(400.0));
        String nodeId1 = "1";
        boolean t1connected = true;
        xfmr3.put("terminal1", nodeId1);
        xfmr3.put("connected1", Boolean.toString(t1connected));
        if (t1connected) {
            List<String> idXfmr3 = equipmentsInNode.computeIfAbsent(nodeId1, z -> new ArrayList<>());
            idXfmr3.add(id);
        }
        String nodeId2 = "2";
        boolean t2connected = xfmr3T2Connected;
        xfmr3.put("terminal2", nodeId2);
        xfmr3.put("connected2", Boolean.toString(t2connected));
        if (t2connected) {
            List<String> idXfmr3 = equipmentsInNode.computeIfAbsent(nodeId2, z -> new ArrayList<>());
            idXfmr3.add(id);
        }
        String nodeId3 = "3";
        boolean t3connected = true;
        xfmr3.put("terminal3", nodeId3);
        xfmr3.put("connected3", Boolean.toString(t3connected));
        if (t3connected) {
            List<String> idXfmr3 = equipmentsInNode.computeIfAbsent(nodeId3, z -> new ArrayList<>());
            idXfmr3.add(id);
        }

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
        model.setTransformerParameters(transformerParameters);
        model.setEquipmentsInNode(equipmentsInNode);
        model.setIsolatedNodes(isolatedNodes);

        return model;
    }

    @Test
    public void fullConnectLineModelTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(testLineModel(true));
        flowValidation.interpret();
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
    }

    @Test
    public void kronAntennaLineModelTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(testLineModel(false));
        flowValidation.interpret();
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
        interpretation.validationDataForAllModelMapping.values().forEach(validationData -> {
            Assert.assertEquals(0, getBadNodes(validationData.getBalanceData()));
        });
    }

    @Test
    public void fullConnectXfmr2ModelTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(testXfmr2Model(true));
        flowValidation.interpret();
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
        interpretation.validationDataForAllModelMapping.values().forEach(validationData -> {
            Assert.assertEquals(0, getBadNodes(validationData.getBalanceData()));
        });
    }

    @Test
    public void kronAntennaXfmr2ModelTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(testXfmr2Model(false));
        flowValidation.interpret();
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
        interpretation.validationDataForAllModelMapping.values().forEach(validationData -> {
            Assert.assertEquals(0, getBadNodes(validationData.getBalanceData()));
        });
    }

    @Test
    public void fullConnectXfmr3ModelTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(testXfmr3Model(true));
        flowValidation.interpret();
        InterpretationResult interpretation = flowValidation.getInterpretation();
        Assert.assertTrue(interpretation.error < BALANCE_TOLERANCE);
        interpretation.validationDataForAllModelMapping.values().forEach(validationData -> {
            Assert.assertEquals(0, getBadNodes(validationData.getBalanceData()));
        });
    }

    @Test
    public void kronAntennaXfmr3ModelTest() throws IOException {
        ModelInterpretation flowValidation = new ModelInterpretation(cgmes);
        flowValidation.setInputModel(testXfmr3Model(false));
        flowValidation.interpret();
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
