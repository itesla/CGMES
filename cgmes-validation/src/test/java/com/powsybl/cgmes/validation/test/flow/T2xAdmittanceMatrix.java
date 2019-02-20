package com.powsybl.cgmes.validation.test.flow;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;

public class T2xAdmittanceMatrix extends AdmittanceMatrix {

    public T2xAdmittanceMatrix(CgmesModel cgmes) {
        super(cgmes);
    }

    public void calculate(PropertyBag transformer, String config) {
        double[] res;
        readParameters(transformer);

        String configurationPtc2Negate = "ptc2_tabular_negate_off";
        String configurationPac2Negate = "pac2_negate_off";
        LOG.debug(" transformer {}", transformer);

        if (config.contains("T2x_ptc2_tabular_negate_off")) {
            configurationPtc2Negate = "ptc2_tabular_negate_off";
        }
        if (config.contains("T2x_ptc2_tabular_negate_on")) {
            configurationPtc2Negate = "ptc2_tabular_negate_on";
        }
        if (config.contains("T2x_pac2_negate_off")) {
            configurationPac2Negate = "pac2_negate_off";
        }
        if (config.contains("T2x_pac2_negate_on")) {
            configurationPac2Negate = "pac2_negate_on";
        }

        // ratio configuration
        res = getRatioConfiguration(rstep1, rns1, rsvi1);
        double rtc1a = res[0];
        double rtc1A = res[1];
        if (ptype1 != null && phaseTapChangerTable1 != null && ptype1.endsWith("tabular")) {
            res = getTabularPhaseConfiguration(pstep1, phaseTapChangerTable1);
            double xc = res[2];
            if (x1 != 0.0) {
                x1 = applyCorrection(x1, xc);
            } else {
                x2 = applyCorrection(x2, xc);
            }
            double rc = res[3];
            if (r1 != 0.0) {
                r1 = applyCorrection(r1, rc);
            } else {
                r2 = applyCorrection(r2, rc);
            }
            double bc = res[4];
            if (b1 != 0.0) {
                b1 = applyCorrection(b1, bc);
            } else {
                b2 = applyCorrection(b2, bc);
            }
            double gc = res[5];
            if (g1 != 0.0) {
                g1 = applyCorrection(g1, gc);
            } else {
                g2 = applyCorrection(g2, gc);
            }
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
            if (configurationPtc2Negate.equals("ptc2_tabular_negate_on")) {
                res[1] = -res[1];
            }
            double xc = res[2];
            if (x2 != 0.0) {
                x2 = applyCorrection(x2, xc);
            } else {
                x1 = applyCorrection(x1, xc);
            }
            double rc = res[3];
            if (r2 != 0.0) {
                r2 = applyCorrection(r2, rc);
            } else {
                r1 = applyCorrection(r1, rc);
            }
            double bc = res[4];
            if (b2 != 0.0) {
                b2 = applyCorrection(b2, bc);
            } else {
                b1 = applyCorrection(b1, bc);
            }
            double gc = res[5];
            if (g2 != 0.0) {
                g2 = applyCorrection(g2, gc);
            } else {
                g1 = applyCorrection(g1, gc);
            }
        } else {
            res = getPhaseConfiguration(ptype2, pstep2, pns2, psvi2, pwca2,
                    stepPhaseShiftIncrement2);
        }
        double ptc2a = res[0];
        double ptc2A = res[1];
        LOG.debug(" rtc1 {} {} ptc1 {} {} rtc2 {} {} ptc2 {} {}", rtc1a, rtc1A, ptc1a, ptc1A,
                rtc2a, rtc2A, ptc2a, ptc2A);

        res = getRatio0(config, ratedU1, ratedU2);
        double a01 = res[0];
        double a02 = res[1];

        res = getRatio(config, rtc1a, ptc1a, rtc2a, ptc2a, rtc1A, ptc1A, rtc2A, ptc2A);
        double a1 = res[0];
        double angle1 = res[1];
        double a2 = res[2];
        double angle2 = res[3];

        // yshunt configuration
        res = getYShunt(config);
        Complex ysh1 = new Complex(res[0], res[1]);
        Complex ysh2 = new Complex(res[2], res[3]);

        setCode(transformer, ysh1, ysh2, a1, angle1, a2, angle2);

        // add structural ratio after coding
        a1 *= a01;
        a2 *= a02;

        // phaseAngleClock configuration
        res = getPhaseAngleClock(config);
        angle1 += res[0];
        if (configurationPac2Negate.equals("pac2_negate_on")) {
            angle2 -= res[1];
        } else {
            angle2 += res[1];
        }

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

    private double[] getPhaseAngleClock(String config) {
        String configurationPhaseAngleClock = "clock_off";
        if (config.contains("T2x_clock_off")) {
            configurationPhaseAngleClock = "clock_off";
        }
        if (config.contains("T2x_clock_on")) {
            configurationPhaseAngleClock = "clock_on";
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
        }

        return new double[] {angle1, angle2};
    }

    private double[] getYShunt(String config) {
        String configurationYshunt = "yshunt_end1";
        if (config.contains("T2x_yshunt_end1")) {
            configurationYshunt = "yshunt_end1";
        }
        if (config.contains("T2x_yshunt_end2")) {
            configurationYshunt = "yshunt_end2";
        }
        if (config.contains("T2x_yshunt_end1_end2")) {
            configurationYshunt = "yshunt_end1_end2";
        }
        if (config.contains("T2x_yshunt_split")) {
            configurationYshunt = "yshunt_split";
        }

        Complex ysh1 = Complex.ZERO;
        Complex ysh2 = Complex.ZERO;
        if (configurationYshunt.equals("yshunt_end1")) {
            ysh1 = ysh1.add(new Complex(g1 + g2, b1 + b2));
        } else if (configurationYshunt.equals("yshunt_end2")) {
            ysh2 = ysh2.add(new Complex(g1 + g2, b1 + b2));
        } else if (configurationYshunt.equals("yshunt_end1_end2")) {
            ysh1 = ysh1.add(new Complex(g1, b1));
            ysh2 = ysh2.add(new Complex(g2, b2));
        } else if (configurationYshunt.equals("yshunt_split")) {
            ysh1 = ysh1.add(new Complex((g1 + g2) * 0.5, (b1 + b2) * 0.5));
            ysh2 = ysh2.add(new Complex((g1 + g2) * 0.5, (b1 + b2) * 0.5));
        }

        return new double[] {ysh1.getReal(), ysh1.getImaginary(), ysh2.getReal(),
                ysh2.getImaginary()};
    }

    private void setCode(PropertyBag transformer, Complex ysh1, Complex ysh2, double a1,
            double angle1, double a2, double angle2) {
        double rstep1 = transformer.asDouble("rstep1");
        double rls1 = transformer.asDouble("rls1");
        double rhs1 = transformer.asDouble("rhs1");
        double pstep1 = transformer.asDouble("pstep1");
        double pls1 = transformer.asDouble("pls1");
        double phs1 = transformer.asDouble("phs1");
        double rstep2 = transformer.asDouble("rstep2");
        double rls2 = transformer.asDouble("rls2");
        double rhs2 = transformer.asDouble("rhs2");
        double pstep2 = transformer.asDouble("pstep2");
        double pls2 = transformer.asDouble("pls2");
        double phs2 = transformer.asDouble("phs2");
        String code1 = endCode(transformer, ysh1, ysh2, a1, angle1, a2, angle2, rstep1, rls1, rhs1,
                pstep1, pls1, phs1, rstep2, rls2, rhs2, pstep2, pls2, phs2);

        setModelCode(code1);
    }

    private double[] getRatio(String config, double rtc1a, double ptc1a, double rtc2a, double ptc2a,
            double rtc1A, double ptc1A, double rtc2A, double ptc2A) {
        String configurationRatio = "ratio_end1_end2";
        if (config.contains("T2x_ratio_end1")) {
            configurationRatio = "ratio_end1";
        }
        if (config.contains("T2x_ratio_end2")) {
            configurationRatio = "ratio_end2";
        }
        if (config.contains("T2x_ratio_end1_end2")) {
            configurationRatio = "ratio_end1_end2";
        }

        double a1 = 1.0;
        double angle1 = 0.0;
        double a2 = 1.0;
        double angle2 = 0.0;
        if (configurationRatio.equals("ratio_end1")) {
            a1 = rtc1a * ptc1a * rtc2a * ptc2a;
            angle1 = rtc1A + ptc1A + rtc2A + ptc2A;
        } else if (configurationRatio.equals("ratio_end2")) {
            a2 = rtc1a * ptc1a * rtc2a * ptc2a;
            angle2 = rtc1A + ptc1A + rtc2A + ptc2A;
        } else if (configurationRatio.equals("ratio_end1_end2")) {
            a1 = rtc1a * ptc1a;
            angle1 = rtc1A + ptc1A;
            a2 = rtc2a * ptc2a;
            angle2 = rtc2A + ptc2A;
        }
        return new double[] {a1, angle1, a2, angle2};
    }

    private double[] getRatio0(String config, double ratedU12, double ratedU22) {
        String configurationRatio0 = "ratio0_end2";
        if (config.contains("T2x_ratio0_end1")) {
            configurationRatio0 = "ratio0_end1";
        }
        if (config.contains("T2x_ratio0_end2")) {
            configurationRatio0 = "ratio0_end2";
        }
        if (config.contains("T2x_ratio0_rtc")) {
            configurationRatio0 = "ratio0_rtc";
        }
        double a10 = 1.0;
        double a20 = 1.0;
        if (configurationRatio0.equals("ratio0_end1")) {
            a10 = ratedU1 / ratedU2;
            a20 = ratedU2 / ratedU2;
        } else if (configurationRatio0.equals("ratio0_end2")) {
            a10 = ratedU1 / ratedU1;
            a20 = ratedU2 / ratedU1;
        } else if (configurationRatio0.equals("ratio0_rtc")) {
            if (rsvi1 != 0.0) {
                a10 = ratedU1 / ratedU2;
                a20 = ratedU2 / ratedU2;
            } else {
                a10 = ratedU1 / ratedU1;
                a20 = ratedU2 / ratedU1;
            }
        }
        return new double[] {a10, a20};
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
    private double pwca1;
    private double pwca2;
    private double stepPhaseShiftIncrement1;
    private double stepPhaseShiftIncrement2;
    private String ptype1;
    private String ptype2;
    private String phaseTapChangerTable1;
    private String phaseTapChangerTable2;
}
