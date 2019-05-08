package com.powsybl.cgmes.conversion.validation;

import java.util.Map;

import com.powsybl.cgmes.conversion.validation.ConversionValidationResult.VerificationData;
import com.powsybl.cgmes.conversion.validation.FlowData.BranchEndType;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public final class ModelConversionValidation {

    private ModelConversionValidation() {
    }

    public static VerificationData validate(CgmesModelForInterpretation interpretedModel, InterpretationAlternative mappingConfig,
            Network network) {

        VerificationData verificationData = new VerificationData();
        Map<String, FlowData> flowDataMap = verificationData.getFlowData();

        // Flows validation
        network.getLineStream().forEach(line -> {
            EndData2 endData = lineFlow(interpretedModel,
                    mappingConfig, network, line);
            if (endData == null) {
                return;
            }

            addToFlowData(flowDataMap, endData.flowData1);
            addToFlowData(flowDataMap,
                    endData.flowData2);
        });

        network.getDanglingLines().forEach(line -> {
            EndData2 endData = danglingLineFlow(interpretedModel, mappingConfig, network, line);
            if (endData == null) {
                return;
            }

            addToFlowData(flowDataMap, endData.flowData1);
            addToFlowData(flowDataMap,
                    endData.flowData2);
        });

        network.getTwoWindingsTransformerStream().forEach(transformer -> {
            EndData2 endData = xfmr2Flow(interpretedModel, mappingConfig, network, transformer);
            if (endData == null) {
                return;
            }

            addToFlowData(flowDataMap, endData.flowData1);
            addToFlowData(flowDataMap, endData.flowData2);
        });

        network.getThreeWindingsTransformerStream().forEach(transformer -> {
            EndData3 endData = xfmr3Flow(interpretedModel, mappingConfig, network, transformer);
            if (endData == null) {
                return;
            }

            addToFlowData(flowDataMap, endData.flowData1);
            addToFlowData(flowDataMap,
                    endData.flowData2);
            addToFlowData(flowDataMap, endData.flowData3);
        });

        return verificationData;
    }

    private static EndData2 lineFlow(CgmesModelForInterpretation interpretedModel, InterpretationAlternative mappingConfig,
            Network network, Line line) {

        if (interpretedModel.getLine(line.getId()) == null) {
            return null;
        }

        Terminal terminal1 = line.getTerminal(Branch.Side.ONE);
        FlowData flowData1;
        if (terminal1.isConnected()) {
            InterpretationForConversionValidation.FlowPQ cgmesPQ = InterpretationForConversionValidation
                    .lineFlow(interpretedModel, mappingConfig, line.getId(), Branch.Side.ONE);
            if (cgmesPQ.calculated) {
                flowData1 = new FlowData(line.getId(), BranchEndType.LINE_ONE, terminal1.getP(), terminal1.getQ(),
                        cgmesPQ.p, cgmesPQ.q);
            } else {
                flowData1 = new FlowData(line.getId(), BranchEndType.LINE_ONE);
            }

        } else {
            flowData1 = new FlowData(line.getId(), BranchEndType.LINE_ONE);
        }

        Terminal terminal2 = line.getTerminal(Branch.Side.TWO);
        FlowData flowData2;
        if (terminal2.isConnected()) {
            InterpretationForConversionValidation.FlowPQ cgmesPQ = InterpretationForConversionValidation
                    .lineFlow(interpretedModel, mappingConfig, line.getId(), Branch.Side.TWO);
            if (cgmesPQ.calculated) {
                flowData2 = new FlowData(line.getId(), BranchEndType.LINE_TWO, terminal2.getP(), terminal2.getQ(),
                        cgmesPQ.p, cgmesPQ.q);
            } else {
                flowData2 = new FlowData(line.getId(), BranchEndType.LINE_TWO);
            }
        } else {
            flowData2 = new FlowData(line.getId(), BranchEndType.LINE_TWO);
        }

        EndData2 endData2 = new EndData2();
        endData2.flowData1 = flowData1;
        endData2.flowData2 = flowData2;
        return endData2;
    }

    private static EndData2 danglingLineFlow(CgmesModelForInterpretation interpretedModel,
            InterpretationAlternative mappingConfig, Network network, DanglingLine line) {

        if (interpretedModel.getLine(line.getId()) == null) {
            return null;
        }

        Terminal terminal = line.getTerminal();
        FlowData flowData1;
        if (terminal.isConnected()) {
            InterpretationForConversionValidation.FlowPQ cgmesPQ = InterpretationForConversionValidation
                    .danglingLineFlow(interpretedModel, mappingConfig, line.getId());
            if (cgmesPQ == null) {
                return null;
            }
            if (cgmesPQ.calculated) {
                flowData1 = new FlowData(line.getId(), BranchEndType.LINE_ONE, terminal.getP(), terminal.getQ(),
                        cgmesPQ.p, cgmesPQ.q);
            } else {
                flowData1 = new FlowData(line.getId(), BranchEndType.LINE_ONE);
            }
        } else {
            flowData1 = new FlowData(line.getId(), BranchEndType.LINE_ONE);
        }

        FlowData flowData2 = new FlowData(line.getId(), BranchEndType.LINE_TWO);

        EndData2 endData2 = new EndData2();
        endData2.flowData1 = flowData1;
        endData2.flowData2 = flowData2;
        return endData2;
    }

    private static EndData2 xfmr2Flow(CgmesModelForInterpretation interpretedModel, InterpretationAlternative mappingConfig,
            Network network, TwoWindingsTransformer transformer) {

        if (interpretedModel.getTransformer(transformer.getId()) == null) {
            return null;
        }

        Terminal terminal1 = transformer.getTerminal(Branch.Side.ONE);
        FlowData flowData1;
        if (terminal1.isConnected()) {
            InterpretationForConversionValidation.FlowPQ cgmesPQ = InterpretationForConversionValidation
                    .xfmr2Flow(interpretedModel, mappingConfig, transformer.getId(), Branch.Side.ONE);
            if (cgmesPQ.calculated) {
                flowData1 = new FlowData(transformer.getId(), BranchEndType.XFMR2_ONE, terminal1.getP(),
                        terminal1.getQ(),
                        cgmesPQ.p, cgmesPQ.q);
            } else {
                flowData1 = new FlowData(transformer.getId(), BranchEndType.XFMR2_ONE);
            }
        } else {
            flowData1 = new FlowData(transformer.getId(), BranchEndType.XFMR2_ONE);
        }

        Terminal terminal2 = transformer.getTerminal(Branch.Side.TWO);
        FlowData flowData2;
        if (terminal2.isConnected()) {
            InterpretationForConversionValidation.FlowPQ cgmesPQ = InterpretationForConversionValidation
                    .xfmr2Flow(interpretedModel, mappingConfig, transformer.getId(), Branch.Side.TWO);
            if (cgmesPQ.calculated) {
                flowData2 = new FlowData(transformer.getId(), BranchEndType.XFMR2_TWO, terminal2.getP(),
                        terminal2.getQ(),
                        cgmesPQ.p, cgmesPQ.q);
            } else {
                flowData2 = new FlowData(transformer.getId(), BranchEndType.XFMR2_TWO);
            }
        } else {
            flowData2 = new FlowData(transformer.getId(), BranchEndType.XFMR2_TWO);
        }

        EndData2 endData2 = new EndData2();
        endData2.flowData1 = flowData1;
        endData2.flowData2 = flowData2;
        return endData2;
    }

    private static EndData3 xfmr3Flow(CgmesModelForInterpretation interpretedModel, InterpretationAlternative mappingConfig,
            Network network, ThreeWindingsTransformer transformer) {

        if (interpretedModel.getTransformer(transformer.getId()) == null) {
            return null;
        }

        Terminal terminal1 = transformer.getTerminal(ThreeWindingsTransformer.Side.ONE);
        FlowData flowData1;
        if (terminal1.isConnected()) {
            InterpretationForConversionValidation.FlowPQ cgmesPQ = InterpretationForConversionValidation
                    .xfmr3Flow(interpretedModel, mappingConfig, transformer.getId(), ThreeWindingsTransformer.Side.ONE);
            if (cgmesPQ.calculated) {
                flowData1 = new FlowData(transformer.getId(), BranchEndType.XFMR3_ONE, terminal1.getP(),
                        terminal1.getQ(),
                        cgmesPQ.p, cgmesPQ.q);
            } else {
                flowData1 = new FlowData(transformer.getId(), BranchEndType.XFMR3_ONE);
            }

        } else {
            flowData1 = new FlowData(transformer.getId(), BranchEndType.XFMR3_ONE);
        }

        Terminal terminal2 = transformer.getTerminal(ThreeWindingsTransformer.Side.TWO);
        FlowData flowData2;
        if (terminal2.isConnected()) {
            InterpretationForConversionValidation.FlowPQ cgmesPQ = InterpretationForConversionValidation
                    .xfmr3Flow(interpretedModel, mappingConfig, transformer.getId(), ThreeWindingsTransformer.Side.TWO);
            if (cgmesPQ.calculated) {
                flowData2 = new FlowData(transformer.getId(), BranchEndType.XFMR3_TWO, terminal2.getP(),
                        terminal2.getQ(),
                        cgmesPQ.p, cgmesPQ.q);
            } else {
                flowData2 = new FlowData(transformer.getId(), BranchEndType.XFMR3_TWO);
            }
        } else {
            flowData2 = new FlowData(transformer.getId(), BranchEndType.XFMR3_TWO);
        }

        Terminal terminal3 = transformer.getTerminal(ThreeWindingsTransformer.Side.THREE);
        FlowData flowData3;
        if (terminal3.isConnected()) {
            InterpretationForConversionValidation.FlowPQ cgmesPQ = InterpretationForConversionValidation
                    .xfmr3Flow(interpretedModel, mappingConfig, transformer.getId(),
                            ThreeWindingsTransformer.Side.THREE);
            if (cgmesPQ.calculated) {
                flowData3 = new FlowData(transformer.getId(), BranchEndType.XFMR3_THREE, terminal3.getP(),
                        terminal3.getQ(),
                        cgmesPQ.p, cgmesPQ.q);
            } else {
                flowData3 = new FlowData(transformer.getId(), BranchEndType.XFMR3_THREE);
            }
        } else {
            flowData3 = new FlowData(transformer.getId(), BranchEndType.XFMR3_THREE);
        }

        EndData3 endData3 = new EndData3();
        endData3.flowData1 = flowData1;
        endData3.flowData2 = flowData2;
        endData3.flowData3 = flowData3;
        return endData3;
    }

    private static void addToFlowData(Map<String, FlowData> flowDataMap, FlowData flowData) {
        if (flowData == null) {
            return;
        }
        flowDataMap.put(flowData.code(), flowData);
    }

    static class EndData2 {
        FlowData flowData1;
        FlowData flowData2;
    }

    static class EndData3 {
        FlowData flowData1;
        FlowData flowData2;
        FlowData flowData3;
    }

}
