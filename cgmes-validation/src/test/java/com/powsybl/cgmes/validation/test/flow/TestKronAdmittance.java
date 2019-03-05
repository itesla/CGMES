package com.powsybl.cgmes.validation.test.flow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.TripleStoreFactory;

public class TestKronAdmittance {

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
        propertyNames = new ArrayList<>(Arrays.asList("r", "x", "bch", "terminal1", "terminal2",
                "connected1", "connected2"));

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

    private PrepareModel testLineModel(boolean lineT2Connected) {
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

        PrepareModel model = new PrepareModel(cgmes);
        model.setNodeParameters(nodeParameters);
        model.setJoinedNodes(joinedNodes);
        model.setLineParameters(lineParameters);
        model.setTransformerParameters(transformerParameters);
        model.setEquipmentsInNode(equipmentsInNode);
        model.setIsolatedNodes(isolatedNodes);

        return model;
    }

    private PrepareModel testT2xModel(boolean t2xT2Connected) {
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
        PropertyBag t2x = transformerParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        t2x.put("r1", Double.toString(0.0016 / zpu));
        t2x.put("x1", Double.toString(0.091 / zpu));
        t2x.put("b1", Double.toString(0.39 * zpu));
        t2x.put("g1", Double.toString(0.0));
        t2x.put("ratedU1", Double.toString(400.0));
        t2x.put("r2", Double.toString(0.0));
        t2x.put("x2", Double.toString(0.0));
        t2x.put("b2", Double.toString(0.0));
        t2x.put("g2", Double.toString(0.0));
        t2x.put("ratedU2", Double.toString(400.0));
        nodeId1 = "T";
        t1connected = true;
        t2x.put("terminal1", nodeId1);
        t2x.put("connected1", Boolean.toString(t1connected));
        if (t1connected) {
            List<String> idT2x = equipmentsInNode.computeIfAbsent(nodeId1, z -> new ArrayList<>());
            idT2x.add(id);
        }
        nodeId2 = "2";
        t2connected = t2xT2Connected;
        t2x.put("terminal2", nodeId2);
        t2x.put("connected2", Boolean.toString(t2connected));
        if (t2connected) {
            List<String> idT2x = equipmentsInNode.computeIfAbsent(nodeId2, z -> new ArrayList<>());
            idT2x.add(id);
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

        PrepareModel model = new PrepareModel(cgmes);
        model.setNodeParameters(nodeParameters);
        model.setJoinedNodes(joinedNodes);
        model.setLineParameters(lineParameters);
        model.setTransformerParameters(transformerParameters);
        model.setEquipmentsInNode(equipmentsInNode);
        model.setIsolatedNodes(isolatedNodes);

        return model;
    }

    private PrepareModel testT3xModel(boolean t2xT2Connected) {
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
        PropertyBag t3x = transformerParameters.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        t3x.put("r1", Double.toString(0.0016 / zpu));
        t3x.put("x1", Double.toString(0.091 / zpu));
        t3x.put("b1", Double.toString(0.39 * zpu));
        t3x.put("g1", Double.toString(0.0));
        t3x.put("ratedU1", Double.toString(400.0));
        t3x.put("r2", Double.toString(0.0));
        t3x.put("x2", Double.toString(0.0));
        t3x.put("b2", Double.toString(0.0));
        t3x.put("g2", Double.toString(0.0));
        t3x.put("ratedU2", Double.toString(400.0));
        t3x.put("r3", Double.toString(0.0));
        t3x.put("x3", Double.toString(0.0));
        t3x.put("b3", Double.toString(0.0));
        t3x.put("g3", Double.toString(0.0));
        t3x.put("ratedU3", Double.toString(400.0));
        String nodeId1 = "1";
        boolean t1connected = true;
        t3x.put("terminal1", nodeId1);
        t3x.put("connected1", Boolean.toString(t1connected));
        if (t1connected) {
            List<String> idT3x = equipmentsInNode.computeIfAbsent(nodeId1, z -> new ArrayList<>());
            idT3x.add(id);
        }
        String nodeId2 = "2";
        boolean t2connected = t2xT2Connected;
        t3x.put("terminal2", nodeId2);
        t3x.put("connected2", Boolean.toString(t2connected));
        if (t2connected) {
            List<String> idT3x = equipmentsInNode.computeIfAbsent(nodeId2, z -> new ArrayList<>());
            idT3x.add(id);
        }
        String nodeId3 = "3";
        boolean t3connected = true;
        t3x.put("terminal3", nodeId3);
        t3x.put("connected3", Boolean.toString(t3connected));
        if (t3connected) {
            List<String> idT3x = equipmentsInNode.computeIfAbsent(nodeId3, z -> new ArrayList<>());
            idT3x.add(id);
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

        PrepareModel model = new PrepareModel(cgmes);
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
        FlowValidation flowValidation = new FlowValidation(cgmes);
        flowValidation.setInputModel(testLineModel(true));
        flowValidation.testBalances("FullConnectLineModel");
        String report = flowValidation.getReport();
        System.out.print(report);
    }

    @Test
    public void kronAntennaLineModelTest() throws IOException {
        FlowValidation flowValidation = new FlowValidation(cgmes);
        flowValidation.setInputModel(testLineModel(false));
        flowValidation.testBalances("KronAntennaLineModel");
        String report = flowValidation.getReport();
        System.out.print(report);
    }

    @Test
    public void fullConnectT2xModelTest() throws IOException {
        FlowValidation flowValidation = new FlowValidation(cgmes);
        flowValidation.setInputModel(testT2xModel(true));
        flowValidation.testBalances("FullConnectT2xModel");
        String report = flowValidation.getReport();
        System.out.print(report);
    }

    @Test
    public void kronAntennaT2xModelTest() throws IOException {
        FlowValidation flowValidation = new FlowValidation(cgmes);
        flowValidation.setInputModel(testT2xModel(false));
        flowValidation.testBalances("KronAntennaT2xModel");
        String report = flowValidation.getReport();
        System.out.print(report);
    }

    @Test
    public void fullConnectT3xModelTest() throws IOException {
        FlowValidation flowValidation = new FlowValidation(cgmes);
        flowValidation.setInputModel(testT3xModel(true));
        flowValidation.testBalances("FullConnectT3xModel");
        String report = flowValidation.getReport();
        System.out.print(report);
    }

    @Test
    public void kronAntennaT3xModelTest() throws IOException {
        FlowValidation flowValidation = new FlowValidation(cgmes);
        flowValidation.setInputModel(testT3xModel(false));
        flowValidation.testBalances("KronAntennaT3xModel");
        String report = flowValidation.getReport();
        System.out.print(report);
    }

    public static final String  CIM_16_NAMESPACE = "http://iec.ch/TC57/2013/CIM-schema-cim16#";
    private static CgmesModel   cgmes;
    private static List<String> propertyNames;
}
