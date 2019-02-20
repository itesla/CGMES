package com.powsybl.cgmes.validation.test.flow;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;

public class T3xAdmittanceMatrix extends AdmittanceMatrix3 {

    public T3xAdmittanceMatrix(CgmesModel cgmes) {
        super(cgmes);
    }

    public void calculate(PropertyBag transformer, String config) {
        double[] res;
        readParameters(transformer);

        LOG.debug(" transformer {}", transformer);

        // ratio configuration
        res = getRatioConfiguration(rstep1, rns1, rsvi1);
        double rtc1a = res[0];
        double rtc1A = res[1];
        if (ptype1 != null && phaseTapChangerTable1 != null && ptype1.endsWith("tabular")) {
            res = getTabularPhaseConfiguration(pstep1, phaseTapChangerTable1);
            double xc = res[2];
            x1 = applyCorrection(x1, xc);
            double rc = res[3];
            r1 = applyCorrection(r1, rc);
            double bc = res[4];
            b1 = applyCorrection(b1, bc);
            double gc = res[5];
            g1 = applyCorrection(g1, gc);
        } else {
            res = getPhaseConfiguration(ptype1, pstep1, pns1, psvi1, pwca1,
                    stepPhaseShiftIncrement1);
        }
        double ptc1a = res[0];
        double ptc1A = res[1];

        res = getRatioConfiguration(rstep2, rns2, rsvi2);
        double rtc2a = res[0];
        double rtc2A = res[1];
        if (ptype2 != null && phaseTapChangerTable2 != null && ptype2.endsWith("tabular")) {
            res = getTabularPhaseConfiguration(pstep2, phaseTapChangerTable2);
            double xc = res[2];
            x2 = applyCorrection(x2, xc);
            double rc = res[3];
            r2 = applyCorrection(r2, rc);
            double bc = res[4];
            b2 = applyCorrection(b2, bc);
            double gc = res[5];
            g2 = applyCorrection(g2, gc);
        } else {
            res = getPhaseConfiguration(ptype2, pstep2, pns2, psvi2, pwca2,
                    stepPhaseShiftIncrement2);
        }
        double ptc2a = res[0];
        double ptc2A = res[1];

        res = getRatioConfiguration(rstep3, rns3, rsvi3);
        double rtc3a = res[0];
        double rtc3A = res[1];
        if (ptype3 != null && phaseTapChangerTable3 != null && ptype3.endsWith("tabular")) {
            res = getTabularPhaseConfiguration(pstep3, phaseTapChangerTable3);
            double xc = res[2];
            x3 = applyCorrection(x3, xc);
            double rc = res[3];
            r3 = applyCorrection(r3, rc);
            double bc = res[4];
            b3 = applyCorrection(b3, bc);
            double gc = res[5];
            g3 = applyCorrection(g3, gc);
        } else {
            res = getPhaseConfiguration(ptype2, pstep2, pns2, psvi2, pwca2,
                    stepPhaseShiftIncrement2);
        }
        double ptc3a = res[0];
        double ptc3A = res[1];
        LOG.debug(" rtc1 {} {} ptc1 {} {} rtc2 {} {} ptc2 {} {} rtc3 {} {} ptc3 {} {}", rtc1a,
                rtc1A, ptc1a, ptc1A, rtc2a, rtc2A, ptc2a, ptc2A, rtc3a, rtc3A, ptc3a, ptc3A);

        double ratedU0 = 1.0;
        res = getRatio0(config, ratedU0, ratedU1, ratedU2, ratedU3);
        double a011 = res[0];
        double a012 = res[1];
        double a021 = res[2];
        double a022 = res[3];
        double a031 = res[4];
        double a032 = res[5];

        res = getRatio(config, rtc1a, ptc1a, rtc2a, ptc2a, rtc3a, ptc3a,
                rtc1A, ptc1A, rtc2A, ptc2A, rtc3A, ptc3A);
        double a11 = res[0];
        double angle11 = res[1];
        double a12 = res[2];
        double angle12 = res[3];
        double a21 = res[4];
        double angle21 = res[5];
        double a22 = res[6];
        double angle22 = res[7];
        double a31 = res[8];
        double angle31 = res[9];
        double a32 = res[10];
        double angle32 = res[11];

        // yshunt configuration
        res = getYShunt(config);
        Complex ysh11 = new Complex(res[0], res[1]);
        Complex ysh12 = new Complex(res[2], res[3]);
        Complex ysh21 = new Complex(res[4], res[5]);
        Complex ysh22 = new Complex(res[6], res[7]);
        Complex ysh31 = new Complex(res[8], res[9]);
        Complex ysh32 = new Complex(res[10], res[11]);

        setCode(transformer, ysh11, ysh12, a11, angle11, a12, angle12,
                ysh21, ysh22, a21, angle21, a22, angle22,
                ysh31, ysh32, a31, angle31, a32, angle32);

        // add structural ratio after coding
        a11 *= a011;
        a12 *= a012;
        a21 *= a021;
        a22 *= a022;
        a31 *= a031;
        a32 *= a032;

        // phaseAngleClock configuration
        res = getPhaseAngleClock(config);
        angle11 += res[0];
        angle12 += res[1];
        angle21 += res[2];
        angle22 += res[3];
        angle31 += res[4];
        angle32 += res[5];

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
        yff = z1.reciprocal().add(ysh11).divide(aA11.conjugate().multiply(aA11));
        yft = z1.reciprocal().negate().divide(aA11.conjugate().multiply(aA12));
        ytf = z1.reciprocal().negate().divide(aA12.conjugate().multiply(aA11));
        ytt = z1.reciprocal().add(ysh12).divide(aA12.conjugate().multiply(aA12));

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

    private double[] getPhaseAngleClock(String config) {
        String configurationPhaseAngleClock = "clock_off";

        if (config.contains("T3x_clock_off")) {
            configurationPhaseAngleClock = "clock_off";
        }
        if (config.contains("T3x_clock_on_inside")) {
            configurationPhaseAngleClock = "clock_on_inside";
        }
        if (config.contains("T3x_clock_on_outside")) {
            configurationPhaseAngleClock = "clock_on_outside";
        }

        double angle11 = 0.0;
        double angle12 = 0.0;
        double angle21 = 0.0;
        double angle22 = 0.0;
        double angle31 = 0.0;
        double angle32 = 0.0;
        if (configurationPhaseAngleClock.equals("clock_on_inside")) {
            if (pac1 != 0) {
                angle12 = getPhaseAngleClock(pac1);
            }
            if (pac2 != 0) {
                angle22 = getPhaseAngleClock(pac2);
            }
            if (pac3 != 0) {
                angle32 = getPhaseAngleClock(pac3);
            }
        } else if (configurationPhaseAngleClock.equals("clock_on_outside")) {
            if (pac1 != 0) {
                angle11 = getPhaseAngleClock(pac1);
            }
            if (pac2 != 0) {
                angle21 = getPhaseAngleClock(pac2);
            }
            if (pac3 != 0) {
                angle31 = getPhaseAngleClock(pac3);
            }
        }
        return new double[] {angle11, angle12, angle21, angle22, angle31, angle32};
    }

    private double[] getYShunt(String config) {
        String configurationYshunt = "yshunt_outside";
        if (config.contains("T3x_yshunt_inside")) {
            configurationYshunt = "yshunt_inside";
        }
        if (config.contains("T3x_yshunt_outside")) {
            configurationYshunt = "yshunt_outside";
        }
        if (config.contains("T3x_yshunt_split")) {
            configurationYshunt = "yshunt_split";
        }

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

        return new double[] {ysh11.getReal(), ysh11.getImaginary(), ysh12.getReal(),
                ysh12.getImaginary(),
                ysh21.getReal(), ysh21.getImaginary(), ysh22.getReal(), ysh22.getImaginary(),
                ysh31.getReal(), ysh31.getImaginary(), ysh32.getReal(), ysh32.getImaginary()};
    }

    private void setCode(PropertyBag transformer, Complex ysh11, Complex ysh12, double a11,
            double angle11, double a12, double angle12, Complex ysh21, Complex ysh22, double a21,
            double angle21, double a22, double angle22, Complex ysh31, Complex ysh32, double a31,
            double angle31, double a32, double angle32) {
        double rstep = transformer.asDouble("rstep1");
        double rls = transformer.asDouble("rls1");
        double rhs = transformer.asDouble("rhs1");
        double pstep = transformer.asDouble("pstep1");
        double pls = transformer.asDouble("pls1");
        double phs = transformer.asDouble("phs1");
        String code1 = endCode(transformer, ysh11, ysh12, a11, angle11, a12, angle12, rstep, rls,
                rhs, pstep, pls, phs, rstep, rls, rhs, pstep, pls, phs);
        rstep = transformer.asDouble("rstep2");
        rls = transformer.asDouble("rls2");
        rhs = transformer.asDouble("rhs2");
        pstep = transformer.asDouble("pstep2");
        pls = transformer.asDouble("pls2");
        phs = transformer.asDouble("phs2");
        String code2 = endCode(transformer, ysh21, ysh22, a21, angle21, a22, angle22, rstep, rls,
                rhs, pstep, pls, phs, rstep, rls, rhs, pstep, pls, phs);
        rstep = transformer.asDouble("rstep3");
        rls = transformer.asDouble("rls3");
        rhs = transformer.asDouble("rhs3");
        pstep = transformer.asDouble("pstep3");
        pls = transformer.asDouble("pls3");
        phs = transformer.asDouble("phs3");
        String code3 = endCode(transformer, ysh31, ysh32, a31, angle31, a32, angle32, rstep, rls,
                rhs, pstep, pls, phs, rstep, rls, rhs, pstep, pls, phs);

        setModelCode(code1 + "." + code2 + "." + code3);
    }

    private double[] getRatio(String config, double rtc1a, double ptc1a, double rtc2a, double ptc2a,
            double rtc3a, double ptc3a, double rtc1A, double ptc1A, double rtc2A, double ptc2A,
            double rtc3A, double ptc3A) {
        String configurationRatio = "ratio_outside";
        if (config.contains("T3x_ratio_inside")) {
            configurationRatio = "ratio_inside";
        }
        if (config.contains("T3x_ratio_outside")) {
            configurationRatio = "ratio_outside";
        }

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
        return new double[] {a11, angle11, a12, angle12, a21, angle21, a22, angle22, a31, angle31,
            a32, angle32};
    }

    private double[] getRatio0(String config, double ratedU0, double ratedU1, double ratedU2,
            double ratedU3) {
        String configurationRatio0 = "ratio0_inside";
        if (config.contains("T3x_ratio0_inside")) {
            configurationRatio0 = "ratio0_inside";
        }
        if (config.contains("T3x_ratio0_outside")) {
            configurationRatio0 = "ratio0_outside";
        }

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
        return new double[] {a110, a120, a210, a220, a310, a320};
    }

    private void readParameters(PropertyBag transformer) {
        r1 = transformer.asDouble("r1");
        x1 = transformer.asDouble("x1");
        b1 = transformer.asDouble("b1");
        g1 = transformer.asDouble("g1");
        pac1 = transformer.asInt("pac1", 0);
        ratedU1 = transformer.asDouble("ratedU1");
        rns1 = transformer.asDouble("rns1", 0.0);
        rsvi1 = transformer.asDouble("rsvi1", 0.0);
        rstep1 = transformer.asDouble("rstep1", 0.0);
        pns1 = transformer.asDouble("pns1", 0.0);
        psvi1 = transformer.asDouble("psvi1", 0.0);
        pstep1 = transformer.asDouble("pstep1", 0.0);
        r2 = transformer.asDouble("r2");
        x2 = transformer.asDouble("x2");
        b2 = transformer.asDouble("b2");
        g2 = transformer.asDouble("g2");
        pac2 = transformer.asInt("pac2", 0);
        ratedU2 = transformer.asDouble("ratedU2");
        rns2 = transformer.asDouble("rns2", 0.0);
        rsvi2 = transformer.asDouble("rsvi2", 0.0);
        rstep2 = transformer.asDouble("rstep2", 0.0);
        pns2 = transformer.asDouble("pns2", 0.0);
        psvi2 = transformer.asDouble("psvi2", 0.0);
        pstep2 = transformer.asDouble("pstep2", 0.0);
        r3 = transformer.asDouble("r3");
        x3 = transformer.asDouble("x3");
        b3 = transformer.asDouble("b3");
        g3 = transformer.asDouble("g3");
        pac3 = transformer.asInt("pac3", 0);
        ratedU3 = transformer.asDouble("ratedU3");
        rns3 = transformer.asDouble("rns3", 0.0);
        rsvi3 = transformer.asDouble("rsvi3", 0.0);
        rstep3 = transformer.asDouble("rstep3", 0.0);
        pns3 = transformer.asDouble("pns3", 0.0);
        psvi3 = transformer.asDouble("psvi3", 0.0);
        pstep3 = transformer.asDouble("pstep3", 0.0);
        pwca1 = transformer.asDouble("pwca1", 0.0);
        pwca2 = transformer.asDouble("pwca2", 0.0);
        pwca3 = transformer.asDouble("pwca3", 0.0);
        stepPhaseShiftIncrement1 = transformer.asDouble("stepPhaseShiftIncrement1", 0.0);
        stepPhaseShiftIncrement2 = transformer.asDouble("stepPhaseShiftIncrement2", 0.0);
        stepPhaseShiftIncrement3 = transformer.asDouble("stepPhaseShiftIncrement3", 0.0);
        ptype1 = transformer.get("ptype1");
        ptype2 = transformer.get("ptype2");
        ptype3 = transformer.get("ptype3");
        phaseTapChangerTable1 = transformer.get("PhaseTapChangerTable1");
        phaseTapChangerTable2 = transformer.get("PhaseTapChangerTable2");
        phaseTapChangerTable3 = transformer.get("PhaseTapChangerTable3");
    }

    private double r1;
    private double x1;
    private double b1;
    private double g1;
    private int    pac1;
    private double ratedU1;
    private double rns1;
    private double rsvi1;
    private double rstep1;
    private double pns1;
    private double psvi1;
    private double pstep1;
    private double r2;
    private double x2;
    private double b2;
    private double g2;
    private int    pac2;
    private double ratedU2;
    private double rns2;
    private double rsvi2;
    private double rstep2;
    private double pns2;
    private double psvi2;
    private double pstep2;
    private double r3;
    private double x3;
    private double b3;
    private double g3;
    private int    pac3;
    private double ratedU3;
    private double rns3;
    private double rsvi3;
    private double rstep3;
    private double pns3;
    private double psvi3;
    private double pstep3;
    private double pwca1;
    private double pwca2;
    private double pwca3;
    private double stepPhaseShiftIncrement1;
    private double stepPhaseShiftIncrement2;
    private double stepPhaseShiftIncrement3;
    private String ptype1;
    private String ptype2;
    private String ptype3;
    private String phaseTapChangerTable1;
    private String phaseTapChangerTable2;
    private String phaseTapChangerTable3;
}
