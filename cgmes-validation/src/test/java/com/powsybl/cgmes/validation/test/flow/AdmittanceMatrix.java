package com.powsybl.cgmes.validation.test.flow;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

public class AdmittanceMatrix {

    public AdmittanceMatrix(CgmesModel cgmes) {
        this.cgmes = cgmes;
    }

    protected double applyCorrection(double x, double xc) {
        return x * (1.0 + xc / 100.0);
    }

    protected double[] getRatioTapChangerData(double rstep, double rns, double rsvi) {
        double rtca = 1.0 + (rstep - rns) * (rsvi / 100.0);
        double rtcA = 0.0;
        return new double[] {rtca, rtcA };
    }

    protected boolean phaseTapChangerIsTabular(String ptype, String phaseTapChangerTable) {
        if (ptype != null && phaseTapChangerTable != null && ptype.endsWith("tabular")) {
            return true;
        }
        return false;
    }

    protected boolean phaseTapChangerIsAsymmetrical(String ptype) {
        if (ptype != null && ptype.endsWith("asymmetrical")) {
            return true;
        }
        return false;
    }

    protected boolean phaseTapChangerIsSymmetrical(String ptype) {
        if (ptype != null && !ptype.endsWith("asymmetrical") && ptype.endsWith("symmetrical")) {
            return true;
        }
        return false;
    }

    protected boolean getTabularPhaseTapChangerDifferentRatios(String phaseTapChangerTableName) {
        PropertyBags phaseTapChangerTable = cgmes.phaseTapChangerTable(phaseTapChangerTableName);
        return phaseTapChangerTable.stream().map(pb -> {
            return pb.asDouble("ratio");
        }).mapToDouble(Double::doubleValue).distinct().limit(2).count() > 1;
    }

    protected boolean getTabularPhaseTapChangerDifferentAngles(String phaseTapChangerTableName) {
        PropertyBags phaseTapChangerTable = cgmes.phaseTapChangerTable(phaseTapChangerTableName);
        return phaseTapChangerTable.stream().map(pb -> {
            return pb.asDouble("angle");
        }).mapToDouble(Double::doubleValue).distinct().limit(2).count() > 1;
    }

    protected boolean getAsymmetricalPhaseTapChangerDifferentRatios(double psvi, double pls,
            double phs) {
        if (psvi != 0.0 && pls != phs) {
            return true;
        }
        return false;
    }

    protected double[] getTabularPhaseTapChangerData(double pstep,
            String phaseTapChangerTableName) {
        double ptca = 1.0;
        double ptcA = 0.0;
        double rc = 0.0;
        double xc = 0.0;
        double bc = 0.0;
        double gc = 0.0;
        PropertyBags phaseTapChangerTable = cgmes.phaseTapChangerTable(phaseTapChangerTableName);
        for (PropertyBag point : phaseTapChangerTable) {
            if (point.asInt("step") == pstep) {
                ptca = point.asDouble("ratio");
                ptcA = point.asDouble("angle");
                xc = point.asDouble("x");
                rc = point.asDouble("r");
                bc = point.asDouble("b");
                gc = point.asDouble("g");
                return new double[] {ptca, ptcA, xc, rc, bc, gc };
            }
        }
        return new double[] {ptca, ptcA, xc, rc, bc, gc };
    }

    protected double[] getSymmetricalPhaseTapChangerData(String ptype, double pstep, double pns,
            double psvi, double stepPhaseShiftIncrement1) {
        double ptca = 1.0;
        double ptcA = 0.0;
        if (stepPhaseShiftIncrement1 != 0.0) {
            ptca = 1.0;
            ptcA = (pstep - pns) * stepPhaseShiftIncrement1;
        } else {
            double dy = (pstep - pns) * (psvi / 100.0);
            ptca = 1.0;
            ptcA = Math.toDegrees(Math.atan2(dy, 1.0));
        }
        return new double[] {ptca, ptcA };
    }

    protected double[] getAsymmetricalPhaseTapChangerData(String ptype, double pstep, double pns,
            double psvi, double pwca) {
        double ptca = 1.0;
        double ptcA = 0.0;
        double dx = 1.0 + (pstep - pns) * (psvi / 100.0) * Math.cos(Math.toRadians(pwca));
        double dy = (pstep - pns) * (psvi / 100.0) * Math.sin(Math.toRadians(pwca));
        ptca = Math.hypot(dx, dy);
        ptcA = Math.toDegrees(Math.atan2(dy, dx));

        return new double[] {ptca, ptcA };
    }

    protected double getPhaseAngleClock(int phaseAngleClock) {
        double phaseAngleClockDegree = 0.0;
        phaseAngleClockDegree += phaseAngleClock * 30.0;
        phaseAngleClockDegree = Math.IEEEremainder(phaseAngleClockDegree, 360.0);
        if (phaseAngleClockDegree > 180.0) {
            phaseAngleClockDegree -= 360.0;
        }
        return phaseAngleClockDegree;
    }

    protected String lineModelCode(double bsh1, double bsh2) {
        StringBuilder modelCode = new StringBuilder();
        modelCode.append(lineBshuntModelCode(bsh1, bsh2));
        return modelCode.toString();
    }

    protected String lineBshuntModelCode(double bsh1, double bsh2) {
        StringBuilder modelCode = new StringBuilder();
        if (bsh1 == 0.0) {
            modelCode.append("N");
        } else {
            modelCode.append("Y");
        }
        if (bsh2 == 0.0) {
            modelCode.append("N");
        } else {
            modelCode.append("Y");
        }
        return modelCode.toString();
    }

    protected String t3xModelCode(String t2xModelCode1, String t2xModelCode2,
            String t2xModelCode3) {
        StringBuilder modelCode = new StringBuilder();
        modelCode.append(t2xModelCode1 + "." + t2xModelCode2 + "." + t2xModelCode3);
        return modelCode.toString();
    }

    protected String t3xModelCodeEnd(Complex ysh1, Complex ysh2,
            double a1, double angle1, double a2, double angle2,
            double rsvi1, double rls1, double rhs1, boolean pct1TabularDifferentRatios,
            boolean pct1AsymmetricalDifferentRatios,
            double psvi1, double stepPhaseShiftIncrement1, double pls1, double phs1,
            boolean pct1TabularDifferentAngles) {
        StringBuilder modelCode = new StringBuilder();

        // Only one of them (a1, a2) could be != 1.0
        // Only one of them (angle1, angle2) could be != 0.0

        if (a1 != 1.0) {
            modelCode.append(t2xRatioModelCode(a1, rsvi1, rls1, rhs1,
                    pct1TabularDifferentRatios, pct1AsymmetricalDifferentRatios));
        } else {
            modelCode.append(t2xRatioModelCodeNoRatio());
        }

        if (angle1 != 0.0) {
            modelCode.append(t2xPhaseModelCode(angle1, psvi1, stepPhaseShiftIncrement1, pls1, phs1,
                    pct1TabularDifferentAngles));
        } else {
            modelCode.append(t2xPhaseModelCodeNoPhase());
        }

        modelCode.append(t2xYshuntModelCode(ysh1, ysh2));

        if (a2 != 1.0) {
            modelCode.append(t2xRatioModelCode(a2, rsvi1, rls1, rhs1,
                    pct1TabularDifferentRatios, pct1AsymmetricalDifferentRatios));
        } else {
            modelCode.append(t2xRatioModelCodeNoRatio());
        }

        if (angle2 != 0.0) {
            modelCode.append(t2xPhaseModelCode(angle2, psvi1, stepPhaseShiftIncrement1, pls1, phs1,
                    pct1TabularDifferentAngles));
        } else {
            modelCode.append(t2xPhaseModelCodeNoPhase());
        }

        return modelCode.toString();
    }

    protected String t2xModelCode(Complex ysh1, Complex ysh2,
            double a1, double angle1, double a2, double angle2,
            double rsvi1, double rls1, double rhs1, boolean pct1TabularDifferentRatios,
            boolean pct1AsymmetricalDifferentRatios,
            double psvi1, double stepPhaseShiftIncrement1, double pls1, double phs1,
            boolean pct1TabularDifferentAngles,
            double rsvi2, double rls2, double rhs2, boolean pct2TabularDifferentRatios,
            boolean pct2AsymmetricalDifferentRatios,
            double psvi2, double stepPhaseShiftIncrement2, double pls2, double phs2,
            boolean pct2TabularDifferentAngles) {
        StringBuilder modelCode = new StringBuilder();
        modelCode.append(t2xRatioModelCode(a1, rsvi1, rls1, rhs1,
                pct1TabularDifferentRatios, pct1AsymmetricalDifferentRatios));
        modelCode.append(t2xPhaseModelCode(angle1, psvi1, stepPhaseShiftIncrement1, pls1, phs1,
                pct1TabularDifferentAngles));
        modelCode.append(t2xYshuntModelCode(ysh1, ysh2));
        modelCode.append(t2xRatioModelCode(a2, rsvi2, rls2, rhs2,
                pct2TabularDifferentRatios, pct2AsymmetricalDifferentRatios));
        modelCode.append(t2xPhaseModelCode(angle2, psvi2, stepPhaseShiftIncrement1, pls2, phs2,
                pct2TabularDifferentAngles));
        return modelCode.toString();
    }

    protected String t2xRatioModelCode(double a, double rsvi, double rls, double rhs,
            boolean pctTabularDifferentRatios, boolean pctAsymmetricalDifferentRatios) {
        StringBuilder modelCode = new StringBuilder();

        if (a == 1.0) {
            if (rsvi != 0.0 && rls != rhs) {
                modelCode.append("r");
            } else if (pctTabularDifferentRatios) {
                modelCode.append("r");
            } else if (pctAsymmetricalDifferentRatios) {
                modelCode.append("r");
            } else {
                modelCode.append("_");
            }
        } else {
            if (rsvi != 0.0 && rls != rhs) {
                modelCode.append("R");
            } else if (pctTabularDifferentRatios) {
                modelCode.append("R");
            } else if (pctAsymmetricalDifferentRatios) {
                modelCode.append("R");
            } else {
                modelCode.append("*");
            }
        }

        return modelCode.toString();
    }

    protected String t2xRatioModelCodeNoRatio() {
        StringBuilder modelCode = new StringBuilder();
        modelCode.append("_");
        return modelCode.toString();
    }

    protected String t2xPhaseModelCode(double angle, double psvi, double stepPhaseShiftIncrement,
            double pls, double phs, boolean pct1TabularDifferentAngles) {
        StringBuilder modelCode = new StringBuilder();
        if (angle == 0.0) {
            if (psvi != 0.0 && pls != phs) {
                modelCode.append("p");
            } else if (stepPhaseShiftIncrement != 0 && pls != phs) {
                modelCode.append("p");
            } else if (pct1TabularDifferentAngles) {
                modelCode.append("p");
            } else {
                modelCode.append("_");
            }
        } else {
            if (psvi != 0.0 && pls != phs) {
                modelCode.append("P");
            } else if (stepPhaseShiftIncrement != 0 && pls != phs) {
                modelCode.append("P");
            } else if (pct1TabularDifferentAngles) {
                modelCode.append("P");
            } else {
                modelCode.append("*");
            }
        }
        return modelCode.toString();
    }

    protected String t2xPhaseModelCodeNoPhase() {
        StringBuilder modelCode = new StringBuilder();
        modelCode.append("_");
        return modelCode.toString();
    }

    protected String t2xYshuntModelCode(Complex ysh1, Complex ysh2) {
        StringBuilder modelCode = new StringBuilder();
        if (ysh1.equals(Complex.ZERO)) {
            modelCode.append("N");
        } else {
            modelCode.append("Y");
        }
        if (ysh2.equals(Complex.ZERO)) {
            modelCode.append("N");
        } else {
            modelCode.append("Y");
        }
        return modelCode.toString();
    }

    public Complex getYff() {
        return yff;
    }

    public Complex getYft() {
        return yft;
    }

    public Complex getYtf() {
        return ytf;
    }

    public Complex getYtt() {
        return ytt;
    }

    public String getModelCode() {
        return code.toString();
    }

    protected void setModelCode(String code) {
        this.code = new StringBuilder(code);
    }

    protected CgmesModel          cgmes;
    protected Complex             yff;
    protected Complex             yft;
    protected Complex             ytf;
    protected Complex             ytt;
    protected StringBuilder       code;

    protected static final Logger LOG = LoggerFactory
            .getLogger(AdmittanceMatrix.class);
}
