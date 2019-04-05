/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class FlowCalculator {

    public FlowCalculator(InterpretedModel inputModel) {
        this.inputModel = inputModel;
        this.p = 0.0;
        this.q = 0.0;
        this.calculated = false;
        this.badVoltage = false;
    }

    public void forLine(String n, PropertyBag node1, PropertyBag node2, PropertyBag line,
            CgmesEquipmentModelMapping config) {
        double v1 = node1.asDouble("v");
        double nominalV1 = node1.asDouble("nominalV");
        double angleDegrees1 = node1.asDouble("angle");
        double v2 = node2.asDouble("v");
        double nominalV2 = node2.asDouble("nominalV");
        double angleDegrees2 = node2.asDouble("angle");
        Boolean connected1 = line.asBoolean("connected1", false);
        Boolean connected2 = line.asBoolean("connected2", false);

        // The admittance and model code can always be calculated
        LineModel lineModel = new LineModel(line, config);
        lineModel.interpret(nominalV1, nominalV2);
        equipmentModel = new DetectedEquipmentModel(lineModel.getBranchModel());

        String nEnd1 = line.get("terminal1");
        String nEnd2 = line.get("terminal2");
        if (connected1 && connected2) {
            calculateBothEndsFlow(n, nEnd1, nEnd2, v1, angleDegrees1, v2, angleDegrees2,
                    lineModel.getAdmittanceMatrix());
        } else if (connected1) {
            calculateEndFromFlow(n, nEnd1, v1, angleDegrees1, lineModel.getAdmittanceMatrix());
        } else if (connected2) {
            calculateEndToFlow(n, nEnd2, v2, angleDegrees2, lineModel.getAdmittanceMatrix());
        }
    }

    public void forTwoWindingTransformer(String n, PropertyBag node1, PropertyBag node2, PropertyBag transformer,
            CgmesEquipmentModelMapping config) {
        double v1 = node1.asDouble("v");
        double angleDegrees1 = node1.asDouble("angle");
        double v2 = node2.asDouble("v");
        double angleDegrees2 = node2.asDouble("angle");
        Boolean connected1 = transformer.asBoolean("connected1", false);
        Boolean connected2 = transformer.asBoolean("connected2", false);

        // The admittance and the model code can always be calculated
        Xfmr2Model xfmr2Model = new Xfmr2Model(inputModel.getCgmes(), transformer, config);
        xfmr2Model.interpret();
        equipmentModel = new DetectedEquipmentModel(xfmr2Model.getBranchModel());

        String nEnd1 = transformer.get("terminal1");
        String nEnd2 = transformer.get("terminal2");
        if (connected1 && connected2) {
            calculateBothEndsFlow(n, nEnd1, nEnd2, v1, angleDegrees1, v2, angleDegrees2,
                    xfmr2Model.getAdmittanceMatrix());
        } else if (connected1) {
            calculateEndFromFlow(n, nEnd1, v1, angleDegrees1, xfmr2Model.getAdmittanceMatrix());
        } else if (connected2) {
            calculateEndToFlow(n, nEnd2, v2, angleDegrees2, xfmr2Model.getAdmittanceMatrix());
        }
    }

    public void forThreeWindingTransformer(String n, PropertyBag node1, PropertyBag node2, PropertyBag node3,
            PropertyBag transformer, CgmesEquipmentModelMapping config) {
        double r1 = transformer.asDouble("r1");
        double x1 = transformer.asDouble("x1");
        double r2 = transformer.asDouble("r2");
        double x2 = transformer.asDouble("x2");
        double r3 = transformer.asDouble("r3");
        double x3 = transformer.asDouble("x3");
        double v1 = node1.asDouble("v");
        double angleDegrees1 = node1.asDouble("angle");
        double v2 = node2.asDouble("v");
        double angleDegrees2 = node2.asDouble("angle");
        double v3 = node3.asDouble("v");
        double angleDegrees3 = node3.asDouble("angle");
        Boolean connected1 = transformer.asBoolean("connected1", false);
        Boolean connected2 = transformer.asBoolean("connected2", false);
        Boolean connected3 = transformer.asBoolean("connected3", false);

        // The admittance and model code can always be calculated
        Xfmr3Model xfmr3Model = new Xfmr3Model(inputModel.getCgmes(), transformer, config);
        xfmr3Model.interpret();
        equipmentModel = new DetectedEquipmentModel(xfmr3Model.getBranchModelEnd1(), xfmr3Model.getBranchModelEnd2(),
                xfmr3Model.getBranchModelEnd3());

        if (!calculateFlowXfmr3IsOk(r1, x1, r2, x2, r3, x3)) {
            return;
        }

        String nEnd1 = transformer.get("terminal1");
        String nEnd2 = transformer.get("terminal2");
        String nEnd3 = transformer.get("terminal3");
        if (connected1 && connected2 && connected3) {
            calculateThreeConnectedEndsFlow(n, nEnd1, nEnd2, nEnd3, v1, angleDegrees1, v2, angleDegrees2, v3,
                    angleDegrees3,
                    xfmr3Model.getAdmittanceMatrixEnd1(), xfmr3Model.getAdmittanceMatrixEnd2(),
                    xfmr3Model.getAdmittanceMatrixEnd3());
        } else if (connected1 && connected2) {
            BranchAdmittanceMatrix admittanceMatrixEnd1 = xfmr3Model.getAdmittanceMatrixEnd1();
            BranchAdmittanceMatrix admittanceMatrixEnd2 = xfmr3Model.getAdmittanceMatrixEnd2();
            BranchAdmittanceMatrix admittanceMatrixOpenEnd = xfmr3Model.getAdmittanceMatrixEnd3();
            calculateTwoConnectedEndsFlow(n, nEnd1, nEnd2, v1, angleDegrees1, v2, angleDegrees2,
                    admittanceMatrixEnd1, admittanceMatrixEnd2, admittanceMatrixOpenEnd);
        } else if (connected1 && connected3) {
            BranchAdmittanceMatrix admittanceMatrixEnd1 = xfmr3Model.getAdmittanceMatrixEnd1();
            BranchAdmittanceMatrix admittanceMatrixEnd3 = xfmr3Model.getAdmittanceMatrixEnd3();
            BranchAdmittanceMatrix admittanceMatrixOpenEnd = xfmr3Model.getAdmittanceMatrixEnd2();
            calculateTwoConnectedEndsFlow(n, nEnd1, nEnd3, v1, angleDegrees1, v3, angleDegrees3,
                    admittanceMatrixEnd1, admittanceMatrixEnd3, admittanceMatrixOpenEnd);
        } else if (connected2 && connected3) {
            BranchAdmittanceMatrix admittanceMatrixEnd2 = xfmr3Model.getAdmittanceMatrixEnd2();
            BranchAdmittanceMatrix admittanceMatrixEnd3 = xfmr3Model.getAdmittanceMatrixEnd3();
            BranchAdmittanceMatrix admittanceMatrixOpenEnd = xfmr3Model.getAdmittanceMatrixEnd1();
            calculateTwoConnectedEndsFlow(n, nEnd2, nEnd3, v2, angleDegrees2, v3, angleDegrees3,
                    admittanceMatrixEnd2, admittanceMatrixEnd3, admittanceMatrixOpenEnd);
        } else if (connected1) {
            BranchAdmittanceMatrix admittanceMatrixEnd1 = xfmr3Model.getAdmittanceMatrixEnd1();
            BranchAdmittanceMatrix admittanceMatrixFirstOpenEnd = xfmr3Model.getAdmittanceMatrixEnd2();
            BranchAdmittanceMatrix admittanceMatrixSecondOpenEnd = xfmr3Model.getAdmittanceMatrixEnd3();
            calculateOneConnectedEndFlow(n, nEnd1, v1, angleDegrees1, admittanceMatrixEnd1,
                    admittanceMatrixFirstOpenEnd, admittanceMatrixSecondOpenEnd);
        } else if (connected2) {
            BranchAdmittanceMatrix admittanceMatrixEnd2 = xfmr3Model.getAdmittanceMatrixEnd2();
            BranchAdmittanceMatrix admittanceMatrixFirstOpenEnd = xfmr3Model.getAdmittanceMatrixEnd1();
            BranchAdmittanceMatrix admittanceMatrixSecondOpenEnd = xfmr3Model.getAdmittanceMatrixEnd3();
            calculateOneConnectedEndFlow(n, nEnd2, v2, angleDegrees2, admittanceMatrixEnd2,
                    admittanceMatrixFirstOpenEnd, admittanceMatrixSecondOpenEnd);
        } else if (connected3) {
            BranchAdmittanceMatrix admittanceMatrixEnd3 = xfmr3Model.getAdmittanceMatrixEnd3();
            BranchAdmittanceMatrix admittanceMatrixFirstOpenEnd = xfmr3Model.getAdmittanceMatrixEnd1();
            BranchAdmittanceMatrix admittanceMatrixSecondOpenEnd = xfmr3Model.getAdmittanceMatrixEnd2();
            calculateOneConnectedEndFlow(n, nEnd3, v3, angleDegrees3, admittanceMatrixEnd3,
                    admittanceMatrixFirstOpenEnd, admittanceMatrixSecondOpenEnd);
        }
    }

    // Line and Xfmr2 flow calculations
    private void calculateEndFromFlow(String n, String nEnd1, double v1, double angleDegrees1,
            BranchAdmittanceMatrix admittanceMatrix) {
        calculateEndFlow(n, nEnd1, v1, angleDegrees1, admittanceMatrix, false);
    }

    private void calculateEndToFlow(String n, String nEnd2, double v2, double angleDegrees2,
            BranchAdmittanceMatrix admittanceMatrix) {
        calculateEndFlow(n, nEnd2, v2, angleDegrees2, admittanceMatrix, true);
    }

    private void calculateEndFlow(String n, String nEnd, double v, double angleDegrees,
            BranchAdmittanceMatrix admittanceMatrix, boolean isOpenFrom) {
        if (v == 0.0) {
            return;
        }
        double angle = Math.toRadians(angleDegrees);
        Complex a = new Complex(v * Math.cos(angle), v * Math.sin(angle));

        if (nEnd.equals(n)) {
            Complex ysh = kronAntenna(admittanceMatrix.y11, admittanceMatrix.y12, admittanceMatrix.y21,
                    admittanceMatrix.y22, isOpenFrom);
            p = ysh.getReal() * a.abs() * a.abs();
            q = -ysh.getImaginary() * a.abs() * a.abs();
        } else {
            LOG.warn("calculateEndToFlow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees};
        badVoltage = !anglesAreOk(angles);
    }

    private void calculateBothEndsFlow(String n, String nEnd1, String nEnd2, double v1, double angleDegrees1, double v2,
            double angleDegrees2, BranchAdmittanceMatrix admittanceMatrix) {
        if (v1 == 0.0 || v2 == 0.0) {
            return;
        }
        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        flowBothEnds(admittanceMatrix.y11, admittanceMatrix.y12,
                admittanceMatrix.y21, admittanceMatrix.y22, vf, vt);

        if (nEnd1.equals(n)) {
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd2.equals(n)) {
            p = stf.getReal();
            q = stf.getImaginary();
        } else {
            LOG.warn("calculateBothEndsFlow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees1, angleDegrees2};
        badVoltage = !anglesAreOk(angles);
    }

    // Xfmr3 flow calculations
    private void calculateOneConnectedEndFlow(String n, String nEnd, double v, double angleDegrees,
            BranchAdmittanceMatrix admittanceMatrixEnd, BranchAdmittanceMatrix admittanceMatrixOpenEnd1,
            BranchAdmittanceMatrix admittanceMatrixOpenEnd2) {
        if (v == 0.0) {
            return;
        }
        double angle = Math.toRadians(angleDegrees);
        Complex vf = new Complex(v * Math.cos(angle), v * Math.sin(angle));

        Complex ysh = calculateEndShunt(admittanceMatrixEnd.y11, admittanceMatrixEnd.y12, admittanceMatrixEnd.y21,
                admittanceMatrixEnd.y22, admittanceMatrixOpenEnd1.y11, admittanceMatrixOpenEnd1.y12,
                admittanceMatrixOpenEnd1.y21,
                admittanceMatrixOpenEnd1.y22, admittanceMatrixOpenEnd2.y11, admittanceMatrixOpenEnd2.y12,
                admittanceMatrixOpenEnd2.y21,
                admittanceMatrixOpenEnd2.y22);

        if (nEnd.equals(n)) {
            p = ysh.getReal() * vf.abs() * vf.abs();
            q = ysh.getImaginary() * vf.abs() * vf.abs();
        } else {
            LOG.warn("calculateEnd1Flow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees};
        badVoltage = !anglesAreOk(angles);
    }

    private void calculateTwoConnectedEndsFlow(String n, String nEnd1, String nEnd2, double v1, double angleDegrees1,
            double v2,
            double angleDegrees2, BranchAdmittanceMatrix admittanceMatrixEnd1,
            BranchAdmittanceMatrix admittanceMatrixEnd2, BranchAdmittanceMatrix admittanceMatrixOpenEnd) {
        if (v1 == 0.0 || v2 == 0.0) {
            return;
        }
        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vf1 = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vf2 = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        BranchAdmittanceMatrix admittance = calculateTwoConnectedEndsAdmittance(admittanceMatrixEnd1.y11,
                admittanceMatrixEnd1.y12,
                admittanceMatrixEnd1.y21, admittanceMatrixEnd1.y22, admittanceMatrixEnd2.y11, admittanceMatrixEnd2.y12,
                admittanceMatrixEnd2.y21, admittanceMatrixEnd2.y22, admittanceMatrixOpenEnd.y11,
                admittanceMatrixOpenEnd.y12,
                admittanceMatrixOpenEnd.y21, admittanceMatrixOpenEnd.y22);

        flowBothEnds(admittance.y11, admittance.y12, admittance.y21, admittance.y22, vf1, vf2);

        if (nEnd1.equals(n)) {
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd2.equals(n)) {
            p = sft.getReal();
            q = sft.getImaginary();
        } else {
            LOG.warn("calculateEnd1End2Flow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees1, angleDegrees2};
        badVoltage = !anglesAreOk(angles);
    }

    private void calculateThreeConnectedEndsFlow(String n, String nEnd1, String nEnd2, String nEnd3, double v1,
            double angleDegrees1,
            double v2, double angleDegrees2, double v3, double angleDegrees3,
            BranchAdmittanceMatrix admittanceMatrixEnd1, BranchAdmittanceMatrix admittanceMatrixEnd2,
            BranchAdmittanceMatrix admittanceMatrixEnd3) {
        if (v1 == 0.0 || v2 == 0.0 || v3 == 0.0) {
            return;
        }

        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        double angle3 = Math.toRadians(angleDegrees3);
        Complex vf1 = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vf2 = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));
        Complex vf3 = new Complex(v3 * Math.cos(angle3), v3 * Math.sin(angle3));

        Complex v0 = admittanceMatrixEnd1.y21.multiply(vf1).add(admittanceMatrixEnd2.y21.multiply(vf2))
                .add(admittanceMatrixEnd3.y21.multiply(vf3)).negate()
                .divide(admittanceMatrixEnd1.y22.add(admittanceMatrixEnd2.y22).add(admittanceMatrixEnd3.y22));

        if (nEnd1.equals(n)) {
            flowBothEnds(admittanceMatrixEnd1.y11, admittanceMatrixEnd1.y12, admittanceMatrixEnd1.y21,
                    admittanceMatrixEnd1.y22, vf1, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd2.equals(n)) {
            flowBothEnds(admittanceMatrixEnd2.y11, admittanceMatrixEnd2.y12, admittanceMatrixEnd2.y21,
                    admittanceMatrixEnd2.y22, vf2, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd3.equals(n)) {
            flowBothEnds(admittanceMatrixEnd3.y11, admittanceMatrixEnd3.y12, admittanceMatrixEnd3.y21,
                    admittanceMatrixEnd3.y22, vf3, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else {
            LOG.warn("calculate3EndsFlow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees1, angleDegrees2, angleDegrees3};
        badVoltage = !anglesAreOk(angles);
    }

    private Complex calculateEndShunt(Complex y11, Complex y12, Complex y21, Complex y22, Complex yFirstOpen11,
            Complex yFirstOpen12, Complex yFirstOpen21, Complex yFirstOpen22, Complex ySecondOpen11,
            Complex ySecondOpen12, Complex ySecondOpen21, Complex ySecondOpen22) {
        Complex ysh1 = kronAntenna(yFirstOpen11, yFirstOpen12, yFirstOpen21, yFirstOpen22, true);
        Complex ysh2 = kronAntenna(ySecondOpen11, ySecondOpen12, ySecondOpen21, ySecondOpen22, true);
        y22.add(ysh1).add(ysh2);

        return kronAntenna(y11, y12, y21, y22, false);
    }

    private BranchAdmittanceMatrix calculateTwoConnectedEndsAdmittance(Complex yFirstConnected11,
            Complex yFirstConnected12,
            Complex yFirstConnected21, Complex yFirstConnected22, Complex ySecondConnected11,
            Complex ySecondConnected12,
            Complex ySecondConnected21, Complex ySecondConnected22, Complex yOpen11, Complex yOpen12, Complex yOpen21,
            Complex yOpen22) {
        Complex ysh = kronAntenna(yOpen11, yOpen12, yOpen21, yOpen22, true);
        ySecondConnected22.add(ysh);

        return kronChain(yFirstConnected11, yFirstConnected12, yFirstConnected21, yFirstConnected22, ySecondConnected11,
                ySecondConnected12, ySecondConnected21, ySecondConnected22);
    }

    private Complex kronAntenna(Complex y11, Complex y12, Complex y21, Complex y22, boolean isOpenFrom) {
        Complex ysh = Complex.ZERO;

        if (isOpenFrom) {
            ysh = y22.subtract(y21.multiply(y12).divide(y11));
        } else {
            ysh = y11.subtract(y12.multiply(y21).divide(y22));
        }
        return ysh;
    }

    private BranchAdmittanceMatrix kronChain(Complex yFirstConnected11, Complex yFirstConnected12,
            Complex yFirstConnected21, Complex yFirstConnected22, Complex ySecondConnected11,
            Complex ySecondConnected12, Complex ySecondConnected21, Complex ySecondConnected22) {
        BranchAdmittanceMatrix admittance = new BranchAdmittanceMatrix();

        admittance.y11 = yFirstConnected11.subtract(yFirstConnected21.multiply(yFirstConnected12)
                        .divide(yFirstConnected22.add(ySecondConnected11)));
        admittance.y12 = ySecondConnected12.multiply(yFirstConnected12)
                .divide(yFirstConnected22.add(ySecondConnected11)).negate();
        admittance.y21 = yFirstConnected21.multiply(ySecondConnected21)
                .divide(yFirstConnected22.add(ySecondConnected11)).negate();
        admittance.y22 = ySecondConnected22.subtract(
                ySecondConnected12.multiply(ySecondConnected21).divide(yFirstConnected22.add(ySecondConnected11)));

        return admittance;
    }

    private void flowBothEnds(Complex y11, Complex y12, Complex y21, Complex y22, Complex v1, Complex v2) {
        Complex ift = y12.multiply(v2).add(y11.multiply(v1));
        sft = ift.conjugate().multiply(v1);

        Complex itf = y21.multiply(v1).add(y22.multiply(v2));
        stf = itf.conjugate().multiply(v2);
    }

    private boolean calculateFlowXfmr3IsOk(double r1, double x1, double r2, double x2, double r3, double x3) {
        if (r1 == 0.0 && x1 == 0.0 || r2 == 0.0 && x2 == 0.0 || r3 == 0.0 && x3 == 0.0) {
            return false;
        }
        return true;
    }

    private boolean anglesAreOk(double[] angleDegrees) {
        for (double angleDegree : angleDegrees) {
            if (angleDegree == 0.0) {
                return false;
            }
        }
        return true;
    }

    public double getP() {
        return p;
    }

    public double getQ() {
        return q;
    }

    public boolean getCalculated() {
        return calculated;
    }

    public boolean getBadVoltage() {
        return badVoltage;
    }

    public DetectedEquipmentModel getEquipmentModel() {
        return equipmentModel;
    }

    private double                 p;
    private double                 q;
    private boolean                calculated;
    private boolean                badVoltage;
    private DetectedEquipmentModel equipmentModel;
    private Complex                sft;
    private Complex                stf;
    private InterpretedModel       inputModel;
    private static final Logger    LOG = LoggerFactory.getLogger(FlowCalculator.class);
}
