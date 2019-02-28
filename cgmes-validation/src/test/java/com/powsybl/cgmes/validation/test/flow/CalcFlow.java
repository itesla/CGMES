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
        this.modelCode = "";
        this.calculated = false;
    }

    public void calcFlowT2x(String n, PropertyBag node1, PropertyBag node2,
            PropertyBag transformer, String config) {
        double r1 = transformer.asDouble("r1");
        double x1 = transformer.asDouble("x1");
        double r2 = transformer.asDouble("r2");
        double x2 = transformer.asDouble("x2");
        double v1 = node1.asDouble("v");
        double nominalV1 = node1.asDouble("nominalV");
        double angleDegrees1 = node1.asDouble("angle");
        double v2 = node2.asDouble("v");
        double nominalV2 = node2.asDouble("nominalV");
        double angleDegrees2 = node2.asDouble("angle");
        Boolean connected1 = transformer.asBoolean("connected1", false);
        Boolean connected2 = transformer.asBoolean("connected2", false);

        // The admittance and the model code can always be calculated
        T2xAdmittanceMatrix admittanceMatrix = new T2xAdmittanceMatrix(inputModel.getCgmes());
        admittanceMatrix.calculate(transformer, config);
        modelCode = admittanceMatrix.getModelCode();

        if (!calcFlowT2xIsOk(connected1, connected2, nominalV1, nominalV2, r1, x1, r2, x2, v1,
                angleDegrees1, v2, angleDegrees2)) {
            return;
        }

        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        calculate(admittanceMatrix.getYff(), admittanceMatrix.getYft(),
                admittanceMatrix.getYtf(), admittanceMatrix.getYtt(), vf, vt);

        if (transformer.get("terminal1").equals(n)) {
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (transformer.get("terminal2").equals(n)) {
            p = stf.getReal();
            q = stf.getImaginary();
        }
        if (n.startsWith("_ARGIAP7_TN1") || n.startsWith("_ARGIAP7_TN2")) {
            LOG.debug("t2x {}", transformer);
            LOG.debug("node1 {}", node1);
            LOG.debug("node2 {}", node2);
            LOG.debug("p {} q {}", p, q);
        }
        calculated = true;
        badVoltage = !voltageIsOk(angleDegrees1, angleDegrees2);
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

        if (!calcFlowT3xIsOk(connected1, connected2, connected3, r1, x1, r2, x2, r3, x3, v1,
                angleDegrees1, v2, angleDegrees2, v3, angleDegrees3)) {
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

        if (transformer.get("terminal1").equals(n)) {
            calculate(admittanceMatrix.getYff1(), admittanceMatrix.getYft1(),
                    admittanceMatrix.getYtf1(), admittanceMatrix.getYtt1(), vf1, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (transformer.get("terminal2").equals(n)) {
            calculate(admittanceMatrix.getYff2(), admittanceMatrix.getYft2(),
                    admittanceMatrix.getYtf2(), admittanceMatrix.getYtt2(), vf2, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (transformer.get("terminal3").equals(n)) {
            calculate(admittanceMatrix.getYff3(), admittanceMatrix.getYft3(),
                    admittanceMatrix.getYtf3(), admittanceMatrix.getYtt3(), vf3, v0);
            p = sft.getReal();
            q = sft.getImaginary();
        }
        if (n.startsWith("_ARGIAP7_TN1") || n.startsWith("_ARGIAP7_TN2")) {
            LOG.debug("t3x {}", transformer);
            LOG.debug("node1 {}", node1);
            LOG.debug("node2 {}", node2);
            LOG.debug("node3 {}", node3);
            LOG.debug("p {} q {}", p, q);
        }
        calculated = true;
        badVoltage = !t3xVoltageIsOk(angleDegrees1, angleDegrees2, angleDegrees3);
    }

    public void calcFlowLine(String n, PropertyBag node1, PropertyBag node2, PropertyBag line,
            String config) {
        double r = line.asDouble("r");
        double x = line.asDouble("x");
        double v1 = node1.asDouble("v");
        double nominalV1 = node1.asDouble("nominalV");
        double angleDegrees1 = node1.asDouble("angle");
        double v2 = node2.asDouble("v");
        double nominalV2 = node2.asDouble("nominalV");
        double angleDegrees2 = node2.asDouble("angle");
        Boolean connected = line.asBoolean("connected", false);

        // The admittance and model code can always be calculated
        LineAdmittanceMatrix admittanceMatrix = new LineAdmittanceMatrix(inputModel.getCgmes());
        admittanceMatrix.calculate(line, nominalV1, nominalV2, config);
        modelCode = admittanceMatrix.getModelCode();

        if (!calcFlowLineIsOk(connected, r, x, nominalV1, nominalV2, v1, angleDegrees1, v2,
                angleDegrees2)) {
            return;
        }

        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        calculate(admittanceMatrix.getYff(), admittanceMatrix.getYft(),
                admittanceMatrix.getYtf(), admittanceMatrix.getYtt(), vf, vt);

        if (line.get("terminal1").equals(n)) {
            p = sft.getReal();
            q = sft.getImaginary();
        } else if (line.get("terminal2").equals(n)) {
            p = stf.getReal();
            q = stf.getImaginary();
        }

        if (n.startsWith("_ARGIAP7_TN1") || n.startsWith("_ARGIAP7_TN2")) {
            LOG.debug("line {}", line);
            LOG.debug("node1 {}", node1);
            LOG.debug("node2 {}", node2);
            LOG.debug("p {} q {}", p, q);
        }
        calculated = true;
        badVoltage = !voltageIsOk(angleDegrees1, angleDegrees2);
    }

    private void calculate(Complex yff, Complex yft, Complex ytf, Complex ytt, Complex vf,
            Complex vt) {
        Complex ift = yft.multiply(vt).add(yff.multiply(vf));
        sft = ift.conjugate().multiply(vf);

        Complex itf = ytf.multiply(vf).add(ytt.multiply(vt));
        stf = itf.conjugate().multiply(vt);
    }

    private boolean calcFlowT2xIsOk(boolean connected1, boolean connected2,
            double vNominal1, double vNominal2,
            double r1, double x1, double r2, double x2,
            double v1, double angleDegrees1, double v2, double angleDegrees2) {
        if (!connected1 || !connected2) {
            return false;
        }
        double baseMVA = 100.0;
        double z0Threshold = 0.00025;
        double vNominal = 1.0;
        if (!Double.isNaN(vNominal1) && vNominal1 > vNominal) {
            vNominal = vNominal1;
        }
        if (!Double.isNaN(vNominal2) && vNominal2 > vNominal) {
            vNominal = vNominal2;
        }
        double r = r1 + r2;
        double x = x1 + x2;
        if (r * baseMVA / Math.pow(vNominal, 2.0) <= z0Threshold
                && x * baseMVA / Math.pow(vNominal, 2.0) <= z0Threshold) {
            return false;
        }
        if (v1 == 0.0 || v2 == 0.0) {
            return false;
        }
        return true;
    }

    private boolean calcFlowT3xIsOk(boolean connected1, boolean connected2, boolean connected3,
            double r1, double x1, double r2, double x2,
            double r3, double x3, double v1, double angleDegrees1,
            double v2, double angleDegrees2, double v3, double angleDegrees3) {
        if (!connected1 || !connected2 || !connected3) {
            return false;
        }
        if (r1 == 0.0 && x1 == 0.0 || r2 == 0.0 && x2 == 0.0 || r3 == 0.0 && x3 == 0.0) {
            return false;
        }
        if (v1 == 0.0 || v2 == 0.0 || v3 == 0.0) {
            return false;
        }
        return true;
    }

    private boolean calcFlowLineIsOk(boolean connected, double r, double x, double vNominal1,
            double vNominal2, double v1, double angleDegrees1, double v2, double angleDegrees2) {
        if (!connected) {
            return false;
        }
        double baseMVA = 100.0;
        double z0Threshold = 0.00025;
        double vNominal = 1.0;
        if (!Double.isNaN(vNominal1) && vNominal1 > vNominal) {
            vNominal = vNominal1;
        }
        if (!Double.isNaN(vNominal2) && vNominal2 > vNominal) {
            vNominal = vNominal2;
        }
        if (r * baseMVA / Math.pow(vNominal, 2.0) <= z0Threshold
                && x * baseMVA / Math.pow(vNominal, 2.0) <= z0Threshold
                || v1 == v2 && angleDegrees1 == angleDegrees2) {
            return false;
        }
        if (v1 == 0.0 || v2 == 0.0) {
            return false;
        }
        return true;
    }

    private boolean voltageIsOk(double angleDegrees1, double angleDegrees2) {
        if (angleDegrees1 == 0.0 || angleDegrees2 == 0.0) {
            return false;
        }
        return true;
    }

    private boolean t3xVoltageIsOk(double angleDegrees1, double angleDegrees2, double angleDegrees3) {
        if (angleDegrees1 == 0.0 || angleDegrees2 == 0.0 || angleDegrees3 == 0.0) {
            return false;
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
