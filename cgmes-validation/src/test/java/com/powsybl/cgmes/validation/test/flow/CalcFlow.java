package com.powsybl.cgmes.validation.test.flow;

import java.util.Map;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.triplestore.api.PropertyBag;

public class CalcFlow {

    public CalcFlow(PrepareModel inputModel, Map<String, Integer> equipmentsReport) {
        this.inputModel = inputModel;
        this.equipmentsReport = equipmentsReport;
    }

    public void calcFlowT2x(String n, PropertyBag node1, PropertyBag node2,
            PropertyBag transformer, String config) {
        if (n.startsWith("_f7d16772")) {
            LOG.debug("From {}  {} To {}  {}", node1.asDouble("v"), node1.asDouble("angle"),
                    node2.asDouble("v"), node2.asDouble("angle"));
        }
        double r1 = transformer.asDouble("r1");
        double x1 = transformer.asDouble("x1");
        double r2 = transformer.asDouble("r2");
        double x2 = transformer.asDouble("x2");
        if (r1 == 0.0 && x1 == 0.0 && r2 == 0.0 && x2 == 0.0) {
            transformer.put("p", Double.toString(0.0));
            transformer.put("q", Double.toString(0.0));
            transformer.put("z0Line", "true");
            return;
        }

        double v1 = node1.asDouble("v");
        double angle1 = Math.toRadians(node1.asDouble("angle"));
        double v2 = node2.asDouble("v");
        double angle2 = Math.toRadians(node2.asDouble("angle"));
        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        T2xAdmittanceMatrix admittanceMatrix = new T2xAdmittanceMatrix(inputModel.getCgmes(),
                equipmentsReport);
        admittanceMatrix.calculate(transformer, config);

        calculate(admittanceMatrix.getYff(), admittanceMatrix.getYft(),
                admittanceMatrix.getYtf(), admittanceMatrix.getYtt(), vf, vt);

        Complex res = Complex.ZERO;
        if (transformer.get("terminal1").equals(n)) {
            res = getSft();
        } else if (transformer.get("terminal2").equals(n)) {
            res = getStf();
        }
        if (n.startsWith("_f7d16772")) {
            LOG.debug(" transformer {}", transformer);
            LOG.debug("t2x {} {} node {}", res.getReal(), res.getImaginary(), n);
        }
        transformer.put("p", Double.toString(res.getReal()));
        transformer.put("q", Double.toString(res.getImaginary()));
    }

    public void calcFlowT3x(String n, PropertyBag node1, PropertyBag node2, PropertyBag node3,
            PropertyBag transformer, String config) {
        if (n.startsWith("_f7d16772")) {
            LOG.debug("End1 {}  {} End2 {}  {} End3 {} {}", node1.asDouble("v"),
                    node1.asDouble("angle"),
                    node2.asDouble("v"), node2.asDouble("angle"), node3.asDouble("v"),
                    node3.asDouble("angle"));
        }
        double r1 = transformer.asDouble("r1");
        double x1 = transformer.asDouble("x1");
        double r2 = transformer.asDouble("r2");
        double x2 = transformer.asDouble("x2");
        double r3 = transformer.asDouble("r3");
        double x3 = transformer.asDouble("x3");
        if (r1 == 0.0 && x1 == 0.0 || r2 == 0.0 && x2 == 0.0 || r3 == 0.0 && x3 == 0.0) {
            transformer.put("p", Double.toString(0.0));
            transformer.put("q", Double.toString(0.0));
            transformer.put("z0Line", "true");
            return;
        }

        double v1 = node1.asDouble("v");
        double angle1 = Math.toRadians(node1.asDouble("angle"));
        double v2 = node2.asDouble("v");
        double angle2 = Math.toRadians(node2.asDouble("angle"));
        double v3 = node3.asDouble("v");
        double angle3 = Math.toRadians(node3.asDouble("angle"));
        Complex vf1 = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vf2 = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));
        Complex vf3 = new Complex(v3 * Math.cos(angle3), v3 * Math.sin(angle3));

        T3xAdmittanceMatrix admittanceMatrix = new T3xAdmittanceMatrix(inputModel.getCgmes(),
                equipmentsReport);
        admittanceMatrix.calculate(transformer, config);
        Complex v0 = admittanceMatrix.getYtf1().multiply(vf1)
                .add(admittanceMatrix.getYtf2().multiply(vf2))
                .add(admittanceMatrix.getYtf3().multiply(vf3))
                .negate().divide(admittanceMatrix.getYtt1().add(admittanceMatrix.getYtt2())
                        .add(admittanceMatrix.getYtt3()));
        LOG.debug("V0 ------> {} {}", v0.abs(), v0.getArgument());

        Complex res = Complex.ZERO;
        if (transformer.get("terminal1").equals(n)) {
            calculate(admittanceMatrix.getYff1(), admittanceMatrix.getYft1(),
                    admittanceMatrix.getYtf1(), admittanceMatrix.getYtt1(), vf1, v0);
            res = getSft();
        } else if (transformer.get("terminal2").equals(n)) {
            calculate(admittanceMatrix.getYff2(), admittanceMatrix.getYft2(),
                    admittanceMatrix.getYtf2(), admittanceMatrix.getYtt2(), vf2, v0);
            res = getSft();
        } else if (transformer.get("terminal3").equals(n)) {
            calculate(admittanceMatrix.getYff3(), admittanceMatrix.getYft3(),
                    admittanceMatrix.getYtf3(), admittanceMatrix.getYtt3(), vf3, v0);
            res = getSft();
        }
        if (n.startsWith("_f7d16772")) {
            LOG.debug("trafo3D {}", transformer);
            LOG.debug("trafo3D {} {} node {}", res.getReal(), res.getImaginary(), n);
        }
        transformer.put("p", Double.toString(res.getReal()));
        transformer.put("q", Double.toString(res.getImaginary()));
    }

    public void calcFlowLine(String n, PropertyBag node1, PropertyBag node2, PropertyBag line,
            String config) {
        if (n.startsWith("_f7d16772")) {
            LOG.debug("From {}  {} To {}  {}", node1.asDouble("v"), node1.asDouble("angle"),
                    node2.asDouble("v"), node2.asDouble("angle"));
        }
        double v1 = node1.asDouble("v");
        double angle1 = Math.toRadians(node1.asDouble("angle"));
        double v2 = node2.asDouble("v");
        double angle2 = Math.toRadians(node2.asDouble("angle"));
        if (angle1 == 0.0) {
            v1 = v2;
            angle1 = angle2;
            line.put("partial", "true");
        } else if (angle2 == 0.0) {
            v2 = v1;
            angle2 = angle1;
            line.put("partial", "true");
        }

        double r = line.asDouble("r");
        double x = line.asDouble("x");
        if (r < 0.0001 && x < 0.0001 || v1 == v2 && angle1 == angle2) {
            line.put("p", Double.toString(0.0));
            line.put("q", Double.toString(0.0));
            line.put("z0Line", "true");
            return;
        }

        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));
        LineAdmittanceMatrix admittanceMatrix = new LineAdmittanceMatrix(inputModel.getCgmes(),
                equipmentsReport);
        admittanceMatrix.calculate(line, config);

        calculate(admittanceMatrix.getYff(), admittanceMatrix.getYft(),
                admittanceMatrix.getYtf(), admittanceMatrix.getYtt(), vf, vt);

        Complex res = Complex.ZERO;
        if (line.get("terminal1").equals(n)) {
            res = getSft();
        } else if (line.get("terminal2").equals(n)) {
            res = getStf();
        }
        if (n.startsWith("_f7d16772")) {
            LOG.debug("line {}", line);
            LOG.debug("line {} {} node {}", res.getReal(), res.getImaginary(), n);
        }
        line.put("p", Double.toString(res.getReal()));
        line.put("q", Double.toString(res.getImaginary()));
    }

    private void calculate(Complex yff, Complex yft, Complex ytf, Complex ytt, Complex vf,
            Complex vt) {
        Complex ift = yft.multiply(vt).add(yff.multiply(vf));
        sft = ift.conjugate().multiply(vf);

        Complex itf = ytf.multiply(vf).add(ytt.multiply(vt));
        stf = itf.conjugate().multiply(vt);
    }

    public Complex getSft() {
        return sft;
    }

    public Complex getStf() {
        return stf;
    }

    private Complex              sft;
    private Complex              stf;
    private PrepareModel    inputModel;
    private Map<String, Integer> equipmentsReport;
    private static final Logger  LOG = LoggerFactory
            .getLogger(CalcFlow.class);
}
