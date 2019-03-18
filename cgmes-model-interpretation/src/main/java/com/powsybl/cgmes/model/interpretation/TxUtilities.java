/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

/**
 * @author José Antonio Marqués <marquesja at aia.es>, Marcos de Miguel <demiguelm at aia.es>
 */
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

    protected double getSymmetricalAlphaMax(String ptype, double stepMin,
            double stepMax, double pns, double psvi, double stepPhaseShiftIncrement) {
        double alphaMax = 0.0;
        for (double step = stepMin; step <= stepMax; step++) {
            TapChangerData tapChangerData = getSymmetricalPhaseTapChangerData(ptype, step, pns, psvi,
                    stepPhaseShiftIncrement);
            if (tapChangerData.rptcA > alphaMax) {
                alphaMax = tapChangerData.rptcA;
            }
        }
        return alphaMax;
    }

    protected TapChangerData getSymmetricalPhaseTapChangerData(String ptype, double pstep,
            double pns, double psvi, double stepPhaseShiftIncrement) {
        TapChangerData tapChangerData = new TapChangerData();
        tapChangerData.rptca = 1.0;
        tapChangerData.rptcA = 0.0;
        if (stepPhaseShiftIncrement != 0.0) {
            tapChangerData.rptca = 1.0;
            tapChangerData.rptcA = (pstep - pns) * stepPhaseShiftIncrement;
        } else {
            double dy = (pstep - pns) * (psvi / 100.0);
            tapChangerData.rptca = 1.0;
            tapChangerData.rptcA = Math.toDegrees(2 * Math.asin(dy / 2));
        }
        return tapChangerData;
    }

    protected double getAsymmetricalAlphaMax(String ptype, double stepMin,
            double stepMax, double pns, double psvi, double pwca) {
        double alphaMax = 0.0;
        for (double step = stepMin; step <= stepMax; step++) {
            TapChangerData tapChangerData = getAsymmetricalPhaseTapChangerData(ptype, step, pns, psvi, pwca);
            if (tapChangerData.rptcA > alphaMax) {
                alphaMax = tapChangerData.rptcA;
            }
        }
        return alphaMax;
    }

    protected TapChangerData getAsymmetricalPhaseTapChangerData(String ptype, double pstep,
            double pns, double psvi, double pwca) {
        TapChangerData tapChangerData = new TapChangerData();
        tapChangerData.rptca = 1.0;
        tapChangerData.rptcA = 0.0;
        double dx = 1.0 + (pstep - pns) * (psvi / 100.0) * Math.cos(Math.toRadians(pwca));
        double dy = (pstep - pns) * (psvi / 100.0) * Math.sin(Math.toRadians(pwca));
        tapChangerData.rptca = Math.hypot(dx, dy);
        tapChangerData.rptcA = Math.toDegrees(Math.atan2(dy, dx));

        return tapChangerData;
    }

    protected double getSymmetricalX(double xStepMin, double xStepMax, double alphaDegrees, double alphaMaxDegrees) {
        double alpha = Math.toRadians(alphaDegrees);
        double alphaMax = Math.toRadians(alphaMaxDegrees);
        return xStepMin + (xStepMax - xStepMin) * Math.pow(Math.sin(alpha / 2) / Math.sin(alphaMax / 2), 2);
    }

    protected double getAsymmetricalX(double xStepMin, double xStepMax, double alphaDegrees, double alphaMaxDegrees,
            double pwcaDegrees) {
        double alpha = Math.toRadians(alphaDegrees);
        double alphaMax = Math.toRadians(alphaMaxDegrees);
        double pwca = Math.toRadians(pwcaDegrees);
        double numer = Math.sin(pwca) - Math.tan(alphaMax) * Math.cos(pwca);
        double denom = Math.sin(pwca) - Math.tan(alpha) * Math.cos(pwca);
        return xStepMin + (xStepMax - xStepMin) * Math.pow(Math.tan(alpha) / Math.tan(alphaMax) * numer / denom, 2);
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

    protected boolean getTxDifferentRatios(double rsvi, double rls, double rhs, boolean rtcTabularDifferentRatios,
            boolean ptcTabularDifferentRatios, boolean ptcAsymmetricalDifferentRatios) {
        if (rsvi != 0 && rls != rhs) {
            return true;
        }
        return rtcTabularDifferentRatios || ptcTabularDifferentRatios || ptcAsymmetricalDifferentRatios;
    }

    protected boolean getTxDifferentAngles(double psvi, double stepPhaseShiftIncrement, double pls, double phs,
            boolean ptcTabularDifferentAngles) {
        if (psvi != 0 && pls != phs) {
            return true;
        }
        if (stepPhaseShiftIncrement != 0 && pls != phs) {
            return true;
        }
        return ptcTabularDifferentAngles;
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
        double  a1                  = 1.0;
        double  angle1              = 0.0;
        double  a2                  = 1.0;
        double  angle2              = 0.0;
        boolean tc1DifferentRatios  = false;
        boolean ptc1DifferentAngles = false;
        boolean tc2DifferentRatios  = false;
        boolean ptc2DifferentAngles = false;
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
            .getLogger(AdmittanceMatrix.class);
}
