package com.powsybl.cgmes.conversion.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping;
import com.powsybl.cgmes.model.interpretation.FlowCalculator;
import com.powsybl.cgmes.model.interpretation.InterpretedModel;
import com.powsybl.cgmes.model.interpretation.ModelInterpretation;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.triplestore.api.PropertyBag;

public final class InterpretationForConversionValidation {

    private InterpretationForConversionValidation() {
    }

    public static InterpretedModel getInterpretedModel(CgmesModel m) {
        try {
            InterpretedModel interpretedModel = new InterpretedModel(m);
            interpretedModel.loadModel();
            return interpretedModel;
        } catch (Exception x) {
            return null;
        }
    }

    // JAM_TODO Eliminar la necesidad de crear un ModelInterpretation para definir la lista de
    // configuraciones
    public static List<CgmesEquipmentModelMapping> getConfigList(CgmesModel m) {
        ModelInterpretation modelInterpretation = new ModelInterpretation(m);

        List<CgmesEquipmentModelMapping> configs = new ArrayList<>();
        modelInterpretation.addModelMappingConfigurations(configs);
        return configs;
    }

    public static FlowPQ danglingLineFlow(InterpretedModel interpretedModel, CgmesEquipmentModelMapping config,
            String id) {

        PropertyBag line = interpretedModel.getLineParameters(id);
        CgmesModel model = interpretedModel.getCgmes();
        for (PropertyBag node : model.boundaryNodes()) {
            if (node.getId("Node").equals(line.get("terminal1"))) {
                return lineFlow(interpretedModel, config, id, Branch.Side.TWO);
            } else if (node.getId("Node").equals(line.get("terminal2"))) {
                return lineFlow(interpretedModel, config, id, Branch.Side.ONE);
            }
        }
        return null;
    }

    public static FlowPQ lineFlow(InterpretedModel interpretedModel, CgmesEquipmentModelMapping config, String id,
            Branch.Side side) {

        FlowCalculator calcFlow = new FlowCalculator(interpretedModel);
        PropertyBag line = interpretedModel.getLineParameters(id);
        PropertyBag node1 = interpretedModel
                .getNodeParameters(line.get("terminal1"));
        Objects.requireNonNull(node1, "node1 null in line");
        PropertyBag node2 = interpretedModel
                .getNodeParameters(line.get("terminal2"));
        Objects.requireNonNull(node2, "node2 null in line");

        String n;
        if (side == Branch.Side.ONE) {
            n = line.get("terminal1");
        } else {
            n = line.get("terminal2");
        }
        calcFlow.forLine(n, node1, node2, line, config);

        FlowPQ flowPQ = new FlowPQ();
        flowPQ.p = calcFlow.getP();
        flowPQ.q = calcFlow.getQ();
        flowPQ.calculated = calcFlow.getCalculated();

        return flowPQ;
    }

    public static FlowPQ xfmr2Flow(InterpretedModel interpretedModel, CgmesEquipmentModelMapping config, String id,
            Branch.Side side) {

        FlowCalculator calcFlow = new FlowCalculator(interpretedModel);
        PropertyBag transformer = interpretedModel.getTransformerParameters(id);
        PropertyBag node1 = interpretedModel
                .getNodeParameters(transformer.get("terminal1"));
        Objects.requireNonNull(node1, "node1 null in transformer");
        PropertyBag node2 = interpretedModel
                .getNodeParameters(transformer.get("terminal2"));
        Objects.requireNonNull(node2, "node2 null in transformer");

        String n;
        if (side == Branch.Side.ONE) {
            n = transformer.get("terminal1");
        } else {
            n = transformer.get("terminal2");
        }
        calcFlow.forTwoWindingTransformer(n, node1, node2, transformer, config);

        FlowPQ flowPQ = new FlowPQ();
        flowPQ.p = calcFlow.getP();
        flowPQ.q = calcFlow.getQ();
        flowPQ.calculated = calcFlow.getCalculated();

        return flowPQ;
    }

    public static FlowPQ xfmr3Flow(InterpretedModel interpretedModel, CgmesEquipmentModelMapping config, String id,
            ThreeWindingsTransformer.Side side) {

        FlowCalculator calcFlow = new FlowCalculator(interpretedModel);
        PropertyBag transformer = interpretedModel.getTransformerParameters(id);
        PropertyBag node1 = interpretedModel
                .getNodeParameters(transformer.get("terminal1"));
        Objects.requireNonNull(node1, "node1 null in transformer");
        PropertyBag node2 = interpretedModel
                .getNodeParameters(transformer.get("terminal2"));
        Objects.requireNonNull(node2, "node2 null in transformer");
        PropertyBag node3 = interpretedModel
                .getNodeParameters(transformer.get("terminal3"));

        String n;
        if (side == ThreeWindingsTransformer.Side.ONE) {
            n = transformer.get("terminal1");
        } else if (side == ThreeWindingsTransformer.Side.TWO) {
            n = transformer.get("terminal2");
        } else {
            n = transformer.get("terminal3");
        }
        calcFlow.forThreeWindingTransformer(n, node1, node2, node3, transformer, config);

        FlowPQ flowPQ = new FlowPQ();
        flowPQ.p = calcFlow.getP();
        flowPQ.q = calcFlow.getQ();
        flowPQ.calculated = calcFlow.getCalculated();

        return flowPQ;
    }

    static class FlowPQ {
        double  p;
        double  q;
        boolean calculated;
    }
}
