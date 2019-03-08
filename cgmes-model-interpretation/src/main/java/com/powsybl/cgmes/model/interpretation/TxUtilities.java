package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

class TxUtilities {

    public TxUtilities(CgmesModel cgmes) {
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
                tapChangerData.rptca = getDoublePoint(point, "ratio", 1.0);
                tapChangerData.xc = getDoublePoint(point, "x", 0.0);
                tapChangerData.rc = getDoublePoint(point, "r", 0.0);
                tapChangerData.bc = getDoublePoint(point, "b", 0.0);
                tapChangerData.gc = getDoublePoint(point, "g", 0.0);
                return tapChangerData;
            }
        }

        return tapChangerData;
    }

    private double getDoublePoint(PropertyBag point, String parameter, double defaultValue) {
        double value = point.asDouble(parameter, defaultValue);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return defaultValue;
        }
        return value;
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
                tapChangerData.rptca = getDoublePoint(point, "ratio", 1.0);
                tapChangerData.rptcA = getDoublePoint(point, "angle", 0.0);

                tapChangerData.xc = getDoublePoint(point, "x", 0.0);
                tapChangerData.rc = getDoublePoint(point, "r", 0.0);
                tapChangerData.bc = getDoublePoint(point, "b", 0.0);
                tapChangerData.gc = getDoublePoint(point, "g", 0.0);
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

    static class TapChangerData {
        double rptca = 1.0;
        double rptcA = 0.0;
        double rc    = 0.0;
        double xc    = 0.0;
        double bc    = 0.0;
        double gc    = 0.0;
    }

    static class RatioPhaseData {
        double a1     = 1.0;
        double angle1 = 0.0;
        double a2     = 1.0;
        double angle2 = 0.0;
    }

    static class YShuntData {
        Complex ysh1 = Complex.ZERO;
        Complex ysh2 = Complex.ZERO;
    }

    static class Ratio0Data {
        double a01 = 1.0;
        double a02 = 1.0;
    }

    static class PhaseAngleClockData {
        double angle1 = 0.0;
        double angle2 = 0.0;
    }

    protected CgmesModel          cgmes;
    protected static final Logger LOG = LoggerFactory
            .getLogger(AbstractAdmittanceMatrix.class);
}
