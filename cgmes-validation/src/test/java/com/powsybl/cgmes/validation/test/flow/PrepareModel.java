package com.powsybl.cgmes.validation.test.flow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesNames;
import com.powsybl.cgmes.model.CgmesTerminal;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

public class PrepareModel {

    public PrepareModel(CgmesModel m) {
        cgmes = m;
    }

    public void loadModel() throws IOException {
        nodeParameters = nodeParameters(cgmes);
        equipmentsInNode = new HashMap<>();
        lineParameters = lineParameters(cgmes, equipmentsInNode);
        transformerParameters = transformerParameters(cgmes,
                equipmentsInNode);
        joinedNodes = joinRetainedSwitchesNodes(cgmes, nodeParameters);
        getNodeFlow(cgmes, nodeParameters);

        if (LOG.isDebugEnabled()) {
            nodeParameters.keySet().forEach(key -> {
                LOG.debug("node {} ,  {}", key, nodeParameters.get(key));
            });
            lineParameters.keySet().forEach(key -> {
                LOG.debug("line {} , {}", key, lineParameters.get(key));
            });
            transformerParameters.keySet().forEach(key -> {
                LOG.debug("transformer {} , {}", key, transformerParameters.get(key));
            });
        }
    }

    public CgmesModel getCgmes() {
        return cgmes;
    }

    public PropertyBag getNodeParameters(String n) {
        return nodeParameters.get(n);
    }

    public Collection<PropertyBag> getNodeParametersValues() {
        return nodeParameters.values();
    }

    public PropertyBag getLineParameters(String n) {
        return lineParameters.get(n);
    }

    public PropertyBag getTransformerParameters(String n) {
        return transformerParameters.get(n);
    }

    public List<List<String>> getJoinedNodes() {
        return joinedNodes;
    }

    public Map<List<String>, Boolean> getIsolatedNodes() {
        return isolatedNodes;
    }

    public Map<String, List<String>> getEquipmentsInNode() {
        return equipmentsInNode;
    }

    private List<List<String>> joinRetainedSwitchesNodes(CgmesModel cgmes,
            Map<String, PropertyBag> nodeParameters) {

        isolatedNodes = new HashMap<>();
        List<List<String>> joinedNodes = new ArrayList<>();
        Map<String, List<String>> nodes = new HashMap<>();

        String retainedSwitches = "SELECT * "
                + "WHERE { "
                + "{ GRAPH ?graph {"
                + "    ?Switch"
                + "        a ?type ;"
                + "        cim:IdentifiedObject.name ?name ;"
                + "        cim:Switch.retained ?retained ;"
                + "        cim:Equipment.EquipmentContainer ?EquipmentContainer ."
                + "    VALUES ?type { cim:Switch cim:Breaker cim:Disconnector } ."
                + "    ?Terminal1"
                + "        a cim:Terminal ;"
                + "        cim:Terminal.ConductingEquipment ?Switch ."
                + "    ?Terminal2"
                + "        a cim:Terminal ;"
                + "        cim:Terminal.ConductingEquipment ?Switch ."
                + "    FILTER ( STR(?Terminal1) < STR(?Terminal2) )"
                + "}} "
                + "OPTIONAL { GRAPH ?graphSSH {"
                + "    ?Switch cim:Switch.open ?open"
                + "}}"
                + "}";
        ((CgmesModelTripleStore) cgmes).query(retainedSwitches).forEach(rs -> {
            Boolean retained = rs.asBoolean("retained", false);
            Boolean open = rs.asBoolean("open", false);
            if (retained && !open) {
                CgmesTerminal t = cgmes.terminal(rs.getId(CgmesNames.TERMINAL + "1"));
                String id1 = t.topologicalNode();
                t = cgmes.terminal(rs.getId(CgmesNames.TERMINAL + "2"));
                String id2 = t.topologicalNode();
                List<String> node = nodes.computeIfAbsent(id1, x -> new ArrayList<>());
                node.add(id2);
                node = nodes.computeIfAbsent(id2, x -> new ArrayList<>());
                node.add(id1);
            }
        });

        List<String> visitedNodes = new ArrayList<>();
        nodeParameters.keySet().forEach(k -> {
            if (visitedNodes.contains(k)) {
                return;
            }
            List<String> joinNodes = new ArrayList<>();
            joinNodes.add(k);
            visitedNodes.add(k);
            if (nodes.containsKey(k)) {
                int indice = 0;
                while (indice < joinNodes.size()) {
                    String node = joinNodes.get(indice);
                    List<String> ns = nodes.get(node);
                    ns.forEach(nodeAd -> {
                        if (visitedNodes.contains(nodeAd)) {
                            return;
                        }
                        joinNodes.add(nodeAd);
                        visitedNodes.add(nodeAd);
                    });
                    indice++;
                }
            }
            joinedNodes.add(joinNodes);
        });

        joinedNodes.forEach(joinNodes -> {
            isolatedNodes.put(joinNodes, Boolean.valueOf("true"));
            for (String node : joinNodes) {
                if (equipmentsInNode.containsKey(node)) {
                    isolatedNodes.put(joinNodes, Boolean.valueOf("false"));
                    return;
                }
            }
        });

        return joinedNodes;
    }

    private Map<String, PropertyBag> nodeParameters(CgmesModel cgmes) {

        propertyNames = new ArrayList<>(Arrays.asList("v", "angle", "p", "q"));
        Map<String, PropertyBag> nodes = new HashMap<>();
        voltages = new HashMap<>();

        String svVoltage = "SELECT * "
                + "WHERE { "
                + "{ GRAPH ?graphSV {"
                + "    ?SvVoltage"
                + "        a cim:SvVoltage ;"
                + "        cim:SvVoltage.TopologicalNode ?TopologicalNode ;"
                + "        cim:SvVoltage.angle ?angle ;"
                + "        cim:SvVoltage.v ?v"
                + "}}"
                + "}";
        ((CgmesModelTripleStore) cgmes).query(svVoltage).forEach(v -> {
            String id = v.getId("TopologicalNode");
            voltages.put(id, v);
        });

        cgmes.topologicalNodes().forEach(n -> {

            String id = n.getId("TopologicalNode");
            String v = n.get("v");
            String angle = n.get("angle");
            String baseVoltageId = n.getId("BaseVoltage");
            double nominalV = cgmes.nominalVoltage(baseVoltageId);

            PropertyBag node = nodes.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
            node.put("v", v);
            node.put("angle", angle);
            node.put("p", "0.0");
            node.put("q", "0.0");
            node.put("nominalV", Double.toString(nominalV));
        });

        return nodes;
    }

    private void getNodeFlow(CgmesModel cgmes, Map<String, PropertyBag> nodes) {
        cgmes.energyConsumers().forEach(e -> terminalFlow(cgmes, nodes, e));
        cgmes.energySources().forEach(e -> terminalFlow(cgmes, nodes, e));
        cgmes.equivalentInjections().forEach(e -> terminalFlow(cgmes, nodes, e));
        cgmes.shuntCompensators().forEach(e -> equipmentFlow(cgmes, nodes, e));
        cgmes.staticVarCompensators().forEach(e -> terminalFlow(cgmes, nodes, e));
        cgmes.asynchronousMachines().forEach(e -> terminalFlow(cgmes, nodes, e));
        cgmes.synchronousMachines().forEach(e -> terminalFlow(cgmes, nodes, e));
        cgmes.externalNetworkInjections().forEach(e -> terminalFlow(cgmes, nodes, e));
    }

    private void terminalFlow(CgmesModel cgmes, Map<String, PropertyBag> nodes,
            PropertyBag equipment) {
        CgmesTerminal t = cgmes.terminal(equipment.getId(CgmesNames.TERMINAL));
        if (!t.connected()) {
            return;
        }
        String nodeId = t.topologicalNode();
        PropertyBag node = nodes.get(nodeId);
        if (node == null) {
            PropertyBag n = voltages.get(nodeId);
            String v = "0.0";
            String angle = "0.0";
            if (n != null) {
                v = n.get("v");
                angle = n.get("angle");
            }

            node = nodes.computeIfAbsent(nodeId, x -> new PropertyBag(propertyNames));
            node.put("v", v);
            node.put("angle", angle);
            node.put("p", "0.0");
            node.put("q", "0.0");
        }
        double pNode = node.asDouble("p");
        double qNode = node.asDouble("q");

        double pEquipment = t.flow().p();
        double qEquipment = t.flow().q();

        node.put("p", String.valueOf(pNode + pEquipment));
        node.put("q", String.valueOf(qNode + qEquipment));
    }

    private void equipmentFlow(CgmesModel cgmes, Map<String, PropertyBag> nodes,
            PropertyBag equipment) {
        CgmesTerminal t = cgmes.terminal(equipment.getId(CgmesNames.TERMINAL));
        if (!t.connected()) {
            return;
        }
        String nodeId = t.topologicalNode();
        PropertyBag node = nodes.get(nodeId);
        if (node == null) {
            PropertyBag n = voltages.get(nodeId);
            String v = n.get("v");
            String angle = n.get("angle");

            node = nodes.computeIfAbsent(nodeId, x -> new PropertyBag(propertyNames));
            node.put("v", v);
            node.put("angle", angle);
            node.put("p", "0.0");
            node.put("q", "0.0");
        }
        double v = node.asDouble("v");
        double bPerSection = equipment.asDouble(CgmesNames.B_PER_SECTION);
        double gPerSection = equipment.asDouble("gPerSection");
        int normalSections = equipment.asInt("normalSections", 0);
        double sections = equipment.asDouble("SVsections");
        if (Double.isNaN(sections)) {
            sections = equipment.asDouble("SSHsections", normalSections);
        }

        double pNode = node.asDouble("p");
        double qNode = node.asDouble("q");
        double pEquipment = gPerSection * sections * v * v;
        if (Double.isNaN(gPerSection)) {
            pEquipment = t.flow().p();
            if (Double.isNaN(pEquipment)) {
                pEquipment = 0.0;
            }
        }
        double qEquipment = -bPerSection * sections * v * v;
        if (Double.isNaN(bPerSection)) {
            qEquipment = t.flow().q();
            if (Double.isNaN(qEquipment)) {
                qEquipment = 0.0;
            }
        }
        node.put("p", String.valueOf(pNode + pEquipment));
        node.put("q", String.valueOf(qNode + qEquipment));
    }

    private Map<String, PropertyBag> lineParameters(CgmesModel cgmes,
            Map<String, List<String>> equipmentsInNode) {

        propertyNames = new ArrayList<>(Arrays.asList("r", "x", "bch"));
        Map<String, PropertyBag> lines = new HashMap<>();
        cgmes.acLineSegments().forEach(l -> {
            String id = l.getId(CgmesNames.AC_LINE_SEGMENT);
            String r = l.get("r");
            String x = l.get("x");
            String bch = l.get("bch");

            CgmesTerminal t1 = cgmes.terminal(l.getId(CgmesNames.TERMINAL + 1));
            CgmesTerminal t2 = cgmes.terminal(l.getId(CgmesNames.TERMINAL + 2));
            String nodeId1 = t1.topologicalNode();
            boolean t1connected = t1.connected();
            String nodeId2 = t2.topologicalNode();
            boolean t2connected = t2.connected();
            if (!t1connected && !t2connected) {
                return;
            }
            PropertyBag line = lines.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
            line.put("r", r);
            line.put("x", x);
            line.put("bch", bch);

            line.put("terminal1", nodeId1);
            if (t1connected) {
                List<String> idLines = equipmentsInNode.computeIfAbsent(nodeId1, z -> new ArrayList<>());
                idLines.add(id);
            }

            line.put("terminal2", nodeId2);
            line.put("connected", Boolean.toString(t1connected && t2connected));
            if (t2connected) {
                List<String> idLines = equipmentsInNode.computeIfAbsent(nodeId2, z -> new ArrayList<>());
                idLines.add(id);
            }
        });
        cgmes.equivalentBranches().forEach(eb -> {
            String id = eb.getId("EquivalentBranch");
            String r = eb.get("r");
            String x = eb.get("x");

            CgmesTerminal t1 = cgmes.terminal(eb.getId(CgmesNames.TERMINAL + 1));
            CgmesTerminal t2 = cgmes.terminal(eb.getId(CgmesNames.TERMINAL + 2));
            String nodeId1 = t1.topologicalNode();
            boolean t1connected = t1.connected();
            String nodeId2 = t2.topologicalNode();
            boolean t2connected = t2.connected();
            if (!t1connected && !t2connected) {
                return;
            }
            PropertyBag line = lines.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
            line.put("r", r);
            line.put("x", x);
            line.put("bch", "0.0");

            line.put("terminal1", nodeId1);
            if (t1connected) {
                List<String> idLines = equipmentsInNode.computeIfAbsent(nodeId1, z -> new ArrayList<>());
                idLines.add(id);
            }

            line.put("terminal2", nodeId2);
            line.put("connected", Boolean.toString(t1connected && t2connected));
            if (t1connected) {
                List<String> idLines = equipmentsInNode.computeIfAbsent(nodeId2, z -> new ArrayList<>());
                idLines.add(id);
            }
        });
        cgmes.seriesCompensators().forEach(sc -> {
            String id = sc.getId(CgmesNames.SERIES_COMPENSATOR);
            String r = sc.get("r");
            String x = sc.get("x");

            CgmesTerminal t1 = cgmes.terminal(sc.getId(CgmesNames.TERMINAL + 1));
            CgmesTerminal t2 = cgmes.terminal(sc.getId(CgmesNames.TERMINAL + 2));
            String nodeId1 = t1.topologicalNode();
            boolean t1connected = t1.connected();
            String nodeId2 = t2.topologicalNode();
            boolean t2connected = t2.connected();
            if (!t1connected && !t2connected) {
                return;
            }
            PropertyBag line = lines.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
            line.put("r", r);
            line.put("x", x);
            line.put("bch", "0.0");

            line.put("terminal1", nodeId1);
            if (t1connected) {
                List<String> idLines = equipmentsInNode.computeIfAbsent(nodeId1, z -> new ArrayList<>());
                idLines.add(id);
            }

            line.put("terminal2", nodeId2);
            line.put("connected", Boolean.toString(t1connected && t2connected));
            if (t2connected) {
                List<String> idLines = equipmentsInNode.computeIfAbsent(nodeId2, z -> new ArrayList<>());
                idLines.add(id);
            }
        });
        return lines;
    }

    private Map<String, PropertyBag> transformerParameters(CgmesModel cgmes,
            Map<String, List<String>> equipmentsInNode) {

        propertyNames = new ArrayList<>(Arrays.asList(
                "r1", "x1", "b1", "g1", "pac1", "ratedU1", "rns1", "rsvi1", "rstep1", "pns1",
                "psvi1", "pstep1",
                "r2", "x2", "b2", "g2", "pac2", "ratedU2", "rns2", "rsvi2", "rstep2", "pns2",
                "psvi2", "pstep2",
                "r3", "x3", "b3", "g3", "pac3", "ratedU3", "rns3", "rsvi3", "rstep3", "pns3",
                "psvi3", "pstep3"));

        Map<String, PropertyBag> powerTransformerRatioTapChanger = new HashMap<>();
        Map<String, PropertyBag> powerTransformerPhaseTapChanger = new HashMap<>();
        cgmes.ratioTapChangers().forEach(ratio -> {
            String id = ratio.getId("RatioTapChanger");
            powerTransformerRatioTapChanger.put(id, ratio);
        });
        cgmes.phaseTapChangers().forEach(phase -> {
            String id = phase.getId("PhaseTapChanger");
            powerTransformerPhaseTapChanger.put(id, phase);
        });

        Map<String, PropertyBag> transformers = new HashMap<>();
        cgmes.groupedTransformerEnds().entrySet().forEach(tends -> {
            String id = tends.getKey();
            PropertyBags ends = tends.getValue();
            boolean partialConnected = false;
            for (PropertyBag end : ends) {
                CgmesTerminal t = cgmes.terminal(end.getId(CgmesNames.TERMINAL));
                if (t.connected()) {
                    partialConnected = true;
                }
            }
            if (partialConnected) {
                ends.forEach(end -> transformerEndParameters(cgmes, equipmentsInNode,
                    powerTransformerRatioTapChanger, powerTransformerPhaseTapChanger, transformers,
                    id, end));
            }
        });
        return transformers;
    }

    private void transformerEndParameters(CgmesModel cgmes,
            Map<String, List<String>> equipmentsInNode,
            Map<String, PropertyBag> powerTransformerRatioTapChanger,
            Map<String, PropertyBag> powerTransformerPhaseTapChanger,
            Map<String, PropertyBag> transformers, String id, PropertyBag end) {
        String endNumber = end.get("endNumber");
        String r = end.get("r");
        String x = end.get("x");
        String b = end.get("b");
        String g = end.get("g");
        String phaseAngleClock = end.get("phaseAngleClock");
        String ratedU = end.get("ratedU");
        String rtc = end.getId("RatioTapChanger");
        PropertyBag rt = powerTransformerRatioTapChanger.get(rtc);
        String ptc = end.getId("PhaseTapChanger");
        PropertyBag pt = powerTransformerPhaseTapChanger.get(ptc);

        PropertyBag transformer = transformers.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
        if (r != null) {
            transformer.put("r" + endNumber, r);
        }
        if (x != null) {
            transformer.put("x" + endNumber, x);
        }
        if (b != null) {
            transformer.put("b" + endNumber, b);
        }
        if (g != null) {
            transformer.put("g" + endNumber, g);
        }
        if (phaseAngleClock != null) {
            transformer.put("pac" + endNumber, phaseAngleClock);
        }
        transformer.put("ratedU" + endNumber, ratedU);
        if (rt != null) {
            String tableId = rt.getId("RatioTapChangerTable");
            String ratioNeutralU = rt.get("neutralU");
            String ratioLowStep = rt.get("lowStep");
            String ratioHighStep = rt.get("highStep");
            String ratioNeutralStep = rt.get("neutralStep");
            String ratioStepVoltageIncrement = rt.get("stepVoltageIncrement");
            String ratioStep = rt.get("SVtapStep");
            if (tableId != null) {
                transformer.put("RatioTapChangerTable" + endNumber, tableId);
            }
            if (ratioNeutralU != null) {
                transformer.put("rnU" + endNumber, ratioNeutralU);
            }
            if (ratioLowStep != null) {
                transformer.put("rls" + endNumber, ratioLowStep);
            }
            if (ratioHighStep != null) {
                transformer.put("rhs" + endNumber, ratioHighStep);
            }
            if (ratioNeutralStep != null) {
                transformer.put("rns" + endNumber, ratioNeutralStep);
            }
            if (ratioStepVoltageIncrement != null) {
                transformer.put("rsvi" + endNumber, ratioStepVoltageIncrement);
            }
            if (ratioStep != null) {
                transformer.put("rstep" + endNumber, ratioStep);
            }
        }
        if (pt != null) {
            String tableId = pt.getId("PhaseTapChangerTable");
            String phaseNeutralU = pt.get("neutralU");
            String phaseLowStep = pt.get("lowStep");
            String phaseHighStep = pt.get("highStep");
            String phaseNeutralStep = pt.get("neutralStep");
            String phaseStepVoltageIncrement = pt.get("voltageStepIncrement");
            String stepPhaseShiftIncrement = pt.get("stepPhaseShiftIncrement");
            String phaseStep = pt.get("SVtapStep");
            String phaseWindingConnectionAngle = pt.get("windingConnectionAngle");
            String ptcType = pt.getLocal("phaseTapChangerType").toLowerCase();
            if (tableId != null) {
                transformer.put("PhaseTapChangerTable" + endNumber, tableId);
            }
            if (phaseNeutralU != null) {
                transformer.put("pnU" + endNumber, phaseNeutralU);
            }
            if (phaseLowStep != null) {
                transformer.put("pls" + endNumber, phaseLowStep);
            }
            if (phaseHighStep != null) {
                transformer.put("phs" + endNumber, phaseHighStep);
            }
            if (phaseNeutralStep != null) {
                transformer.put("pns" + endNumber, phaseNeutralStep);
            }
            if (phaseStepVoltageIncrement != null) {
                transformer.put("psvi" + endNumber, phaseStepVoltageIncrement);
            }
            if (stepPhaseShiftIncrement != null) {
                transformer.put("pspsi" + endNumber, stepPhaseShiftIncrement);
            }
            if (phaseStep != null) {
                transformer.put("pstep" + endNumber, phaseStep);
            }
            if (phaseWindingConnectionAngle != null) {
                transformer.put("pwca" + endNumber, phaseWindingConnectionAngle);
            }
            if (ptcType != null) {
                transformer.put("ptype" + endNumber, ptcType);
            }
        }

        CgmesTerminal t = cgmes.terminal(end.getId(CgmesNames.TERMINAL));
        String nodeId = t.topologicalNode();
        transformer.put("terminal" + endNumber, nodeId);
        transformer.put("connected" + endNumber, Boolean.toString(t.connected()));
        if (t.connected()) {
            List<String> idTransformers = equipmentsInNode.computeIfAbsent(nodeId, z -> new ArrayList<>());
            idTransformers.add(id);
        }
    }

    private CgmesModel                 cgmes;
    private List<String>               propertyNames;
    private Map<String, PropertyBag>   voltages;
    private List<List<String>>         joinedNodes;
    private Map<List<String>, Boolean> isolatedNodes;
    private Map<String, PropertyBag>   nodeParameters;
    private Map<String, PropertyBag>   lineParameters;
    private Map<String, PropertyBag>   transformerParameters;
    private Map<String, List<String>>  equipmentsInNode;

    private static final Logger        LOG = LoggerFactory
            .getLogger(PrepareModel.class);
}
