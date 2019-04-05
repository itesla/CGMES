/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr2RatioPhaseMappingAlternative;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr2ShuntMappingAlternative;
import com.powsybl.cgmes.model.interpretation.XfmrUtilities.PhaseAngleClockData;
import com.powsybl.cgmes.model.interpretation.XfmrUtilities.Ratio0Data;
import com.powsybl.cgmes.model.interpretation.XfmrUtilities.RatioPhaseData;
import com.powsybl.cgmes.model.interpretation.XfmrUtilities.TapChangerData;
import com.powsybl.cgmes.model.interpretation.XfmrUtilities.YShuntData;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class Xfmr2Model {

    public Xfmr2Model(CgmesModel cgmes, PropertyBag transformer, CgmesEquipmentModelMapping config) {
        super();
        this.config = config;
        this.transformer = transformer;
        this.cgmes = cgmes;

        admittanceMatrix = new BranchAdmittanceMatrix();

        r1 = transformer.asDouble("r1", 0.0);
        x1 = transformer.asDouble("x1", 0.0);
        b1 = transformer.asDouble("b1", 0.0);
        g1 = transformer.asDouble("g1", 0.0);
        r2 = transformer.asDouble("r2", 0.0);
        x2 = transformer.asDouble("x2", 0.0);
        b2 = transformer.asDouble("b2", 0.0);
        g2 = transformer.asDouble("g2", 0.0);
    }

    public void interpret() {

        RatioPhaseData ratioPhaseData = getXfmr2RatioPhase(config);
        double a1 = ratioPhaseData.a1;
        double angle1 = ratioPhaseData.angle1;
        double a2 = ratioPhaseData.a2;
        double angle2 = ratioPhaseData.angle2;
        boolean tc1DifferentRatios = ratioPhaseData.tc1DifferentRatios;
        boolean ptc1DifferentAngles = ratioPhaseData.ptc1DifferentAngles;
        boolean tc2DifferentRatios = ratioPhaseData.tc2DifferentRatios;
        boolean ptc2DifferentAngles = ratioPhaseData.ptc2DifferentAngles;

        // yshunt
        YShuntData yShuntData = getXfmr2YShunt(config);
        Complex ysh1 = yShuntData.ysh1;
        Complex ysh2 = yShuntData.ysh2;

        // phaseAngleClock
        PhaseAngleClockData phaseAngleClockData = getXfmr2PhaseAngleClock(config);
        angle1 += phaseAngleClockData.angle1;
        angle2 += phaseAngleClockData.angle2;

        detectBranchModel(ysh1, ysh2, a1, angle1, a2, angle2, tc1DifferentRatios, ptc1DifferentAngles,
                tc2DifferentRatios, ptc2DifferentAngles);

        // add structural ratio after detected branch model
        Ratio0Data ratio0Data = getXfmr2Ratio0(config);
        double a01 = ratio0Data.a01;
        double a02 = ratio0Data.a02;
        a1 *= a01;
        a2 *= a02;

        // admittance
        admittanceMatrix.calculateAdmittance(r1 + r2, x1 + x2, a1, angle1, ysh1, a2, angle2, ysh2);
    }

    public DetectedBranchModel getBranchModel() {
        return branchModel;
    }

    public BranchAdmittanceMatrix getAdmittanceMatrix() {
        return admittanceMatrix;
    }

    private void detectBranchModel(Complex ysh1, Complex ysh2, double a1, double angle1, double a2, double angle2,
            boolean tc1DifferentRatios, boolean ptc1DifferentAngles, boolean tc2DifferentRatios,
            boolean ptc2DifferentAngles) {
        branchModel = new DetectedBranchModel(ysh1, ysh2, a1, angle1, a2, angle2, tc1DifferentRatios,
                ptc1DifferentAngles, tc2DifferentRatios, ptc2DifferentAngles);
    }

    private Ratio0Data getXfmr2Ratio0(CgmesEquipmentModelMapping config) {
        double rsvi1 = transformer.asDouble("rsvi1", 0.0);
        double ratedU1 = transformer.asDouble("ratedU1");
        double ratedU2 = transformer.asDouble("ratedU2");
        Xfmr2RatioPhaseMappingAlternative xfmr2Ratio0 = config.getXfmr2Ratio0();
        Ratio0Data ratio0Data = new Ratio0Data();
        switch (xfmr2Ratio0) {
            case END1:
                ratio0Data.a01 = ratedU1 / ratedU2;
                ratio0Data.a02 = ratedU2 / ratedU2;
                break;
            case END2:
                ratio0Data.a01 = ratedU1 / ratedU1;
                ratio0Data.a02 = ratedU2 / ratedU1;
                break;
            case RTC:
                if (rsvi1 != 0.0) {
                    ratio0Data.a01 = ratedU1 / ratedU2;
                    ratio0Data.a02 = ratedU2 / ratedU2;
                } else {
                    ratio0Data.a01 = ratedU1 / ratedU1;
                    ratio0Data.a02 = ratedU2 / ratedU1;
                }
                break;
            case X:
                if (x1 == 0.0) {
                    ratio0Data.a01 = ratedU1 / ratedU2;
                    ratio0Data.a02 = ratedU2 / ratedU2;
                } else {
                    ratio0Data.a01 = ratedU1 / ratedU1;
                    ratio0Data.a02 = ratedU2 / ratedU1;
                }
                break;
        }
        return ratio0Data;
    }

    private RatioPhaseData getXfmr2RatioPhase(CgmesEquipmentModelMapping config) {
        double rns1 = transformer.asDouble("rns1", 0.0);
        double rsvi1 = transformer.asDouble("rsvi1", 0.0);
        double rstep1 = transformer.asDouble("rstep1", 0.0);
        double rls1 = transformer.asDouble("rls1", 0.0);
        double rhs1 = transformer.asDouble("rhs1", 0.0);
        String ratioTapChangerTableName1 = transformer.get("RatioTapChangerTable1");
        PropertyBags ratioTapChangerTable1 = null;
        if (ratioTapChangerTableName1 != null) {
            ratioTapChangerTable1 = cgmes.ratioTapChangerTable(ratioTapChangerTableName1);
        }
        Xfmr2RatioPhaseMappingAlternative xfmr2RatioPhase = config.getXfmr2RatioPhase();
        RatioPhaseData ratioPhaseData = new RatioPhaseData();
        // ratio end1
        TapChangerData tapChangerData = XfmrUtilities.getRatioTapChanger(rstep1, rns1, rsvi1, ratioTapChangerTable1);
        double rtc1a = tapChangerData.rptca;
        double rtc1A = tapChangerData.rptcA;
        boolean rtc1TabularDifferentRatios = tapChangerData.tabularDifferentRatios;

        xfmr2ParametersCorrectionEnd1(tapChangerData);

        // phase end1
        double pns1 = transformer.asDouble("pns1", 0.0);
        double psvi1 = transformer.asDouble("psvi1", 0.0);
        double pstep1 = transformer.asDouble("pstep1", 0.0);
        double pls1 = transformer.asDouble("pls1", 0.0);
        double phs1 = transformer.asDouble("phs1", 0.0);
        double xStepMin1 = transformer.asDouble("xStepMin1", 0.0);
        double xStepMax1 = transformer.asDouble("xStepMax1", 0.0);
        double pwca1 = transformer.asDouble("pwca1", 0.0);
        double stepPhaseShiftIncrement1 = transformer.asDouble("pspsi1", 0.0);
        String ptype1 = transformer.get("ptype1");
        String phaseTapChangerTableName1 = transformer.get("PhaseTapChangerTable1");
        PropertyBags phaseTapChangerTable1 = null;
        if (phaseTapChangerTableName1 != null) {
            phaseTapChangerTable1 = cgmes.phaseTapChangerTable(phaseTapChangerTableName1);
        }
        tapChangerData = XfmrUtilities.getPhaseTapChanger(ptype1, pstep1, pls1, phs1, pns1, psvi1, pwca1,
                stepPhaseShiftIncrement1, phaseTapChangerTable1);
        double ptc1a = tapChangerData.rptca;
        double ptc1A = tapChangerData.rptcA;
        boolean ptc1TabularDifferentRatios = tapChangerData.tabularDifferentRatios;
        boolean ptc1TabularDifferentAngles = tapChangerData.tabularDifferentAngles;
        boolean ptc1AsymmetricalDifferentRatios = tapChangerData.asymmetricalDifferentRatios;
        x1 = XfmrUtilities.getX(x1, ptc1A, ptype1, pls1, phs1, pns1, psvi1, pwca1, stepPhaseShiftIncrement1, xStepMin1,
                xStepMax1);

        xfmr2ParametersCorrectionEnd1(tapChangerData);

        // ratio end2
        double rns2 = transformer.asDouble("rns2", 0.0);
        double rsvi2 = transformer.asDouble("rsvi2", 0.0);
        double rstep2 = transformer.asDouble("rstep2", 0.0);
        double rls2 = transformer.asDouble("rls2", 0.0);
        double rhs2 = transformer.asDouble("rhs2", 0.0);
        String ratioTapChangerTableName2 = transformer.get("RatioTapChangerTable2");
        PropertyBags ratioTapChangerTable2 = null;
        if (ratioTapChangerTableName2 != null) {
            ratioTapChangerTable2 = cgmes.ratioTapChangerTable(ratioTapChangerTableName2);
        }
        tapChangerData = XfmrUtilities.getRatioTapChanger(rstep2, rns2, rsvi2, ratioTapChangerTable2);
        double rtc2a = tapChangerData.rptca;
        double rtc2A = tapChangerData.rptcA;
        boolean rtc2TabularDifferentRatios = tapChangerData.tabularDifferentRatios;

        xfmr2ParametersCorrectionEnd2(tapChangerData);

        // phase end2
        double pns2 = transformer.asDouble("pns2", 0.0);
        double psvi2 = transformer.asDouble("psvi2", 0.0);
        double pstep2 = transformer.asDouble("pstep2", 0.0);
        double pls2 = transformer.asDouble("pls2", 0.0);
        double phs2 = transformer.asDouble("phs2", 0.0);
        double xStepMin2 = transformer.asDouble("xStepMin2", 0.0);
        double xStepMax2 = transformer.asDouble("xStepMax2", 0.0);
        double pwca2 = transformer.asDouble("pwca2", 0.0);
        double stepPhaseShiftIncrement2 = transformer.asDouble("pspsi2", 0.0);
        String ptype2 = transformer.get("ptype2");
        String phaseTapChangerTableName2 = transformer.get("PhaseTapChangerTable2");
        PropertyBags phaseTapChangerTable2 = null;
        if (phaseTapChangerTableName2 != null) {
            phaseTapChangerTable2 = cgmes.phaseTapChangerTable(phaseTapChangerTableName2);
        }
        tapChangerData = XfmrUtilities.getPhaseTapChanger(ptype2, pstep2, pls2, phs2, pns2, psvi2, pwca2,
                stepPhaseShiftIncrement2, phaseTapChangerTable2);
        double ptc2a = tapChangerData.rptca;
        double ptc2A = getXfmr2Ptc2Negate(config, tapChangerData.rptcA);
        boolean ptc2TabularDifferentRatios = tapChangerData.tabularDifferentRatios;
        boolean ptc2TabularDifferentAngles = tapChangerData.tabularDifferentAngles;
        boolean ptc2AsymmetricalDifferentRatios = tapChangerData.asymmetricalDifferentRatios;
        x2 = XfmrUtilities.getX(x2, ptc2A, ptype2, pls2, phs2, pns2, psvi2, pwca2, stepPhaseShiftIncrement2, xStepMin2,
                xStepMax2);

        xfmr2ParametersCorrectionEnd2(tapChangerData);

        boolean tc1DifferentRatios = XfmrUtilities.getXfmrDifferentRatios(rsvi1, rls1, rhs1, rtc1TabularDifferentRatios,
                ptc1TabularDifferentRatios, ptc1AsymmetricalDifferentRatios);
        boolean tc2DifferentRatios = XfmrUtilities.getXfmrDifferentRatios(rsvi2, rls2, rhs2, rtc2TabularDifferentRatios,
                ptc2TabularDifferentRatios, ptc2AsymmetricalDifferentRatios);
        boolean ptc1DifferentAngles = XfmrUtilities.getXfmrDifferentAngles(psvi1, stepPhaseShiftIncrement1, pls1, phs1,
                ptc1TabularDifferentAngles);
        boolean ptc2DifferentAngles = XfmrUtilities.getXfmrDifferentAngles(psvi2, stepPhaseShiftIncrement2, pls2, phs2,
                ptc2TabularDifferentAngles);

        switch (xfmr2RatioPhase) {
            case END1:
                ratioPhaseData.a1 = rtc1a * ptc1a * rtc2a * ptc2a;
                ratioPhaseData.angle1 = rtc1A + ptc1A + rtc2A + ptc2A;
                ratioPhaseData.tc1DifferentRatios = tc1DifferentRatios || tc2DifferentRatios;
                ratioPhaseData.ptc1DifferentAngles = ptc1DifferentAngles || ptc2DifferentAngles;
                break;
            case END2:
                ratioPhaseData.a2 = rtc1a * ptc1a * rtc2a * ptc2a;
                ratioPhaseData.angle2 = rtc1A + ptc1A + rtc2A + ptc2A;
                ratioPhaseData.tc2DifferentRatios = tc1DifferentRatios || tc2DifferentRatios;
                ratioPhaseData.ptc2DifferentAngles = ptc1DifferentAngles || ptc2DifferentAngles;
                break;
            case END1_END2:
                ratioPhaseData.a1 = rtc1a * ptc1a;
                ratioPhaseData.angle1 = rtc1A + ptc1A;
                ratioPhaseData.a2 = rtc2a * ptc2a;
                ratioPhaseData.angle2 = rtc2A + ptc2A;
                ratioPhaseData.tc1DifferentRatios = tc1DifferentRatios;
                ratioPhaseData.ptc1DifferentAngles = ptc1DifferentAngles;
                ratioPhaseData.tc2DifferentRatios = tc2DifferentRatios;
                ratioPhaseData.ptc2DifferentAngles = ptc2DifferentAngles;
                break;
            case X:
                if (x1 == 0.0) {
                    ratioPhaseData.a1 = rtc1a * ptc1a * rtc2a * ptc2a;
                    ratioPhaseData.angle1 = rtc1A + ptc1A + rtc2A + ptc2A;
                    ratioPhaseData.tc1DifferentRatios = tc1DifferentRatios || tc2DifferentRatios;
                    ratioPhaseData.ptc1DifferentAngles = ptc1DifferentAngles || ptc2DifferentAngles;
                } else {
                    ratioPhaseData.a2 = rtc1a * ptc1a * rtc2a * ptc2a;
                    ratioPhaseData.angle2 = rtc1A + ptc1A + rtc2A + ptc2A;
                    ratioPhaseData.tc2DifferentRatios = tc1DifferentRatios || tc2DifferentRatios;
                    ratioPhaseData.ptc2DifferentAngles = ptc1DifferentAngles || ptc2DifferentAngles;
                }
                break;
        }
        return ratioPhaseData;
    }

    private double getXfmr2Ptc2Negate(CgmesEquipmentModelMapping config, double angle) {

        double outAngle = angle;
        if (config.isXfmr2Ptc2Negate()) {
            outAngle = -angle;
        }
        return outAngle;
    }

    private YShuntData getXfmr2YShunt(CgmesEquipmentModelMapping config) {
        Xfmr2ShuntMappingAlternative xfmr2YShunt = config.getXfmr2YShunt();
        YShuntData yShuntData = new YShuntData();
        switch (xfmr2YShunt) {
            case END1:
                yShuntData.ysh1 = yShuntData.ysh1.add(new Complex(g1 + g2, b1 + b2));
                break;
            case END2:
                yShuntData.ysh2 = yShuntData.ysh2.add(new Complex(g1 + g2, b1 + b2));
                break;
            case END1_END2:
                yShuntData.ysh1 = yShuntData.ysh1.add(new Complex(g1, b1));
                yShuntData.ysh2 = yShuntData.ysh2.add(new Complex(g2, b2));
                break;
            case SPLIT:
                yShuntData.ysh1 = yShuntData.ysh1.add(new Complex((g1 + g2) * 0.5, (b1 + b2) * 0.5));
                yShuntData.ysh2 = yShuntData.ysh2.add(new Complex((g1 + g2) * 0.5, (b1 + b2) * 0.5));
                break;
        }
        return yShuntData;
    }

    private PhaseAngleClockData getXfmr2PhaseAngleClock(CgmesEquipmentModelMapping config) {
        int pac1 = transformer.asInt("pac1", 0);
        int pac2 = transformer.asInt("pac2", 0);
        PhaseAngleClockData phaseAngleClockData = new PhaseAngleClockData();
        switch (config.getXfmr2PhaseAngleClock()) {
            case END1_END2:
                if (pac1 != 0) {
                    phaseAngleClockData.angle1 = XfmrUtilities.getPhaseAngleClock(pac1);
                }
                if (pac2 != 0) {
                    phaseAngleClockData.angle2 = XfmrUtilities.getPhaseAngleClock(pac2);
                }
                if (config.isXfmr2Pac2Negate()) {
                    phaseAngleClockData.angle2 = -phaseAngleClockData.angle2;
                }
                break;
        }
        return phaseAngleClockData;
    }

    private void xfmr2ParametersCorrectionEnd1(TapChangerData tapChangerData) {
        double xc = tapChangerData.xc;
        double rc = tapChangerData.rc;
        double bc = tapChangerData.bc;
        double gc = tapChangerData.gc;

        if (x1 != 0.0) {
            x1 = XfmrUtilities.applyCorrection(x1, xc);
        } else {
            x2 = XfmrUtilities.applyCorrection(x2, xc);
        }
        if (r1 != 0.0) {
            r1 = XfmrUtilities.applyCorrection(r1, rc);
        } else {
            r2 = XfmrUtilities.applyCorrection(r2, rc);
        }
        if (b1 != 0.0) {
            b1 = XfmrUtilities.applyCorrection(b1, bc);
        } else {
            b2 = XfmrUtilities.applyCorrection(b2, bc);
        }
        if (g1 != 0.0) {
            g1 = XfmrUtilities.applyCorrection(g1, gc);
        } else {
            g2 = XfmrUtilities.applyCorrection(g2, gc);
        }
    }

    private void xfmr2ParametersCorrectionEnd2(TapChangerData tapChangerData) {
        double xc = tapChangerData.xc;
        double rc = tapChangerData.rc;
        double bc = tapChangerData.bc;
        double gc = tapChangerData.gc;

        if (x2 != 0.0) {
            x2 = XfmrUtilities.applyCorrection(x2, xc);
        } else {
            x1 = XfmrUtilities.applyCorrection(x1, xc);
        }
        if (r2 != 0.0) {
            r2 = XfmrUtilities.applyCorrection(r2, rc);
        } else {
            r1 = XfmrUtilities.applyCorrection(r1, rc);
        }
        if (b2 != 0.0) {
            b2 = XfmrUtilities.applyCorrection(b2, bc);
        } else {
            b1 = XfmrUtilities.applyCorrection(b1, bc);
        }
        if (g2 != 0.0) {
            g2 = XfmrUtilities.applyCorrection(g2, gc);
        } else {
            g1 = XfmrUtilities.applyCorrection(g1, gc);
        }
    }

    private final CgmesEquipmentModelMapping config;
    private final CgmesModel                 cgmes;
    private final PropertyBag                transformer;

    private double                           r1;
    private double                           x1;
    private double                           b1;
    private double                           g1;
    private double                           r2;
    private double                           x2;
    private double                           b2;
    private double                           g2;

    private BranchAdmittanceMatrix           admittanceMatrix;
    private DetectedBranchModel              branchModel;
}
