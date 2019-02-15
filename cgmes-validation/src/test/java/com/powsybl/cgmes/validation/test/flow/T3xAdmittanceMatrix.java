package com.powsybl.cgmes.validation.test.flow;

import java.util.Map;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

public class T3xAdmittanceMatrix {

    public T3xAdmittanceMatrix(CgmesModel cgmes, Map<String, Integer> equipmentsReport) {
        this.cgmes = cgmes;
        this.equipmentsReport = equipmentsReport;
    }

    public void calculate(PropertyBag transformer, String config) {
        double r1 = transformer.asDouble("r1");
        double x1 = transformer.asDouble("x1");
        double b1 = transformer.asDouble("b1");
        double g1 = transformer.asDouble("g1");
        int pac1 = transformer.asInt("pac1", 0);
        double ratedU1 = transformer.asDouble("ratedU1");
        double rns1 = transformer.asDouble("rns1", 0.0);
        double rsvi1 = transformer.asDouble("rsvi1", 0.0);
        double rstep1 = transformer.asDouble("rstep1", 0.0);
        double pns1 = transformer.asDouble("pns1", 0.0);
        double psvi1 = transformer.asDouble("psvi1", 0.0);
        double pstep1 = transformer.asDouble("pstep1", 0.0);
        double r2 = transformer.asDouble("r2");
        double x2 = transformer.asDouble("x2");
        double b2 = transformer.asDouble("b2");
        double g2 = transformer.asDouble("g2");
        int pac2 = transformer.asInt("pac2", 0);
        double ratedU2 = transformer.asDouble("ratedU2");
        double rns2 = transformer.asDouble("rns2", 0.0);
        double rsvi2 = transformer.asDouble("rsvi2", 0.0);
        double rstep2 = transformer.asDouble("rstep2", 0.0);
        double pns2 = transformer.asDouble("pns2", 0.0);
        double psvi2 = transformer.asDouble("psvi2", 0.0);
        double pstep2 = transformer.asDouble("pstep2", 0.0);
        double r3 = transformer.asDouble("r3");
        double x3 = transformer.asDouble("x3");
        double b3 = transformer.asDouble("b3");
        double g3 = transformer.asDouble("g3");
        int pac3 = transformer.asInt("pac3", 0);
        double ratedU3 = transformer.asDouble("ratedU3");
        double rns3 = transformer.asDouble("rns3", 0.0);
        double rsvi3 = transformer.asDouble("rsvi3", 0.0);
        double rstep3 = transformer.asDouble("rstep3", 0.0);
        double pns3 = transformer.asDouble("pns3", 0.0);
        double psvi3 = transformer.asDouble("psvi3", 0.0);
        double pstep3 = transformer.asDouble("pstep3", 0.0);
        double pwca1 = transformer.asDouble("pwca1", 0.0);
        double pwca2 = transformer.asDouble("pwca2", 0.0);
        double pwca3 = transformer.asDouble("pwca3", 0.0);
        double stepPhaseShiftIncrement1 = transformer.asDouble("stepPhaseShiftIncrement1", 0.0);
        double stepPhaseShiftIncrement2 = transformer.asDouble("stepPhaseShiftIncrement2", 0.0);
        double stepPhaseShiftIncrement3 = transformer.asDouble("stepPhaseShiftIncrement3", 0.0);
        String ptype1 = transformer.get("ptype1");
        String ptype2 = transformer.get("ptype2");
        String ptype3 = transformer.get("ptype3");
        String phaseTapChangerTable1 = transformer.get("PhaseTapChangerTable1");
        String phaseTapChangerTable2 = transformer.get("PhaseTapChangerTable2");
        String phaseTapChangerTable3 = transformer.get("PhaseTapChangerTable3");

        String configurationRatio0 = "ratio0_inside";
        String configurationRatio = "ratio_outside";
        String configurationYshunt = "yshunt_outside";
        String configurationPhaseAngleClock = "clock_off";

        if (config.contains("T3x_ratio0_inside")) {
            configurationRatio0 = "ratio0_inside";
        }
        if (config.contains("T3x_ratio0_outside")) {
            configurationRatio0 = "ratio0_outside";
        }
        if (config.contains("T3x_ratio_inside")) {
            configurationRatio = "ratio_inside";
        }
        if (config.contains("T3x_ratio_outside")) {
            configurationRatio = "ratio_outside";
        }
        if (config.contains("T3x_yshunt_inside")) {
            configurationYshunt = "yshunt_inside";
        }
        if (config.contains("T3x_yshunt_outside")) {
            configurationYshunt = "yshunt_outside";
        }
        if (config.contains("T3x_yshunt_split")) {
            configurationYshunt = "yshunt_split";
        }
        if (config.contains("T3x_clock_off")) {
            configurationPhaseAngleClock = "clock_off";
        }
        if (config.contains("T3x_clock_on_inside")) {
            configurationPhaseAngleClock = "clock_on_inside";
        }
        if (config.contains("T3x_clock_on_outside")) {
            configurationPhaseAngleClock = "clock_on_outside";
        }
        LOG.debug(" transformer {}", transformer);

        // ratio configuration

        double rtc1a = 1.0 + (rstep1 - rns1) * (rsvi1 / 100.0);
        double rtc1A = 0.0;
        double ptc1a = 1.0;
        double ptc1A = 0.0;
        if (ptype1 != null && ptype1.endsWith("asymmetrical")) {
            double dx = 1.0
                    + (pstep1 - pns1) * (psvi1 / 100.0) * Math.cos(Math.toRadians(pwca1));
            double dy = (pstep1 - pns1) * (psvi1 / 100.0) * Math.sin(Math.toRadians(pwca1));
            ptc1a = Math.hypot(dx, dy);
            ptc1A = Math.toDegrees(Math.atan2(dy, dx));
        } else if (ptype1 != null && ptype1.endsWith("symmetrical")) {
            if (stepPhaseShiftIncrement1 != 0.0) {
                ptc1a = 1.0;
                ptc1A = (pstep1 - pns1) * stepPhaseShiftIncrement1;
            } else {
                double dy = (pstep1 - pns1) * (psvi1 / 100.0);
                ptc1a = 1.0;
                ptc1A = Math.toDegrees(Math.atan2(dy, 1.0));
            }
        } else if (ptype1 != null && phaseTapChangerTable1 != null && ptype1.endsWith("tabular")) {
            PropertyBags phaseTapChangerTable = cgmes.phaseTapChangerTable(phaseTapChangerTable1);
            for (PropertyBag point : phaseTapChangerTable) {
                if (point.asInt("step") == pstep1) {
                    ptc1a = point.asDouble("ratio");
                    ptc1A = point.asDouble("angle");
                    double xc = point.asDouble("x");
                    x1 = applyCorrection(x1, xc);
                    double rc = point.asDouble("r");
                    r1 = applyCorrection(r1, rc);
                    double bc = point.asDouble("b");
                    b1 = applyCorrection(b1, bc);
                    double gc = point.asDouble("g");
                    g1 = applyCorrection(g1, gc);
                }
            }
        }
        double rtc2a = 1.0 + (rstep2 - rns2) * (rsvi2 / 100.0);
        double rtc2A = 0.0;
        double ptc2a = 1.0;
        double ptc2A = 0.0;
        if (ptype2 != null && ptype2.endsWith("asymmetrical")) {
            double dx = 1.0
                    + (pstep2 - pns2) * (psvi2 / 100.0) * Math.cos(Math.toRadians(pwca2));
            double dy = (pstep2 - pns2) * (psvi2 / 100.0) * Math.sin(Math.toRadians(pwca2));
            ptc2a = Math.hypot(dx, dy);
            ptc2A = Math.toDegrees(Math.atan2(dy, dx));
        } else if (ptype2 != null && ptype2.endsWith("symmetrical")) {
            if (stepPhaseShiftIncrement2 != 0.0) {
                ptc2a = 1.0;
                ptc2A = (pstep2 - pns2) * stepPhaseShiftIncrement2;
            } else {
                double dy = (pstep2 - pns2) * (psvi2 / 100.0);
                ptc2a = 1.0;
                ptc2A = Math.toDegrees(Math.atan2(dy, 1.0));
            }
        } else if (ptype2 != null && phaseTapChangerTable2 != null && ptype2.endsWith("tabular")) {
            PropertyBags phaseTapChangerTable = cgmes.phaseTapChangerTable(phaseTapChangerTable2);
            for (PropertyBag point : phaseTapChangerTable) {
                if (point.asInt("step") == pstep2) {
                    ptc2a = point.asDouble("ratio");
                    ptc2A = point.asDouble("angle");
                    double xc = point.asDouble("x");
                    x2 = applyCorrection(x2, xc);
                    double rc = point.asDouble("r");
                    r2 = applyCorrection(r2, rc);
                    double bc = point.asDouble("b");
                    b2 = applyCorrection(b2, bc);
                    double gc = point.asDouble("g");
                    g2 = applyCorrection(g2, gc);
                }
            }
        }
        double rtc3a = 1.0 + (rstep3 - rns3) * (rsvi3 / 100.0);
        double rtc3A = 0.0;
        double ptc3a = 1.0;
        double ptc3A = 0.0;
        if (ptype3 != null && ptype3.endsWith("asymmetrical")) {
            double dx = 1.0
                    + (pstep3 - pns3) * (psvi3 / 100.0) * Math.cos(Math.toRadians(pwca3));
            double dy = (pstep3 - pns3) * (psvi3 / 100.0) * Math.sin(Math.toRadians(pwca3));
            ptc3a = Math.hypot(dx, dy);
            ptc3A = Math.toDegrees(Math.atan2(dy, dx));
        } else if (ptype3 != null && ptype3.endsWith("symmetrical")) {
            if (stepPhaseShiftIncrement3 != 0.0) {
                ptc3a = 1.0;
                ptc3A = (pstep3 - pns3) * stepPhaseShiftIncrement3;
            } else {
                double dy = (pstep3 - pns3) * (psvi3 / 100.0);
                ptc3a = 1.0;
                ptc3A = Math.toDegrees(Math.atan2(dy, 1.0));
            }
        } else if (ptype3 != null && phaseTapChangerTable3 != null && ptype3.endsWith("tabular")) {
            PropertyBags phaseTapChangerTable = cgmes.phaseTapChangerTable(phaseTapChangerTable3);
            for (PropertyBag point : phaseTapChangerTable) {
                if (point.asInt("step") == pstep3) {
                    ptc3a = point.asDouble("ratio");
                    ptc3A = point.asDouble("angle");
                    double xc = point.asDouble("x");
                    x3 = applyCorrection(x3, xc);
                    double rc = point.asDouble("r");
                    r3 = applyCorrection(r3, rc);
                    double bc = point.asDouble("b");
                    b3 = applyCorrection(b3, bc);
                    double gc = point.asDouble("g");
                    g3 = applyCorrection(g3, gc);
                }
            }
        }
        LOG.debug(" rtc1 {} {} ptc1 {} {} rtc2 {} {} ptc2 {} {} rtc3 {} {} ptc3 {} {}", rtc1a,
                rtc1A, ptc1a, ptc1A, rtc2a, rtc2A, ptc2a, ptc2A, rtc3a, rtc3A, ptc3a, ptc3A);

        double a11 = 1.0;
        double angle11 = 0.0;
        double a12 = 1.0;
        double angle12 = 0.0;

        double a21 = 1.0;
        double angle21 = 0.0;
        double a22 = 1.0;
        double angle22 = 0.0;

        double a31 = 1.0;
        double angle31 = 0.0;
        double a32 = 1.0;
        double angle32 = 0.0;

        double ratedU0 = 1.0;
        double a110 = 1.0;
        double a120 = 1.0;

        double a210 = 1.0;
        double a220 = 1.0;

        double a310 = 1.0;
        double a320 = 1.0;
        if (configurationRatio0.equals("ratio0_inside")) {
            a110 = ratedU1 / ratedU1;
            a120 = ratedU0 / ratedU1;
            a210 = ratedU2 / ratedU2;
            a220 = ratedU0 / ratedU2;
            a310 = ratedU3 / ratedU3;
            a320 = ratedU0 / ratedU3;
        } else if (configurationRatio0.equals("ratio0_outside")) {
            a110 = ratedU1 / ratedU0;
            a120 = ratedU0 / ratedU0;
            a210 = ratedU2 / ratedU0;
            a220 = ratedU0 / ratedU0;
            a310 = ratedU3 / ratedU0;
            a320 = ratedU0 / ratedU0;
        }

        if (configurationRatio.equals("ratio_outside")) {
            a11 = rtc1a * ptc1a;
            angle11 = rtc1A + ptc1A;
            a21 = rtc2a * ptc2a;
            angle21 = rtc2A + ptc2A;
            a31 = rtc3a * ptc3a;
            angle31 = rtc3A + ptc3A;
        } else if (configurationRatio.equals("ratio_inside")) {
            a12 = rtc1a * ptc1a;
            angle12 = rtc1A + ptc1A;
            a22 = rtc2a * ptc2a;
            angle22 = rtc2A + ptc2A;
            a32 = rtc3a * ptc3a;
            angle32 = rtc3A + ptc3A;
        }

        // yshunt configuration

        Complex ysh11 = Complex.ZERO;
        Complex ysh12 = Complex.ZERO;
        Complex ysh21 = Complex.ZERO;
        Complex ysh22 = Complex.ZERO;
        Complex ysh31 = Complex.ZERO;
        Complex ysh32 = Complex.ZERO;
        if (configurationYshunt.equals("yshunt_outside")) {
            ysh11 = ysh11.add(new Complex(g1, b1));
            ysh21 = ysh21.add(new Complex(g2, b2));
            ysh31 = ysh31.add(new Complex(g3, b3));
        } else if (configurationYshunt.equals("yshunt_inside")) {
            ysh12 = ysh12.add(new Complex(g1, b1));
            ysh22 = ysh22.add(new Complex(g2, b2));
            ysh32 = ysh32.add(new Complex(g3, b3));
        } else if (configurationYshunt.equals("yshunt_split")) {
            ysh11 = ysh11.add(new Complex(g1 * 0.5, b1 * 0.5));
            ysh21 = ysh21.add(new Complex(g2 * 0.5, b2 * 0.5));
            ysh31 = ysh31.add(new Complex(g3 * 0.5, b3 * 0.5));
            ysh12 = ysh12.add(new Complex(g1 * 0.5, b1 * 0.5));
            ysh22 = ysh22.add(new Complex(g2 * 0.5, b2 * 0.5));
            ysh32 = ysh32.add(new Complex(g3 * 0.5, b3 * 0.5));
        }

        String code1 = code(transformer, ysh11, ysh12, a11, angle11, a12, angle12, 1);
        String code2 = code(transformer, ysh21, ysh22, a21, angle21, a22, angle22, 2);
        String code3 = code(transformer, ysh31, ysh32, a31, angle31, a32, angle32, 3);
        String code = code1 + "." + code2 + "." + code3;
        transformer.put("code", code);
        Integer total = equipmentsReport.get(code);
        if (total == null) {
            total = new Integer(0);
        }
        equipmentsReport.put(code, total + 1);

        // add structural ratio after coding
        a11 *= a110;
        a12 *= a120;
        a21 *= a210;
        a22 *= a220;
        a31 *= a310;
        a32 *= a320;

        // phaseAngleClock configuration

        if (configurationPhaseAngleClock.equals("clock_on_inside")) {
            if (pac1 != 0) {
                angle12 += getPhaseAngleClock(pac1);
            }
            if (pac2 != 0) {
                angle22 += getPhaseAngleClock(pac2);
            }
            if (pac3 != 0) {
                angle32 += getPhaseAngleClock(pac3);
            }
        } else if (configurationPhaseAngleClock.equals("clock_on_outside")) {
            if (pac1 != 0) {
                angle11 += getPhaseAngleClock(pac1);
            }
            if (pac2 != 0) {
                angle21 += getPhaseAngleClock(pac2);
            }
            if (pac3 != 0) {
                angle31 += getPhaseAngleClock(pac3);
            }
        }

        angle11 = Math.toRadians(angle11);
        angle12 = Math.toRadians(angle12);
        angle21 = Math.toRadians(angle21);
        angle22 = Math.toRadians(angle22);
        angle31 = Math.toRadians(angle31);
        angle32 = Math.toRadians(angle32);
        Complex aA11 = new Complex(a11 * Math.cos(angle11), a11 * Math.sin(angle11));
        Complex aA12 = new Complex(a12 * Math.cos(angle12), a12 * Math.sin(angle12));
        Complex aA21 = new Complex(a21 * Math.cos(angle21), a21 * Math.sin(angle21));
        Complex aA22 = new Complex(a22 * Math.cos(angle22), a22 * Math.sin(angle22));
        Complex aA31 = new Complex(a31 * Math.cos(angle31), a31 * Math.sin(angle31));
        Complex aA32 = new Complex(a32 * Math.cos(angle32), a32 * Math.sin(angle32));
        LOG.debug(" aA11 {} aA12 {} aA21 {} aA22 {} aA31 {} aA32 {}", aA11, aA12, aA21, aA22,
                aA31, aA32);

        Complex z1 = new Complex(r1, x1);
        yff1 = z1.reciprocal().add(ysh11).divide(aA11.conjugate().multiply(aA11));
        yft1 = z1.reciprocal().negate().divide(aA11.conjugate().multiply(aA12));
        ytf1 = z1.reciprocal().negate().divide(aA12.conjugate().multiply(aA11));
        ytt1 = z1.reciprocal().add(ysh12).divide(aA12.conjugate().multiply(aA12));

        Complex z2 = new Complex(r2, x2);
        yff2 = z2.reciprocal().add(ysh21).divide(aA21.conjugate().multiply(aA21));
        yft2 = z2.reciprocal().negate().divide(aA21.conjugate().multiply(aA22));
        ytf2 = z2.reciprocal().negate().divide(aA22.conjugate().multiply(aA21));
        ytt2 = z2.reciprocal().add(ysh22).divide(aA22.conjugate().multiply(aA22));

        Complex z3 = new Complex(r3, x3);
        yff3 = z3.reciprocal().add(ysh31).divide(aA31.conjugate().multiply(aA31));
        yft3 = z3.reciprocal().negate().divide(aA31.conjugate().multiply(aA32));
        ytf3 = z3.reciprocal().negate().divide(aA32.conjugate().multiply(aA31));
        ytt3 = z3.reciprocal().add(ysh32).divide(aA32.conjugate().multiply(aA32));
    }

    private double getPhaseAngleClock(int phaseAngleClock) {
        double phaseAngleClockDegree = 0.0;
        phaseAngleClockDegree += phaseAngleClock * 30.0;
        phaseAngleClockDegree = Math.IEEEremainder(phaseAngleClockDegree, 360.0);
        if (phaseAngleClockDegree > 180.0) {
            phaseAngleClockDegree -= 360.0;
        }
        return phaseAngleClockDegree;
    }

    private String code(PropertyBag transformer, Complex ysh1, Complex ysh2, double a1,
            double angle1, double a2, double angle2, int end) {
        StringBuilder code = new StringBuilder();
        if (a1 == 1.0) {
            if (transformer.containsKey("rstep" + end)) {
                if (!Double.isNaN(transformer.asDouble("rhs" + end)) && transformer.asDouble("rls" + end, 0.0) == transformer.asDouble("rhs" + end, 0.0)) {
                    code.append("*");
                } else {
                    code.append("r");
                }
            } else {
                code.append("_");
            }
        } else {
            if (!Double.isNaN(transformer.asDouble("rhs" + end)) && transformer.asDouble("rls" + end, 0.0) == transformer.asDouble("rhs" + end, 0.0)) {
                code.append("*");
            } else {
                code.append("R");
            }
        }
        if (angle1 == 0.0) {
            if (transformer.containsKey("pstep" + end)) {
                if (!Double.isNaN(transformer.asDouble("phs" + end)) && transformer.asDouble("pls" + end, 0.0) == transformer.asDouble("phs" + end, 0.0)) {
                    code.append("*");
                } else {
                    code.append("p");
                }
            } else {
                code.append("_");
            }
        } else {
            if (!Double.isNaN(transformer.asDouble("phs" + end)) && transformer.asDouble("pls" + end, 0.0) == transformer.asDouble("phs" + end, 0.0)) {
                code.append("*");
            } else {
                code.append("P");
            }
        }
        if (ysh1.equals(Complex.ZERO)) {
            code.append("N");
        } else {
            code.append("Y");
        }
        if (ysh2.equals(Complex.ZERO)) {
            code.append("N");
        } else {
            code.append("Y");
        }
        if (a2 == 1.0) {
            if (transformer.containsKey("rstep" + end)) {
                if (!Double.isNaN(transformer.asDouble("rhs" + end)) && transformer.asDouble("rls" + end, 0.0) == transformer.asDouble("rhs" + end, 0.0)) {
                    code.append("*");
                } else {
                    code.append("r");
                }
            } else {
                code.append("_");
            }
        } else {
            if (!Double.isNaN(transformer.asDouble("rhs" + end)) && transformer.asDouble("rls" + end, 0.0) == transformer.asDouble("rhs" + end, 0.0)) {
                code.append("*");
            } else {
                code.append("R");
            }
        }
        if (angle2 == 0.0) {
            if (transformer.containsKey("pstep" + end)) {
                if (!Double.isNaN(transformer.asDouble("phs" + end)) && transformer.asDouble("pls" + end, 0.0) == transformer.asDouble("phs" + end, 0.0)) {
                    code.append("*");
                } else {
                    code.append("p");
                }
            } else {
                code.append("_");
            }
        } else {
            if (!Double.isNaN(transformer.asDouble("phs" + end)) && transformer.asDouble("pls" + end, 0.0) == transformer.asDouble("phs" + end, 0.0)) {
                code.append("*");
            } else {
                code.append("P");
            }
        }
        return code.toString();
    }

    private double applyCorrection(double v, double vc) {
        return v * (1.0 + vc / 100.0);
    }

    public Complex getYff1() {
        return yff1;
    }

    public Complex getYft1() {
        return yft1;
    }

    public Complex getYtf1() {
        return ytf1;
    }

    public Complex getYtt1() {
        return ytt1;
    }

    public Complex getYff2() {
        return yff2;
    }

    public Complex getYft2() {
        return yft2;
    }

    public Complex getYtf2() {
        return ytf2;
    }

    public Complex getYtt2() {
        return ytt2;
    }

    public Complex getYff3() {
        return yff3;
    }

    public Complex getYft3() {
        return yft3;
    }

    public Complex getYtf3() {
        return ytf3;
    }

    public Complex getYtt3() {
        return ytt3;
    }

    protected CgmesModel           cgmes;
    protected Map<String, Integer> equipmentsReport;
    protected Complex              yff1;
    protected Complex              yft1;
    protected Complex              ytf1;
    protected Complex              ytt1;
    protected Complex              yff2;
    protected Complex              yft2;
    protected Complex              ytf2;
    protected Complex              ytt2;
    protected Complex              yff3;
    protected Complex              yft3;
    protected Complex              ytf3;
    protected Complex              ytt3;

    private static final Logger    LOG = LoggerFactory
            .getLogger(T3xAdmittanceMatrix.class);
}
