package com.powsybl.cgmes.validation.test.flow;

import java.util.Map;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

public class T2xAdmittanceMatrix extends AdmittanceMatrix {

    public T2xAdmittanceMatrix(CgmesModel cgmes, Map<String, Integer> equipmentsReport) {
        super(cgmes, equipmentsReport);
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
        double pwca1 = transformer.asDouble("pwca1", 0.0);
        double pwca2 = transformer.asDouble("pwca2", 0.0);
        double stepPhaseShiftIncrement1 = transformer.asDouble("stepPhaseShiftIncrement1", 0.0);
        double stepPhaseShiftIncrement2 = transformer.asDouble("stepPhaseShiftIncrement2", 0.0);
        String ptype1 = transformer.get("ptype1");
        String ptype2 = transformer.get("ptype2");
        String phaseTapChangerTable1 = transformer.get("PhaseTapChangerTable1");
        String phaseTapChangerTable2 = transformer.get("PhaseTapChangerTable2");

        String configurationRatio0 = "ratio0_end2";
        String configurationRatio = "ratio_end1_end2";
        String configurationYshunt = "yshunt_end1";
        String configurationPtc2Negate = "ptc2_tabular_negate_off";
        String configurationPhaseAngleClock = "clock_off";
        String configurationPac2Negate = "pac2_negate_off";
        LOG.debug(" transformer {}", transformer);

        if (config.contains("T2x_ratio0_end1")) {
            configurationRatio0 = "ratio0_end1";
        }
        if (config.contains("T2x_ratio0_end2")) {
            configurationRatio0 = "ratio0_end2";
        }
        if (config.contains("T2x_ratio0_rtc")) {
            configurationRatio0 = "ratio0_rtc";
        }
        if (config.contains("T2x_ratio_end1")) {
            configurationRatio = "ratio_end1";
        }
        if (config.contains("T2x_ratio_end2")) {
            configurationRatio = "ratio_end2";
        }
        if (config.contains("T2x_ratio_end1_end2")) {
            configurationRatio = "ratio_end1_end2";
        }
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
        if (config.contains("T2x_ptc2_tabular_negate_off")) {
            configurationPtc2Negate = "ptc2_tabular_negate_off";
        }
        if (config.contains("T2x_ptc2_tabular_negate_on")) {
            configurationPtc2Negate = "ptc2_tabular_negate_on";
        }
        if (config.contains("T2x_clock_off")) {
            configurationPhaseAngleClock = "clock_off";
        }
        if (config.contains("T2x_clock_on")) {
            configurationPhaseAngleClock = "clock_on";
        }
        if (config.contains("T2x_pac2_negate_off")) {
            configurationPac2Negate = "pac2_negate_off";
        }
        if (config.contains("T2x_pac2_negate_on")) {
            configurationPac2Negate = "pac2_negate_on";
        }

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
                    if (x1 != 0.0) {
                        x1 = applyCorrection(x1, xc);
                    } else {
                        x2 = applyCorrection(x2, xc);
                    }
                    double rc = point.asDouble("r");
                    if (r1 != 0.0) {
                        r1 = applyCorrection(r1, rc);
                    } else {
                        r2 = applyCorrection(r2, rc);
                    }
                    double bc = point.asDouble("b");
                    if (b1 != 0.0) {
                        b1 = applyCorrection(b1, bc);
                    } else {
                        b2 = applyCorrection(b2, bc);
                    }
                    double gc = point.asDouble("g");
                    if (g1 != 0.0) {
                        g1 = applyCorrection(g1, gc);
                    } else {
                        g2 = applyCorrection(g2, gc);
                    }
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
                    if (configurationPtc2Negate.equals("ptc2_tabular_negate_on")) {
                        ptc2A = -ptc2A;
                    }
                    double xc = point.asDouble("x");
                    if (x2 != 0.0) {
                        x2 = applyCorrection(x2, xc);
                    } else {
                        x1 = applyCorrection(x1, xc);
                    }
                    double rc = point.asDouble("r");
                    if (r2 != 0.0) {
                        r2 = applyCorrection(r2, rc);
                    } else {
                        r1 = applyCorrection(r1, rc);
                    }
                    double bc = point.asDouble("b");
                    if (b2 != 0.0) {
                        b2 = applyCorrection(b2, bc);
                    } else {
                        b1 = applyCorrection(b1, bc);
                    }
                    double gc = point.asDouble("g");
                    if (g2 != 0.0) {
                        g2 = applyCorrection(g2, gc);
                    } else {
                        g1 = applyCorrection(g1, gc);
                    }
                }
            }
        }
        LOG.debug(" rtc1 {} {} ptc1 {} {} rtc2 {} {} ptc2 {} {}", rtc1a, rtc1A, ptc1a, ptc1A,
                rtc2a, rtc2A, ptc2a, ptc2A);

        double a1 = 1.0;
        double angle1 = 0.0;
        double a2 = 1.0;
        double angle2 = 0.0;

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

        // yshunt configuration

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

        StringBuilder code = new StringBuilder();
        if (a1 == 1.0) {
            if (transformer.containsKey("rstep1")) {
                if (!Double.isNaN(transformer.asDouble("rhs1")) && transformer.asDouble("rls1", 0.0) == transformer.asDouble("rhs1", 0.0)) {
                    code.append("*");
                } else {
                    code.append("r");
                }
            } else {
                code.append("_");
            }
        } else {
            if (!Double.isNaN(transformer.asDouble("rhs1")) && transformer.asDouble("rls1", 0.0) == transformer.asDouble("rhs1", 0.0)) {
                code.append("*");
            } else {
                code.append("R");
            }
        }
        if (angle1 == 0.0) {
            if (transformer.containsKey("pstep1")) {
                if (!Double.isNaN(transformer.asDouble("phs1")) && transformer.asDouble("pls1", 0.0) == transformer.asDouble("phs1", 0.0)) {
                    code.append("*");
                } else {
                    code.append("p");
                }
            } else {
                code.append("_");
            }
        } else {
            if (!Double.isNaN(transformer.asDouble("phs1")) && transformer.asDouble("pls1", 0.0) == transformer.asDouble("phs1", 0.0)) {
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
            if (transformer.containsKey("rstep2")) {
                if (!Double.isNaN(transformer.asDouble("rhs2")) && transformer.asDouble("rls2") == transformer.asDouble("rhs2")) {
                    LOG.info("trafo {}", transformer);
                    code.append("*");
                } else {
                    code.append("r");
                }
            } else {
                code.append("_");
            }
        } else {
            if (!Double.isNaN(transformer.asDouble("rhs2")) && transformer.asDouble("rls2") == transformer.asDouble("rhs2")) {
                LOG.info("trafo {}", transformer);
                code.append("*");
            } else {
                code.append("R");
            }
        }
        if (angle2 == 0.0) {
            if (transformer.containsKey("pstep2")) {
                if (!Double.isNaN(transformer.asDouble("phs2")) && transformer.asDouble("pls2", 0.0) == transformer.asDouble("phs2", 0.0)) {
                    code.append("*");
                } else {
                    code.append("p");
                }
            } else {
                code.append("_");
            }
        } else {
            if (!Double.isNaN(transformer.asDouble("phs2")) && transformer.asDouble("pls2", 0.0) == transformer.asDouble("phs2", 0.0)) {
                code.append("*");
            } else {
                code.append("P");
            }
        }
        transformer.put("code", code.toString());
        Integer total = equipmentsReport.get(code.toString());
        if (total == null) {
            total = new Integer(0);
        }
        equipmentsReport.put(code.toString(), total + 1);

        // add structural ratio after coding
        a1 *= a10;
        a2 *= a20;

        // phaseAngleClock configuration

        if (configurationPhaseAngleClock.equals("clock_on")) {
            if (pac1 != 0) {
                LOG.info("pac1 trafo {}", transformer);
                angle1 += getPhaseAngleClock(pac1);
            }
            if (pac2 != 0) {
                if (configurationPac2Negate.equals("pac2_negate_on")) {
                    angle2 -= getPhaseAngleClock(pac2);
                } else {
                    angle2 = getPhaseAngleClock(pac2);
                }
            }
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

    private double applyCorrection(double v, double vc) {
        return v * (1.0 + vc / 100.0);
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

}
