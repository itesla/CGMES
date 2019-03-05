package com.powsybl.cgmes.validation.test.flow;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.triplestore.api.PropertyBag;

public class CalcFlow {

    public CalcFlow(PrepareModel inputModel) {
        this.inputModel = inputModel;
        this.p = 0.0;
        this.q = 0.0;
        this.calculated = false;
        this.badVoltage = false;
        this.modelCode = "";
    }

    public void calcFlowT2x(String n, PropertyBag node1, PropertyBag node2,
            PropertyBag transformer, String config) {
        double v1 = node1.asDouble("v");
        double angleDegrees1 = node1.asDouble("angle");
        double v2 = node2.asDouble("v");
        double angleDegrees2 = node2.asDouble("angle");
        Boolean connected1 = transformer.asBoolean("connected1", false);
        Boolean connected2 = transformer.asBoolean("connected2", false);

        // The admittance and the model code can always be calculated
        T2xAdmittanceMatrix admittanceMatrix = new T2xAdmittanceMatrix(inputModel.getCgmes());
        admittanceMatrix.calculate(transformer, config);
        modelCode = admittanceMatrix.getModelCode();

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

    public void calcFlowT3x(String n, PropertyBag node1, PropertyBag node2, PropertyBag node3,
            PropertyBag transformer, String config) {
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
        T3xAdmittanceMatrix admittanceMatrix = new T3xAdmittanceMatrix(inputModel.getCgmes());
        admittanceMatrix.calculate(transformer, config);
        modelCode = admittanceMatrix.getModelCode();

        if (!calcFlowT3xIsOk(r1, x1, r2, x2, r3, x3)) {
            LOG.warn("T3x {}", transformer);
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

    public void calcFlowLine(String n, PropertyBag node1, PropertyBag node2, PropertyBag line,
            String config) {
        double v1 = node1.asDouble("v");
        double nominalV1 = node1.asDouble("nominalV");
        double angleDegrees1 = node1.asDouble("angle");
        double v2 = node2.asDouble("v");
        double nominalV2 = node2.asDouble("nominalV");
        double angleDegrees2 = node2.asDouble("angle");
        Boolean connected1 = line.asBoolean("connected1", false);
        Boolean connected2 = line.asBoolean("connected2", false);

        // The admittance and model code can always be calculated
        LineAdmittanceMatrix admittanceMatrix = new LineAdmittanceMatrix(inputModel.getCgmes());
        admittanceMatrix.calculate(line, nominalV1, nominalV2, config);
        modelCode = admittanceMatrix.getModelCode();

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

    private void calculateEndFromFlow(String n, String nEnd1, double v1, double angleDegrees1,
            AdmittanceMatrix admittanceMatrix) {
        if (v1 == 0.0) {
            return;
        }
        double angle1 = Math.toRadians(angleDegrees1);
        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));

        if (nEnd1.equals(n)) {
            Complex ysh = kronAntenna(admittanceMatrix.getYff(), admittanceMatrix.getYft(),
                    admittanceMatrix.getYtf(), admittanceMatrix.getYtt(), false);
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
            AdmittanceMatrix admittanceMatrix) {
        if (v2 == 0.0) {
            return;
        }
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        if (nEnd2.equals(n)) {
            Complex ysh = kronAntenna(admittanceMatrix.getYff(), admittanceMatrix.getYft(),
                    admittanceMatrix.getYtf(), admittanceMatrix.getYtt(), true);
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
            double v2, double angleDegrees2, AdmittanceMatrix admittanceMatrix) {
        if (v1 == 0.0 || v2 == 0.0) {
            return;
        }
        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        flowBothEnds(admittanceMatrix.getYff(), admittanceMatrix.getYft(),
                admittanceMatrix.getYtf(), admittanceMatrix.getYtt(), vf, vt);

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

        Complex ysh = calculateEndShunt(admittanceMatrix.getYff1(), admittanceMatrix.getYft1(),
                admittanceMatrix.getYtf1(), admittanceMatrix.getYtt1(),
                admittanceMatrix.getYff2(), admittanceMatrix.getYft2(),
                admittanceMatrix.getYtf2(), admittanceMatrix.getYtt2(),
                admittanceMatrix.getYff3(), admittanceMatrix.getYft3(),
                admittanceMatrix.getYtf3(), admittanceMatrix.getYtt3());

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

        Complex ysh = calculateEndShunt(admittanceMatrix.getYff2(), admittanceMatrix.getYft2(),
                admittanceMatrix.getYtf2(), admittanceMatrix.getYtt2(),
                admittanceMatrix.getYff1(), admittanceMatrix.getYft1(),
                admittanceMatrix.getYtf1(), admittanceMatrix.getYtt1(),
                admittanceMatrix.getYff3(), admittanceMatrix.getYft3(),
                admittanceMatrix.getYtf3(), admittanceMatrix.getYtt3());

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

        Complex ysh = calculateEndShunt(admittanceMatrix.getYff3(), admittanceMatrix.getYft3(),
                admittanceMatrix.getYtf3(), admittanceMatrix.getYtt3(),
                admittanceMatrix.getYff1(), admittanceMatrix.getYft1(),
                admittanceMatrix.getYtf1(), admittanceMatrix.getYtt1(),
                admittanceMatrix.getYff2(), admittanceMatrix.getYft2(),
                admittanceMatrix.getYtf2(), admittanceMatrix.getYtt2());

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

        KronChainAdmittance admittance = calculate2EndsAdmittance(admittanceMatrix.getYff1(),
                admittanceMatrix.getYft1(),
                admittanceMatrix.getYtf1(), admittanceMatrix.getYtt1(),
                admittanceMatrix.getYff2(), admittanceMatrix.getYft2(),
                admittanceMatrix.getYtf2(), admittanceMatrix.getYtt2(),
                admittanceMatrix.getYff3(), admittanceMatrix.getYft3(),
                admittanceMatrix.getYtf3(), admittanceMatrix.getYtt3());

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

        KronChainAdmittance admittance = calculate2EndsAdmittance(admittanceMatrix.getYff1(),
                admittanceMatrix.getYft1(),
                admittanceMatrix.getYtf1(), admittanceMatrix.getYtt1(),
                admittanceMatrix.getYff3(), admittanceMatrix.getYft3(),
                admittanceMatrix.getYtf3(), admittanceMatrix.getYtt3(),
                admittanceMatrix.getYff2(), admittanceMatrix.getYft2(),
                admittanceMatrix.getYtf2(), admittanceMatrix.getYtt2());

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

        KronChainAdmittance admittance = calculate2EndsAdmittance(admittanceMatrix.getYff2(),
                admittanceMatrix.getYft2(),
                admittanceMatrix.getYtf2(), admittanceMatrix.getYtt2(),
                admittanceMatrix.getYff3(), admittanceMatrix.getYft3(),
                admittanceMatrix.getYtf3(), admittanceMatrix.getYtt3(),
                admittanceMatrix.getYff1(), admittanceMatrix.getYft1(),
                admittanceMatrix.getYtf1(), admittanceMatrix.getYtt1());

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

        Complex v0 = admittanceMatrix.getYtf1().multiply(vf1)
                .add(admittanceMatrix.getYtf2().multiply(vf2))
                .add(admittanceMatrix.getYtf3().multiply(vf3))
                .negate().divide(admittanceMatrix.getYtt1().add(admittanceMatrix.getYtt2())
                        .add(admittanceMatrix.getYtt3()));

        if (nEnd1.equals(n)) {
            flowBothEnds(admittanceMatrix.getYff1(), admittanceMatrix.getYft1(),
                    admittanceMatrix.getYtf1(), admittanceMatrix.getYtt1(), vf1, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd2.equals(n)) {
            flowBothEnds(admittanceMatrix.getYff2(), admittanceMatrix.getYft2(),
                    admittanceMatrix.getYtf2(), admittanceMatrix.getYtt2(), vf2, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (nEnd3.equals(n)) {
            flowBothEnds(admittanceMatrix.getYff3(), admittanceMatrix.getYft3(),
                    admittanceMatrix.getYtf3(), admittanceMatrix.getYtt3(), vf3, v0);
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

    private boolean calcFlowT3xIsOk(double r1, double x1, double r2, double x2,
            double r3, double x3) {
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

    public String getModelCode() {
        return modelCode;
    }

    public boolean getCalculated() {
        return calculated;
    }

    public boolean getBadVoltage() {
        return badVoltage;
    }

    class KronChainAdmittance {
        Complex yff;
        Complex yft;
        Complex ytf;
        Complex ytt;
    }

    private double              p;
    private double              q;
    private String              modelCode;
    private boolean             calculated;
    private boolean             badVoltage;
    private Complex             sft;
    private Complex             stf;
    private PrepareModel        inputModel;
    private static final Logger LOG = LoggerFactory
            .getLogger(CalcFlow.class);
}
