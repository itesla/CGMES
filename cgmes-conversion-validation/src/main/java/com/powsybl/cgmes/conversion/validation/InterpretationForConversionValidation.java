package com.powsybl.cgmes.conversion.validation;

import java.util.List;

import com.powsybl.cgmes.interpretation.InterpretationAlternatives;
import com.powsybl.cgmes.interpretation.InterpretedComputedFlow;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesLine;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesTransformer;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.triplestore.api.PropertyBag;

public final class InterpretationForConversionValidation {

    private InterpretationForConversionValidation() {
    }

    public static CgmesModelForInterpretation getCgmesModelForInterpretation(String name, CgmesModel m) {
        try {
            CgmesModelForInterpretation interpretedModel = new CgmesModelForInterpretation(name, m, true);
            return interpretedModel;
        } catch (Exception x) {
            return null;
        }
    }

    // JAM_TODO Eliminar la necesidad de crear un ModelInterpretation para definir la lista de
    // configuraciones
    public static List<InterpretationAlternative> getConfigList() {
        return InterpretationAlternatives.configured();
    }

    public static FlowPQ danglingLineFlow(CgmesModelForInterpretation interpretedModel, InterpretationAlternative mappingConfig,
            String id) {

        CgmesLine line = interpretedModel.getLine(id);
        CgmesModel model = interpretedModel.cgmes();
        for (PropertyBag node : model.boundaryNodes()) {
            if (node.getId("Node").equals(line.nodeId1())) {
                return lineFlow(interpretedModel, mappingConfig, id, Branch.Side.TWO);
            } else if (node.getId("Node").equals(line.nodeId2())) {
                return lineFlow(interpretedModel, mappingConfig, id, Branch.Side.ONE);
            }
        }
        return null;
    }

    public static FlowPQ lineFlow(CgmesModelForInterpretation interpretedModel, InterpretationAlternative mappingConfig, String id,
            Branch.Side side) {

        String n;
        CgmesLine line = interpretedModel.getLine(id);
        if (side == Branch.Side.ONE) {
            n = line.nodeId1();
        } else {
            n = line.nodeId2();
        }
        InterpretedComputedFlow calcFlow = InterpretedComputedFlow.forEquipment(id, n, mappingConfig, interpretedModel);

        FlowPQ flowPQ = new FlowPQ();
        flowPQ.p = calcFlow.p();
        flowPQ.q = calcFlow.q();
        flowPQ.calculated = calcFlow.isCalculated();

        return flowPQ;
    }

    public static FlowPQ xfmr2Flow(CgmesModelForInterpretation interpretedModel, InterpretationAlternative mappingConfig, String id,
            Branch.Side side) {

        String n;
        CgmesTransformer transformer = interpretedModel.getTransformer(id);
        if (side == Branch.Side.ONE) {
            n = transformer.end1().nodeId();
        } else {
            n = transformer.end2().nodeId();
        }
        InterpretedComputedFlow calcFlow = InterpretedComputedFlow.forEquipment(id, n, mappingConfig, interpretedModel);

        FlowPQ flowPQ = new FlowPQ();
        flowPQ.p = calcFlow.p();
        flowPQ.q = calcFlow.q();
        flowPQ.calculated = calcFlow.isCalculated();

        return flowPQ;
    }

    public static FlowPQ xfmr3Flow(CgmesModelForInterpretation interpretedModel, InterpretationAlternative mappingConfig, String id,
            ThreeWindingsTransformer.Side side) {

        String n;
        CgmesTransformer transformer = interpretedModel.getTransformer(id);
        if (side == ThreeWindingsTransformer.Side.ONE) {
            n = transformer.end1().nodeId();
        } else if (side == ThreeWindingsTransformer.Side.TWO) {
            n = transformer.end2().nodeId();
        } else {
            n = transformer.end3().nodeId();
        }
        InterpretedComputedFlow calcFlow = InterpretedComputedFlow.forEquipment(id, n, mappingConfig, interpretedModel);

        FlowPQ flowPQ = new FlowPQ();
        flowPQ.p = calcFlow.p();
        flowPQ.q = calcFlow.q();
        flowPQ.calculated = calcFlow.isCalculated();

        return flowPQ;
    }

    static class FlowPQ {
        double  p;
        double  q;
        boolean calculated;
    }
}
