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

    protected boolean ratioTapChangerIsTabular(String ratioTapChangerTable) {
        if (ratioTapChangerTable != null) {
            return true;
        }
        return false;
    }

    protected TapChangerData getRatioTapChangerData(double rstep, double rns, double rsvi) {
        TapChangerData tapChangerData = new TapChangerData();
        tapChangerData.rptca = 1.0 + (rstep - rns) * (rsvi / 100.0);
        tapChangerData.rptcA = 0.0;
        return tapChangerData;
    }

    protected TapChangerData getTabularRatioTapChangerData(double rstep,
            String ratioTapChangerTableName) {
        TapChangerData tapChangerData = new TapChangerData();
        PropertyBags ratioTapChangerTable = cgmes.ratioTapChangerTable(ratioTapChangerTableName);
        if (ratioTapChangerTable.isEmpty()) {
            LOG.warn("Empty RatioTapChangerTable");
        }
        for (PropertyBag point : ratioTapChangerTable) {
            if (point.asInt("step") == rstep) {
                tapChangerData.rptca = point.asDouble("ratio", 0.0);
                tapChangerData.xc = point.asDouble("x", 0.0);
                tapChangerData.rc = point.asDouble("r", 0.0);
                tapChangerData.bc = point.asDouble("b", 0.0);
                tapChangerData.gc = point.asDouble("g", 0.0);
                return tapChangerData;
            }
        }

        return tapChangerData;
    }

    protected boolean getTabularRatioTapChangerDifferentRatios(String ratioTapChangerTableName) {
        PropertyBags ratioTapChangerTable = cgmes.ratioTapChangerTable(ratioTapChangerTableName);
        return ratioTapChangerTable.stream().map(pb -> {
            return pb.asDouble("ratio");
        }).mapToDouble(Double::doubleValue).distinct().limit(2).count() > 1;
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

    protected TapChangerData getTabularPhaseTapChangerData(double pstep,
            String phaseTapChangerTableName) {
        TapChangerData tapChangerData = new TapChangerData();
        PropertyBags phaseTapChangerTable = cgmes.phaseTapChangerTable(phaseTapChangerTableName);
        if (phaseTapChangerTable.isEmpty()) {
            LOG.warn("Empty PhaseTapChangerTable");
        }
        for (PropertyBag point : phaseTapChangerTable) {
            if (point.asInt("step") == pstep) {
                tapChangerData.rptca = point.asDouble("ratio", 1.0);
                tapChangerData.rptcA = point.asDouble("angle", 0.0);

                tapChangerData.xc = point.asDouble("x", 0.0);
                tapChangerData.rc = point.asDouble("r", 0.0);
                tapChangerData.bc = point.asDouble("b", 0.0);
                tapChangerData.gc = point.asDouble("g", 0.0);
                return tapChangerData;
            }
        }
        return tapChangerData;
    }

    protected TapChangerData getSymmetricalPhaseTapChangerData(String ptype, double pstep,
            double pns,
            double psvi, double stepPhaseShiftIncrement1) {
        TapChangerData tapChangerData = new TapChangerData();
        tapChangerData.rptca = 1.0;
        tapChangerData.rptcA = 0.0;
        if (stepPhaseShiftIncrement1 != 0.0) {
            tapChangerData.rptca = 1.0;
            tapChangerData.rptcA = (pstep - pns) * stepPhaseShiftIncrement1;
        } else {
            double dy = (pstep - pns) * (psvi / 100.0);
            tapChangerData.rptca = 1.0;
            tapChangerData.rptcA = Math.toDegrees(Math.atan2(dy, 1.0));
        }
        return tapChangerData;
    }

    protected TapChangerData getAsymmetricalPhaseTapChangerData(String ptype, double pstep,
            double pns,
            double psvi, double pwca) {
        TapChangerData tapChangerData = new TapChangerData();
        tapChangerData.rptca = 1.0;
        tapChangerData.rptcA = 0.0;
        double dx = 1.0 + (pstep - pns) * (psvi / 100.0) * Math.cos(Math.toRadians(pwca));
        double dy = (pstep - pns) * (psvi / 100.0) * Math.sin(Math.toRadians(pwca));
        tapChangerData.rptca = Math.hypot(dx, dy);
        tapChangerData.rptcA = Math.toDegrees(Math.atan2(dy, dx));

        return tapChangerData;
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

    protected String t3xModelCodeEnd(Complex ysh1, Complex ysh2, double a1, double angle1,
            double a2, double angle2, double rsvi1, double rls1, double rhs1,
            boolean rct1TabularDifferentRatios, boolean pct1TabularDifferentRatios,
            boolean pct1AsymmetricalDifferentRatios, double psvi1, double stepPhaseShiftIncrement1,
            double pls1, double phs1, boolean pct1TabularDifferentAngles) {
        StringBuilder modelCode = new StringBuilder();

        // Only one of them (a1, a2) could be != 1.0
        // Only one of them (angle1, angle2) could be != 0.0

        if (a1 != 1.0) {
            modelCode.append(t2xRatioModelCode(a1, rsvi1, rls1, rhs1, rct1TabularDifferentRatios,
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
            modelCode.append(t2xRatioModelCode(a2, rsvi1, rls1, rhs1, rct1TabularDifferentRatios,
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

    protected String t2xModelCode(Complex ysh1, Complex ysh2, double a1, double angle1, double a2,
            double angle2, double rsvi1, double rls1, double rhs1,
            boolean rct1TabularDifferentRatios, boolean pct1TabularDifferentRatios,
            boolean pct1AsymmetricalDifferentRatios, double psvi1, double stepPhaseShiftIncrement1,
            double pls1, double phs1, boolean pct1TabularDifferentAngles, double rsvi2, double rls2,
            double rhs2, boolean rct2TabularDifferentRatios, boolean pct2TabularDifferentRatios,
            boolean pct2AsymmetricalDifferentRatios, double psvi2, double stepPhaseShiftIncrement2,
            double pls2, double phs2, boolean pct2TabularDifferentAngles) {
        StringBuilder modelCode = new StringBuilder();
        modelCode.append(t2xRatioModelCode(a1, rsvi1, rls1, rhs1, rct1TabularDifferentRatios,
                pct1TabularDifferentRatios, pct1AsymmetricalDifferentRatios));
        modelCode.append(t2xPhaseModelCode(angle1, psvi1, stepPhaseShiftIncrement1, pls1, phs1,
                pct1TabularDifferentAngles));
        modelCode.append(t2xYshuntModelCode(ysh1, ysh2));
        modelCode.append(t2xRatioModelCode(a2, rsvi2, rls2, rhs2, rct2TabularDifferentRatios,
                pct2TabularDifferentRatios, pct2AsymmetricalDifferentRatios));
        modelCode.append(t2xPhaseModelCode(angle2, psvi2, stepPhaseShiftIncrement1, pls2, phs2,
                pct2TabularDifferentAngles));
        return modelCode.toString();
    }

    protected String t2xRatioModelCode(double a, double rsvi, double rls, double rhs,
            boolean rctTabularDifferentRatios,
            boolean pctTabularDifferentRatios, boolean pctAsymmetricalDifferentRatios) {
        StringBuilder modelCode = new StringBuilder();

        if (a == 1.0) {
            if (rsvi != 0.0 && rls != rhs) {
                modelCode.append("r");
            } else if (rctTabularDifferentRatios) {
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
            } else if (rctTabularDifferentRatios) {
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

    protected class TapChangerData {
        double rptca = 1.0;
        double rptcA = 0.0;
        double rc    = 0.0;
        double xc    = 0.0;
        double bc    = 0.0;
        double gc    = 0.0;
    }

    protected class RatioPhaseData {
        double a1     = 1.0;
        double angle1 = 0.0;
        double a2     = 1.0;
        double angle2 = 0.0;
    }

    protected class YShuntData {
        Complex ysh1 = Complex.ZERO;
        Complex ysh2 = Complex.ZERO;
    }

    protected class Ratio0Data {
        double a01 = 1.0;
        double a02 = 1.0;
    }

    protected class PhaseAngleClockData {
        double angle1 = 0.0;
        double angle2 = 0.0;
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
