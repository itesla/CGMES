package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.triplestore.api.PropertyBag;

public class CalculateFlow {

    public CalculateFlow(PrepareModel inputModel) {
        this.inputModel = inputModel;
        this.p = 0.0;
        this.q = 0.0;
        this.calculated = false;
        this.badVoltage = false;
    }

    public void calculateFlowLine(String n, PropertyBag node1, PropertyBag node2, PropertyBag line,
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
        LineAdmittanceMatrix admittanceMatrix = new LineAdmittanceMatrix(line, config);
        admittanceMatrix.calculate(nominalV1, nominalV2);
        equipmentModel = new DetectedEquipmentModel(admittanceMatrix.branchModel);

        String nEnd1 = line.get("terminal1");
        String nEnd2 = line.get("terminal2");
        if (connected1 && connected2) {
            calculateBothEndsFlow(n, nEnd1, nEnd2, v1, angleDegrees1, v2, angleDegrees2,
                    admittanceMatrix);
        } else if (connected1) {
            calculateEndFromFlow(n, nEnd1, v1, angleDegrees1, admittanceMatrix);
        } else if (connected2) {
            calculateEndToFlow(n, nEnd2, v2, angleDegrees2, admittanceMatrix);
        }
    }

    public void calculateFlowT2x(String n, PropertyBag node1, PropertyBag node2,
            PropertyBag transformer, CgmesEquipmentModelMapping config) {
        double v1 = node1.asDouble("v");
        double angleDegrees1 = node1.asDouble("angle");
        double v2 = node2.asDouble("v");
        double angleDegrees2 = node2.asDouble("angle");
        Boolean connected1 = transformer.asBoolean("connected1", false);
        Boolean connected2 = transformer.asBoolean("connected2", false);

        // The admittance and the model code can always be calculated
        T2xAdmittanceMatrix admittanceMatrix = new T2xAdmittanceMatrix(inputModel.getCgmes(), transformer, config);
        admittanceMatrix.calculate();
        equipmentModel = new DetectedEquipmentModel(admittanceMatrix.branchModel);

        String nEnd1 = transformer.get("terminal1");
        String nEnd2 = transformer.get("terminal2");
        if (connected1 && connected2) {
            calculateBothEndsFlow(n, nEnd1, nEnd2, v1, angleDegrees1, v2, angleDegrees2,
                    admittanceMatrix);
        } else if (connected1) {
            calculateEndFromFlow(n, nEnd1, v1, angleDegrees1, admittanceMatrix);
        } else if (connected2) {
            calculateEndToFlow(n, nEnd2, v2, angleDegrees2, admittanceMatrix);
        }
    }

    public void calculateFlowT3x(String n, PropertyBag node1, PropertyBag node2, PropertyBag node3,
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
        T3xAdmittanceMatrix admittanceMatrix = new T3xAdmittanceMatrix(inputModel.getCgmes(), transformer, config);
        admittanceMatrix.calculate();
        equipmentModel = new DetectedEquipmentModel(admittanceMatrix.end1.branchModel,
                admittanceMatrix.end2.branchModel, admittanceMatrix.end3.branchModel);

        if (!calculateFlowT3xIsOk(r1, x1, r2, x2, r3, x3)) {
            return;
        }

        String nEnd1 = transformer.get("terminal1");
        String nEnd2 = transformer.get("terminal2");
        String nEnd3 = transformer.get("terminal3");
        if (connected1 && connected2 && connected3) {
            calculate3EndsFlow(n, nEnd1, nEnd2, nEnd3, v1, angleDegrees1, v2, angleDegrees2, v3,
                    angleDegrees3, admittanceMatrix);
        } else if (connected1 && connected2) {
            calculateEnd1End2Flow(n, nEnd1, nEnd2, v1, angleDegrees1, v2, angleDegrees2,
                    admittanceMatrix);
        } else if (connected1 && connected3) {
            calculateEnd1End3Flow(n, nEnd1, nEnd3, v1, angleDegrees1, v3, angleDegrees3,
                    admittanceMatrix);
        } else if (connected2 && connected3) {
            calculateEnd2End3Flow(n, nEnd2, nEnd3, v2, angleDegrees2, v3, angleDegrees3,
                    admittanceMatrix);
        } else if (connected1) {
            calculateEnd1Flow(n, nEnd1, v1, angleDegrees1, admittanceMatrix);
        } else if (connected2) {
            calculateEnd2Flow(n, nEnd2, v2, angleDegrees2, admittanceMatrix);
        } else if (connected3) {
            calculateEnd3Flow(n, nEnd3, v3, angleDegrees3, admittanceMatrix);
        }
    }

    private void calculateEndFromFlow(String n, String nEnd1, double v1, double angleDegrees1,
            AbstractAdmittanceMatrix admittanceMatrix) {
        if (v1 == 0.0) {
            return;
        }
        double angle1 = Math.toRadians(angleDegrees1);
        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));

        if (nEnd1.equals(n)) {
            Complex ysh = kronAntenna(admittanceMatrix.yff, admittanceMatrix.yft,
                    admittanceMatrix.ytf, admittanceMatrix.ytt, false);
            p = ysh.getReal() * vf.abs() * vf.abs();
            q = -ysh.getImaginary() * vf.abs() * vf.abs();
        } else {
            LOG.warn("calculateEndFromFlow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees1};
        badVoltage = !anglesAreOk(angles);
    }

    private void calculateEndToFlow(String n, String nEnd2, double v2, double angleDegrees2,
            AbstractAdmittanceMatrix admittanceMatrix) {
        if (v2 == 0.0) {
            return;
        }
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        if (nEnd2.equals(n)) {
            Complex ysh = kronAntenna(admittanceMatrix.yff, admittanceMatrix.yft,
                    admittanceMatrix.ytf, admittanceMatrix.ytt, true);
            p = ysh.getReal() * vt.abs() * vt.abs();
            q = -ysh.getImaginary() * vt.abs() * vt.abs();
        } else {
            LOG.warn("calculateEndToFlow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees2};
        badVoltage = !anglesAreOk(angles);
    }

    private void calculateBothEndsFlow(String n, String nEnd1, String nEnd2, double v1,
            double angleDegrees1,
            double v2, double angleDegrees2, AbstractAdmittanceMatrix admittanceMatrix) {
        if (v1 == 0.0 || v2 == 0.0) {
            return;
        }
        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        flowBothEnds(admittanceMatrix.yff, admittanceMatrix.yft,
                admittanceMatrix.ytf, admittanceMatrix.ytt, vf, vt);

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

    // T3x flow calculations
    private void calculateEnd1Flow(String n, String nEnd1, double v1,
            double angleDegrees1, T3xAdmittanceMatrix admittanceMatrix) {
        if (v1 == 0.0) {
            return;
        }
        double angle1 = Math.toRadians(angleDegrees1);
        Complex vf1 = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));

        Complex ysh = calculateEndShunt(admittanceMatrix.end1.yff, admittanceMatrix.end1.yft,
                admittanceMatrix.end1.ytf, admittanceMatrix.end1.ytt,
                admittanceMatrix.end2.yff, admittanceMatrix.end2.yft,
                admittanceMatrix.end2.ytf, admittanceMatrix.end2.ytt,
                admittanceMatrix.end3.yff, admittanceMatrix.end3.yft,
                admittanceMatrix.end3.ytf, admittanceMatrix.end3.ytt);

        if (nEnd1.equals(n)) {
            p = ysh.getReal() * vf1.abs() * vf1.abs();
            q = ysh.getImaginary() * vf1.abs() * vf1.abs();
        } else {
            LOG.warn("calculateEnd1Flow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees1};
        badVoltage = !anglesAreOk(angles);
    }

    private void calculateEnd2Flow(String n, String nEnd2, double v2,
            double angleDegrees2, T3xAdmittanceMatrix admittanceMatrix) {
        if (v2 == 0.0) {
            return;
        }
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vf2 = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        Complex ysh = calculateEndShunt(admittanceMatrix.end2.yff, admittanceMatrix.end2.yft,
                admittanceMatrix.end2.ytf, admittanceMatrix.end2.ytt,
                admittanceMatrix.end1.yff, admittanceMatrix.end1.yft,
                admittanceMatrix.end1.ytf, admittanceMatrix.end1.ytt,
                admittanceMatrix.end3.yff, admittanceMatrix.end3.yft,
                admittanceMatrix.end3.ytf, admittanceMatrix.end3.ytt);

        if (nEnd2.equals(n)) {
            p = ysh.getReal() * vf2.abs() * vf2.abs();
            q = ysh.getImaginary() * vf2.abs() * vf2.abs();
        } else {
            LOG.warn("calculateEnd2Flow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees2};
        badVoltage = !anglesAreOk(angles);
    }

    private void calculateEnd3Flow(String n, String nEnd3, double v3,
            double angleDegrees3, T3xAdmittanceMatrix admittanceMatrix) {
        if (v3 == 0.0) {
            return;
        }
        double angle3 = Math.toRadians(angleDegrees3);
        Complex vf3 = new Complex(v3 * Math.cos(angle3), v3 * Math.sin(angle3));

        Complex ysh = calculateEndShunt(admittanceMatrix.end3.yff, admittanceMatrix.end3.yft,
                admittanceMatrix.end3.ytf, admittanceMatrix.end3.ytt,
                admittanceMatrix.end1.yff, admittanceMatrix.end1.yft,
                admittanceMatrix.end1.ytf, admittanceMatrix.end1.ytt,
                admittanceMatrix.end2.yff, admittanceMatrix.end2.yft,
                admittanceMatrix.end2.ytf, admittanceMatrix.end2.ytt);

        if (nEnd3.equals(n)) {
            p = ysh.getReal() * vf3.abs() * vf3.abs();
            q = ysh.getImaginary() * vf3.abs() * vf3.abs();
        } else {
            LOG.warn("calculateEnd3Flow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees3};
        badVoltage = !anglesAreOk(angles);
    }

    private void calculateEnd1End2Flow(String n, String nEnd1, String nEnd2, double v1,
            double angleDegrees1,
            double v2, double angleDegrees2, T3xAdmittanceMatrix admittanceMatrix) {
        if (v1 == 0.0 || v2 == 0.0) {
            return;
        }
        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vf1 = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vf2 = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        KronChainAdmittance admittance = calculate2EndsAdmittance(admittanceMatrix.end1.yff,
                admittanceMatrix.end1.yft,
                admittanceMatrix.end1.ytf, admittanceMatrix.end1.ytt,
                admittanceMatrix.end2.yff, admittanceMatrix.end2.yft,
                admittanceMatrix.end2.ytf, admittanceMatrix.end2.ytt,
                admittanceMatrix.end3.yff, admittanceMatrix.end3.yft,
                admittanceMatrix.end3.ytf, admittanceMatrix.end3.ytt);

        flowBothEnds(admittance.yff, admittance.yft,
                admittance.ytf, admittance.ytt, vf1, vf2);

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

    private void calculateEnd1End3Flow(String n, String nEnd1, String nEnd3, double v1,
            double angleDegrees1,
            double v3, double angleDegrees3, T3xAdmittanceMatrix admittanceMatrix) {
        if (v1 == 0.0 || v3 == 0.0) {
            return;
        }
        double angle1 = Math.toRadians(angleDegrees1);
        double angle3 = Math.toRadians(angleDegrees3);
        Complex vf1 = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vf3 = new Complex(v3 * Math.cos(angle3), v3 * Math.sin(angle3));

        KronChainAdmittance admittance = calculate2EndsAdmittance(admittanceMatrix.end1.yff,
                admittanceMatrix.end1.yft,
                admittanceMatrix.end1.ytf, admittanceMatrix.end1.ytt,
                admittanceMatrix.end3.yff, admittanceMatrix.end3.yft,
                admittanceMatrix.end3.ytf, admittanceMatrix.end3.ytt,
                admittanceMatrix.end2.yff, admittanceMatrix.end2.yft,
                admittanceMatrix.end2.ytf, admittanceMatrix.end2.ytt);

        flowBothEnds(admittance.yff, admittance.yft,
                admittance.ytf, admittance.ytt, vf1, vf3);

        if (nEnd1.equals(n)) {
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd3.equals(n)) {
            p = sft.getReal();
            q = sft.getImaginary();
        } else {
            LOG.warn("calculateEnd1End3Flow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees1, angleDegrees3};
        badVoltage = !anglesAreOk(angles);
    }

    private void calculateEnd2End3Flow(String n, String nEnd2, String nEnd3, double v2,
            double angleDegrees2,
            double v3, double angleDegrees3, T3xAdmittanceMatrix admittanceMatrix) {
        if (v2 == 0.0 || v3 == 0.0) {
            return;
        }
        double angle2 = Math.toRadians(angleDegrees2);
        double angle3 = Math.toRadians(angleDegrees3);
        Complex vf2 = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));
        Complex vf3 = new Complex(v3 * Math.cos(angle3), v3 * Math.sin(angle3));

        KronChainAdmittance admittance = calculate2EndsAdmittance(admittanceMatrix.end2.yff,
                admittanceMatrix.end2.yft,
                admittanceMatrix.end2.ytf, admittanceMatrix.end2.ytt,
                admittanceMatrix.end3.yff, admittanceMatrix.end3.yft,
                admittanceMatrix.end3.ytf, admittanceMatrix.end3.ytt,
                admittanceMatrix.end1.yff, admittanceMatrix.end1.yft,
                admittanceMatrix.end1.ytf, admittanceMatrix.end1.ytt);

        flowBothEnds(admittance.yff, admittance.yft,
                admittance.ytf, admittance.ytt, vf2, vf3);

        if (nEnd2.equals(n)) {
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd3.equals(n)) {
            p = sft.getReal();
            q = sft.getImaginary();
        } else {
            LOG.warn("calculateEnd2End3Flow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees2, angleDegrees3};
        badVoltage = !anglesAreOk(angles);
    }

    private void calculate3EndsFlow(String n, String nEnd1, String nEnd2, String nEnd3, double v1,
            double angleDegrees1,
            double v2, double angleDegrees2, double v3, double angleDegrees3,
            T3xAdmittanceMatrix admittanceMatrix) {
        if (v1 == 0.0 || v2 == 0.0 || v3 == 0.0) {
            return;
        }

        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        double angle3 = Math.toRadians(angleDegrees3);
        Complex vf1 = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vf2 = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));
        Complex vf3 = new Complex(v3 * Math.cos(angle3), v3 * Math.sin(angle3));

        Complex v0 = admittanceMatrix.end1.ytf.multiply(vf1)
                .add(admittanceMatrix.end2.ytf.multiply(vf2))
                .add(admittanceMatrix.end3.ytf.multiply(vf3))
                .negate().divide(admittanceMatrix.end1.ytt.add(admittanceMatrix.end2.ytt)
                        .add(admittanceMatrix.end3.ytt));

        if (nEnd1.equals(n)) {
            flowBothEnds(admittanceMatrix.end1.yff, admittanceMatrix.end1.yft,
                    admittanceMatrix.end1.ytf, admittanceMatrix.end1.ytt, vf1, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd2.equals(n)) {
            flowBothEnds(admittanceMatrix.end2.yff, admittanceMatrix.end2.yft,
                    admittanceMatrix.end2.ytf, admittanceMatrix.end2.ytt, vf2, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd3.equals(n)) {
            flowBothEnds(admittanceMatrix.end3.yff, admittanceMatrix.end3.yft,
                    admittanceMatrix.end3.ytf, admittanceMatrix.end3.ytt, vf3, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else {
            LOG.warn("calculate3EndsFlow. Unexpected node");
        }
        calculated = true;
        double[] angles = {angleDegrees1, angleDegrees2, angleDegrees3};
        badVoltage = !anglesAreOk(angles);
    }

    private Complex calculateEndShunt(Complex yff, Complex yft, Complex ytf,
            Complex ytt, Complex y1Openff, Complex y1Openft, Complex y1Opentf, Complex y1Opentt,
            Complex y2Openff,
            Complex y2Openft, Complex y2Opentf, Complex y2Opentt) {
        Complex ysh1 = kronAntenna(y1Openff, y1Openft, y1Opentf, y1Opentt, true);
        Complex ysh2 = kronAntenna(y2Openff, y2Openft, y2Opentf, y2Opentt, true);
        ytt.add(ysh1).add(ysh2);

        return kronAntenna(yff, yft, ytf, ytt, false);
    }

    private KronChainAdmittance calculate2EndsAdmittance(Complex y1ff, Complex y1ft, Complex y1tf,
            Complex y1tt, Complex y2ff, Complex y2ft, Complex y2tf, Complex y2tt, Complex yOpenff,
            Complex yOpenft, Complex yOpentf, Complex yOpentt) {
        Complex ysh = kronAntenna(yOpenff, yOpenft, yOpentf, yOpentt, true);
        y2tt.add(ysh);

        return kronChain(y1ff, y1ft, y1tf, y1tt, y2ff, y2ft, y2tf, y2tt);
    }

    private Complex kronAntenna(Complex yff, Complex yft, Complex ytf, Complex ytt,
            boolean isOpenFrom) {
        Complex ysh = Complex.ZERO;

        if (isOpenFrom) {
            ysh = ytt.subtract(ytf.multiply(yft).divide(yff));
        } else {
            ysh = yff.subtract(yft.multiply(ytf).divide(ytt));
        }
        return ysh;
    }

    private KronChainAdmittance kronChain(Complex y1ff, Complex y1ft, Complex y1tf, Complex y1tt,
            Complex y2ff, Complex y2ft, Complex y2tf, Complex y2tt) {
        KronChainAdmittance admittance = new KronChainAdmittance();

        admittance.yff = y1ff.subtract(y1tf.multiply(y1ft).divide(y1tt.add(y2ff)));
        admittance.yft = y2ft.multiply(y1ft).divide(y1tt.add(y2ff)).negate();
        admittance.ytf = y1tf.multiply(y2tf).divide(y1tt.add(y2ff)).negate();
        admittance.ytt = y2tt.subtract(y2ft.multiply(y2tf).divide(y1tt.add(y2ff)));

        return admittance;
    }

    private void flowBothEnds(Complex yff, Complex yft, Complex ytf, Complex ytt, Complex vf,
            Complex vt) {
        Complex ift = yft.multiply(vt).add(yff.multiply(vf));
        sft = ift.conjugate().multiply(vf);

        Complex itf = ytf.multiply(vf).add(ytt.multiply(vt));
        stf = itf.conjugate().multiply(vt);
    }

    private boolean calculateFlowT3xIsOk(double r1, double x1, double r2, double x2, double r3, double x3) {
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

    static class KronChainAdmittance {
        Complex yff;
        Complex yft;
        Complex ytf;
        Complex ytt;
    }

    private double                 p;
    private double                 q;
    private boolean                calculated;
    private boolean                badVoltage;
    private DetectedEquipmentModel equipmentModel;
    private Complex                sft;
    private Complex                stf;
    private PrepareModel           inputModel;
    private static final Logger    LOG = LoggerFactory
            .getLogger(CalculateFlow.class);
}
