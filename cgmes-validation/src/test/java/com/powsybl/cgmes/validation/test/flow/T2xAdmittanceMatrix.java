package com.powsybl.cgmes.validation.test.flow;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;

public class T2xAdmittanceMatrix extends AdmittanceMatrix {

    public T2xAdmittanceMatrix(CgmesModel cgmes) {
        super(cgmes);
    }

    public void calculate(PropertyBag transformer, String config) {
        readT2xParameters(transformer);

        LOG.debug(" transformer {}", transformer);

        // ratio configuration
        double[] res;
        res = getRatioTapChangerData(rstep1, rns1, rsvi1);
        double rtc1a = res[0];
        double rtc1A = res[1];

        boolean pct1TabularDifferentRatios = false;
        boolean pct1TabularDifferentAngles = false;
        boolean pct1AsymmetricalDifferentRatios = false;
        double ptc1a = 1.0;
        double ptc1A = 0.0;
        if (phaseTapChangerIsTabular(ptype1, phaseTapChangerTable1)) {
            res = getTabularPhaseTapChangerData(pstep1, phaseTapChangerTable1);
            ptc1a = res[0];
            ptc1A = res[1];
            double xc = res[2];
            double rc = res[3];
            double bc = res[4];
            double gc = res[5];

            t2xParametersCorrectionEnd1(xc, rc, bc, gc);

            pct1TabularDifferentRatios = getTabularPhaseTapChangerDifferentRatios(phaseTapChangerTable1);
            pct1TabularDifferentAngles = getTabularPhaseTapChangerDifferentAngles(phaseTapChangerTable1);

        } else if (phaseTapChangerIsAsymmetrical(ptype1)) {
            res = getAsymmetricalPhaseTapChangerData(ptype1, pstep1, pns1, psvi1, pwca1);
            ptc1a = res[0];
            ptc1A = res[1];

            pct1AsymmetricalDifferentRatios = getAsymmetricalPhaseTapChangerDifferentRatios(psvi1, pls1, phs1);

        } else if (phaseTapChangerIsSymmetrical(ptype1)) {
            res = getSymmetricalPhaseTapChangerData(ptype1, pstep1, pns1, psvi1,
                    stepPhaseShiftIncrement1);
            ptc1a = res[0];
            ptc1A = res[1];
        }

        res = getRatioTapChangerData(rstep2, rns2, rsvi2);
        double rtc2a = res[0];
        double rtc2A = res[1];

        boolean pct2TabularDifferentRatios = false;
        boolean pct2TabularDifferentAngles = false;
        boolean pct2AsymmetricalDifferentRatios = false;
        double ptc2a = 1.0;
        double ptc2A = 0.0;
        if (phaseTapChangerIsTabular(ptype2, phaseTapChangerTable2)) {
            res = getTabularPhaseTapChangerData(pstep2, phaseTapChangerTable2);
            ptc2a = res[0];
            ptc2A = res[1];
            ptc2A = getT2xPtc2Negate(config, ptc2A);

            double xc = res[2];
            double rc = res[3];
            double bc = res[4];
            double gc = res[5];

            t2xParametersCorrectionEnd2(xc, rc, bc, gc);

            pct2TabularDifferentRatios = getTabularPhaseTapChangerDifferentRatios(phaseTapChangerTable2);
            pct2TabularDifferentAngles = getTabularPhaseTapChangerDifferentAngles(phaseTapChangerTable2);

        } else if (phaseTapChangerIsAsymmetrical(ptype2)) {
            res = getAsymmetricalPhaseTapChangerData(ptype2, pstep2, pns2, psvi2, pwca2);
            ptc2a = res[0];
            ptc2A = res[1];

            pct2AsymmetricalDifferentRatios = getAsymmetricalPhaseTapChangerDifferentRatios(psvi2, pls2, phs2);

        } else if (phaseTapChangerIsSymmetrical(ptype2)) {
            res = getSymmetricalPhaseTapChangerData(ptype2, pstep2, pns2, psvi2,
                    stepPhaseShiftIncrement2);
            ptc2a = res[0];
            ptc2A = res[1];
        }

        res = getT2xRatioPhase(config, rtc1a, ptc1a, rtc2a, ptc2a, rtc1A, ptc1A, rtc2A, ptc2A);
        double a1 = res[0];
        double angle1 = res[1];
        double a2 = res[2];
        double angle2 = res[3];

        // yshunt configuration
        res = getT2xYShunt(config);
        Complex ysh1 = new Complex(res[0], res[1]);
        Complex ysh2 = new Complex(res[2], res[3]);

        setT2xModelCode(ysh1, ysh2, a1, angle1, a2, angle2,
            pct1TabularDifferentRatios, pct1AsymmetricalDifferentRatios, pct1TabularDifferentAngles,
            pct2TabularDifferentRatios, pct2AsymmetricalDifferentRatios, pct2TabularDifferentAngles);

        // add structural ratio after coding

        res = getT2xRatio0(config, ratedU1, ratedU2);
        double a01 = res[0];
        double a02 = res[1];
        a1 *= a01;
        a2 *= a02;

        // phaseAngleClock configuration
        res = getT2xPhaseAngleClock(config);
        angle1 += res[0];
        angle2 += res[1];

        angle1 = Math.toRadians(angle1);
        angle2 = Math.toRadians(angle2);
        Complex aA1 = new Complex(a1 * Math.cos(angle1), a1 * Math.sin(angle1));
        Complex aA2 = new Complex(a2 * Math.cos(angle2), a2 * Math.sin(angle2));

        Complex z = new Complex(r1 + r2, x1 + x2);
        yff = z.reciprocal().add(ysh1).divide(aA1.conjugate().multiply(aA1));
        yft = z.reciprocal().negate().divide(aA1.conjugate().multiply(aA2));
        ytf = z.reciprocal().negate().divide(aA2.conjugate().multiply(aA1));
        ytt = z.reciprocal().add(ysh2).divide(aA2.conjugate().multiply(aA2));
    }

    private double[] getT2xRatio0(String config, double ratedU12, double ratedU22) {
        String configurationRatio0 = "ratio0End2";
        if (config.contains("T2x_ratio0End1")) {
            configurationRatio0 = "ratio0End1";
        }
        if (config.contains("T2x_ratio0End2")) {
            configurationRatio0 = "ratio0End2";
        }
        if (config.contains("T2x_ratio0_rtc")) {
            configurationRatio0 = "ratio0_rtc";
        }
        double a01 = 1.0;
        double a02 = 1.0;
        if (configurationRatio0.equals("ratio0End1")) {
            a01 = ratedU1 / ratedU2;
            a02 = ratedU2 / ratedU2;
        } else if (configurationRatio0.equals("ratio0End2")) {
            a01 = ratedU1 / ratedU1;
            a02 = ratedU2 / ratedU1;
        } else if (configurationRatio0.equals("ratio0_rtc")) {
            if (rsvi1 != 0.0) {
                a01 = ratedU1 / ratedU2; // JAM TODO Es necesario el if ?
                a02 = ratedU2 / ratedU2;
            } else {
                a01 = ratedU1 / ratedU1;
                a02 = ratedU2 / ratedU1;
            }
        }
        return new double[] {a01, a02 };
    }

    private double[] getT2xRatioPhase(String config, double rtc1a, double ptc1a, double rtc2a,
            double ptc2a,
            double rtc1A, double ptc1A, double rtc2A, double ptc2A) {
        String configurationRatio = "ratioEnd1End2";
        if (config.contains("T2x_ratioEnd1")) {
            configurationRatio = "ratioEnd1";
        }
        if (config.contains("T2x_ratioEnd2")) {
            configurationRatio = "ratioEnd2";
        }
        if (config.contains("T2x_ratioEnd1End2")) {
            configurationRatio = "ratioEnd1End2";
        }

        double a1 = 1.0;
        double angle1 = 0.0;
        double a2 = 1.0;
        double angle2 = 0.0;
        if (configurationRatio.equals("ratioEnd1")) {
            a1 = rtc1a * ptc1a * rtc2a * ptc2a;
            angle1 = rtc1A + ptc1A + rtc2A + ptc2A;
        } else if (configurationRatio.equals("ratioEnd2")) {
            a2 = rtc1a * ptc1a * rtc2a * ptc2a;
            angle2 = rtc1A + ptc1A + rtc2A + ptc2A;
        } else if (configurationRatio.equals("ratioEnd1End2")) {
            a1 = rtc1a * ptc1a;
            angle1 = rtc1A + ptc1A;
            a2 = rtc2a * ptc2a;
            angle2 = rtc2A + ptc2A;
        }
        return new double[] {a1, angle1, a2, angle2 };
    }

    private double getT2xPtc2Negate(String config, double angle) {

        double outAngle = angle;
        String configurationPtc2Negate = "ptc2_tabular_negate_off";
        if (config.contains("T2x_ptc2_tabular_negate_off")) {
            configurationPtc2Negate = "ptc2_tabular_negate_off";
        }
        if (config.contains("T2x_ptc2_tabular_negate_on")) {
            configurationPtc2Negate = "ptc2_tabular_negate_on";
        }

        if (configurationPtc2Negate.equals("ptc2_tabular_negate_on")) {
            outAngle = -angle;
        }

        return outAngle;
    }

    private double[] getT2xYShunt(String config) {
        String configurationYshunt = "yshuntEnd1";
        if (config.contains("T2x_yshuntEnd1")) {
            configurationYshunt = "yshuntEnd1";
        }
        if (config.contains("T2x_yshuntEnd2")) {
            configurationYshunt = "yshuntEnd2";
        }
        if (config.contains("T2x_yshuntEnd1End2")) {
            configurationYshunt = "yshuntEnd1End2";
        }
        if (config.contains("T2x_yshunt_split")) {
            configurationYshunt = "yshunt_split";
        }

        Complex ysh1 = Complex.ZERO;
        Complex ysh2 = Complex.ZERO;
        if (configurationYshunt.equals("yshuntEnd1")) {
            ysh1 = ysh1.add(new Complex(g1 + g2, b1 + b2));
        } else if (configurationYshunt.equals("yshuntEnd2")) {
            ysh2 = ysh2.add(new Complex(g1 + g2, b1 + b2));
        } else if (configurationYshunt.equals("yshuntEnd1End2")) {
            ysh1 = ysh1.add(new Complex(g1, b1));
            ysh2 = ysh2.add(new Complex(g2, b2));
        } else if (configurationYshunt.equals("yshunt_split")) {
            ysh1 = ysh1.add(new Complex((g1 + g2) * 0.5, (b1 + b2) * 0.5));
            ysh2 = ysh2.add(new Complex((g1 + g2) * 0.5, (b1 + b2) * 0.5));
        }

        return new double[] {ysh1.getReal(), ysh1.getImaginary(), ysh2.getReal(),
                ysh2.getImaginary() };
    }

    private double[] getT2xPhaseAngleClock(String config) {

        String configurationPhaseAngleClock = "clock_off";
        if (config.contains("T2x_clock_off")) {
            configurationPhaseAngleClock = "clock_off";
        }
        if (config.contains("T2x_clock_on")) {
            configurationPhaseAngleClock = "clock_on";
        }

        String configurationPac2Negate = "pac2_negate_off";
        if (config.contains("T2x_pac2_negate_off")) {
            configurationPac2Negate = "pac2_negate_off";
        }
        if (config.contains("T2x_pac2_negate_on")) {
            configurationPac2Negate = "pac2_negate_on";
        }

        double angle1 = 0.0;
        double angle2 = 0.0;
        if (configurationPhaseAngleClock.equals("clock_on")) {
            if (pac1 != 0) {
                angle1 = getPhaseAngleClock(pac1);
            }
            if (pac2 != 0) {
                angle2 = getPhaseAngleClock(pac2);
            }

            if (configurationPac2Negate.equals("pac2_negate_on")) {
                angle2 = -angle2;
            }
        }

        return new double[] {angle1, angle2 };
    }

    private void setT2xModelCode(Complex ysh1, Complex ysh2,
            double a1, double angle1, double a2, double angle2,
            boolean pct1TabularDifferentRatios, boolean pct1AsymmetricalDifferentRatios,
            boolean pct1TabularDifferentAngles, boolean pct2TabularDifferentRatios,
            boolean pct2AsymmetricalDifferentRatios, boolean pct2TabularDifferentAngles) {
        String modelCode = t2xModelCode(ysh1, ysh2, a1, angle1, a2, angle2,
                rsvi1, rls1, rhs1, pct1TabularDifferentRatios, pct1AsymmetricalDifferentRatios,
                psvi1, stepPhaseShiftIncrement1, pls1, phs1, pct1TabularDifferentAngles,
                rsvi2, rls2, rhs2, pct2TabularDifferentRatios, pct2AsymmetricalDifferentRatios,
                psvi2, stepPhaseShiftIncrement2, pls2, phs2, pct2TabularDifferentAngles);

        setModelCode(modelCode);
    }

    private void t2xParametersCorrectionEnd1(double xc, double rc, double bc, double gc) {
        if (x1 != 0.0) {
            x1 = applyCorrection(x1, xc);
        } else {
            x2 = applyCorrection(x2, xc);
        }
        if (r1 != 0.0) {
            r1 = applyCorrection(r1, rc);
        } else {
            r2 = applyCorrection(r2, rc);
        }
        if (b1 != 0.0) {
            b1 = applyCorrection(b1, bc);
        } else {
            b2 = applyCorrection(b2, bc);
        }
        if (g1 != 0.0) {
            g1 = applyCorrection(g1, gc);
        } else {
            g2 = applyCorrection(g2, gc);
        }
    }

    private void t2xParametersCorrectionEnd2(double xc, double rc, double bc, double gc) {
        if (x2 != 0.0) {
            x2 = applyCorrection(x2, xc);
        } else {
            x1 = applyCorrection(x1, xc);
        }
        if (r2 != 0.0) {
            r2 = applyCorrection(r2, rc);
        } else {
            r1 = applyCorrection(r1, rc);
        }
        if (b2 != 0.0) {
            b2 = applyCorrection(b2, bc);
        } else {
            b1 = applyCorrection(b1, bc);
        }
        if (g2 != 0.0) {
            g2 = applyCorrection(g2, gc);
        } else {
            g1 = applyCorrection(g1, gc);
        }
    }

    private void readT2xParameters(PropertyBag transformer) {
        r1 = transformer.asDouble("r1");
        x1 = transformer.asDouble("x1");
        b1 = transformer.asDouble("b1");
        g1 = transformer.asDouble("g1");
        pac1 = transformer.asInt("pac1", 0);
        ratedU1 = transformer.asDouble("ratedU1");
        rns1 = transformer.asDouble("rns1", 0.0);
        rsvi1 = transformer.asDouble("rsvi1", 0.0);
        rstep1 = transformer.asDouble("rstep1", 0.0);
        rls1 = transformer.asDouble("rls1", 0.0);
        rhs1 = transformer.asDouble("rhs1", 0.0);
        pns1 = transformer.asDouble("pns1", 0.0);
        psvi1 = transformer.asDouble("psvi1", 0.0);
        pstep1 = transformer.asDouble("pstep1", 0.0);
        pls1 = transformer.asDouble("pls1", 0.0);
        phs1 = transformer.asDouble("phs1", 0.0);
        r2 = transformer.asDouble("r2");
        x2 = transformer.asDouble("x2");
        b2 = transformer.asDouble("b2");
        g2 = transformer.asDouble("g2");
        pac2 = transformer.asInt("pac2", 0);
        ratedU2 = transformer.asDouble("ratedU2");
        rns2 = transformer.asDouble("rns2", 0.0);
        rsvi2 = transformer.asDouble("rsvi2", 0.0);
        rstep2 = transformer.asDouble("rstep2", 0.0);
        rls2 = transformer.asDouble("rls2", 0.0);
        rhs2 = transformer.asDouble("rhs2", 0.0);
        pns2 = transformer.asDouble("pns2", 0.0);
        psvi2 = transformer.asDouble("psvi2", 0.0);
        pstep2 = transformer.asDouble("pstep2", 0.0);
        pls2 = transformer.asDouble("pls2", 0.0);
        phs2 = transformer.asDouble("phs2", 0.0);
        pwca1 = transformer.asDouble("pwca1", 0.0);
        pwca2 = transformer.asDouble("pwca2", 0.0);
        stepPhaseShiftIncrement1 = transformer.asDouble("stepPhaseShiftIncrement1", 0.0);
        stepPhaseShiftIncrement2 = transformer.asDouble("stepPhaseShiftIncrement2", 0.0);
        ptype1 = transformer.get("ptype1");
        ptype2 = transformer.get("ptype2");
        phaseTapChangerTable1 = transformer.get("PhaseTapChangerTable1");
        phaseTapChangerTable2 = transformer.get("PhaseTapChangerTable2");
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
    private double rls1;
    private double rhs1;
    private double pns1;
    private double psvi1;
    private double pstep1;
    private double pls1;
    private double phs1;
    private double r2;
    private double x2;
    private double b2;
    private double g2;
    private int    pac2;
    private double ratedU2;
    private double rns2;
    private double rsvi2;
    private double rstep2;
    private double rls2;
    private double rhs2;
    private double pns2;
    private double psvi2;
    private double pstep2;
    private double pls2;
    private double phs2;
    private double pwca1;
    private double pwca2;
    private double stepPhaseShiftIncrement1;
    private double stepPhaseShiftIncrement2;
    private String ptype1;
    private String ptype2;
    private String phaseTapChangerTable1;
    private String phaseTapChangerTable2;
}
