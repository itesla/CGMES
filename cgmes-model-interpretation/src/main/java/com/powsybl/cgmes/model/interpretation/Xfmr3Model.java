/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr3ShuntMappingAlternative;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr3PhaseAngleClockAlternative;
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
public class Xfmr3Model {

    public Xfmr3Model(CgmesModel cgmes, PropertyBag transformer, CgmesEquipmentModelMapping config) {
        super();
        this.config = config;
        this.transformer = transformer;
        this.cgmes = cgmes;

        admittanceMatrixEnd1 = new BranchAdmittanceMatrix();
        admittanceMatrixEnd2 = new BranchAdmittanceMatrix();
        admittanceMatrixEnd3 = new BranchAdmittanceMatrix();

        r1 = transformer.asDouble("r1", 0.0);
        x1 = transformer.asDouble("x1", 0.0);
        b1 = transformer.asDouble("b1", 0.0);
        g1 = transformer.asDouble("g1", 0.0);

        r2 = transformer.asDouble("r2", 0.0);
        x2 = transformer.asDouble("x2", 0.0);
        b2 = transformer.asDouble("b2", 0.0);
        g2 = transformer.asDouble("g2", 0.0);

        r3 = transformer.asDouble("r3", 0.0);
        x3 = transformer.asDouble("x3", 0.0);
        b3 = transformer.asDouble("b3", 0.0);
        g3 = transformer.asDouble("g3", 0.0);
    }

    public void interpret() {

        Xfmr3RatioPhaseData ratioPhaseData = getXfmr3RatioPhase(config);
        double a11 = ratioPhaseData.end1.a1;
        double angle11 = ratioPhaseData.end1.angle1;
        double a12 = ratioPhaseData.end1.a2;
        double angle12 = ratioPhaseData.end1.angle2;
        double a21 = ratioPhaseData.end2.a1;
        double angle21 = ratioPhaseData.end2.angle1;
        double a22 = ratioPhaseData.end2.a2;
        double angle22 = ratioPhaseData.end2.angle2;
        double a31 = ratioPhaseData.end3.a1;
        double angle31 = ratioPhaseData.end3.angle1;
        double a32 = ratioPhaseData.end3.a2;
        double angle32 = ratioPhaseData.end3.angle2;
        boolean tc11DifferentRatios = ratioPhaseData.end1.tc1DifferentRatios;
        boolean tc12DifferentRatios = ratioPhaseData.end1.tc2DifferentRatios;
        boolean ptc11DifferentAngles = ratioPhaseData.end1.ptc1DifferentAngles;
        boolean ptc12DifferentAngles = ratioPhaseData.end1.ptc2DifferentAngles;
        boolean tc21DifferentRatios = ratioPhaseData.end2.tc1DifferentRatios;
        boolean tc22DifferentRatios = ratioPhaseData.end2.tc2DifferentRatios;
        boolean ptc21DifferentAngles = ratioPhaseData.end2.ptc1DifferentAngles;
        boolean ptc22DifferentAngles = ratioPhaseData.end2.ptc2DifferentAngles;
        boolean tc31DifferentRatios = ratioPhaseData.end3.tc1DifferentRatios;
        boolean tc32DifferentRatios = ratioPhaseData.end3.tc2DifferentRatios;
        boolean ptc31DifferentAngles = ratioPhaseData.end3.ptc1DifferentAngles;
        boolean ptc32DifferentAngles = ratioPhaseData.end3.ptc2DifferentAngles;

        // yshunt
        Xfmr3YShuntData yShuntData = getXfmr3YShunt(config);
        Complex ysh11 = yShuntData.end1.ysh1;
        Complex ysh12 = yShuntData.end1.ysh2;
        Complex ysh21 = yShuntData.end2.ysh1;
        Complex ysh22 = yShuntData.end2.ysh2;
        Complex ysh31 = yShuntData.end3.ysh1;
        Complex ysh32 = yShuntData.end3.ysh2;

        // phaseAngleClock
        Xfmr3PhaseAngleClockData phaseAngleClockData = getXfmr3PhaseAngleClock(config);
        angle11 += phaseAngleClockData.end1.angle1;
        angle12 += phaseAngleClockData.end1.angle2;
        angle21 += phaseAngleClockData.end2.angle1;
        angle22 += phaseAngleClockData.end2.angle2;
        angle31 += phaseAngleClockData.end3.angle1;
        angle32 += phaseAngleClockData.end3.angle2;

        detectBranchModel(ysh11, ysh12, a11, angle11, a12, angle12, tc11DifferentRatios, ptc11DifferentAngles,
                tc12DifferentRatios, ptc12DifferentAngles, ysh21, ysh22, a21, angle21, a22, angle22,
                tc21DifferentRatios, ptc21DifferentAngles, tc22DifferentRatios, ptc22DifferentAngles, ysh31, ysh32, a31,
                angle31, a32, angle32, tc31DifferentRatios, ptc31DifferentAngles, tc32DifferentRatios,
                ptc32DifferentAngles);

        // add structural ratio after detected branch model
        double ratedU0 = 1.0;
        Xfmr3Ratio0Data ratio0Data = getXfmr3Ratio0(config, ratedU0);
        double a011 = ratio0Data.end1.a01;
        double a012 = ratio0Data.end1.a02;
        double a021 = ratio0Data.end2.a01;
        double a022 = ratio0Data.end2.a02;
        double a031 = ratio0Data.end3.a01;
        double a032 = ratio0Data.end3.a02;
        a11 *= a011;
        a12 *= a012;
        a21 *= a021;
        a22 *= a022;
        a31 *= a031;
        a32 *= a032;

        // admittance
        admittanceMatrixEnd1.calculateAdmittance(r1, x1, a11, angle11, ysh11, a12, angle12, ysh12);
        admittanceMatrixEnd2.calculateAdmittance(r2, x2, a21, angle21, ysh21, a22, angle22, ysh22);
        admittanceMatrixEnd3.calculateAdmittance(r3, x3, a31, angle31, ysh31, a32, angle32, ysh32);
    }

    public DetectedBranchModel getBranchModelEnd1() {
        return branchModelEnd1;
    }

    public DetectedBranchModel getBranchModelEnd2() {
        return branchModelEnd2;
    }

    public DetectedBranchModel getBranchModelEnd3() {
        return branchModelEnd3;
    }

    private void detectBranchModel(Complex ysh11, Complex ysh12, double a11, double angle11, double a12, double angle12,
            boolean tc11DifferentRatios, boolean ptc11DifferentAngles, boolean tc12DifferentRatios,
            boolean ptc12DifferentAngles, Complex ysh21, Complex ysh22, double a21, double angle21, double a22,
            double angle22, boolean tc21DifferentRatios, boolean ptc21DifferentAngles, boolean tc22DifferentRatios,
            boolean ptc22DifferentAngles, Complex ysh31, Complex ysh32, double a31, double angle31, double a32,
            double angle32, boolean tc31DifferentRatios, boolean ptc31DifferentAngles, boolean tc32DifferentRatios,
            boolean ptc32DifferentAngles) {
        branchModelEnd1 = new DetectedBranchModel(ysh11, ysh12, a11, angle11, a12, angle12, tc11DifferentRatios,
                ptc11DifferentAngles, tc12DifferentRatios, ptc12DifferentAngles);

        branchModelEnd2 = new DetectedBranchModel(ysh21, ysh22, a21, angle21, a22, angle22, tc21DifferentRatios,
                ptc21DifferentAngles, tc22DifferentRatios, ptc22DifferentAngles);

        branchModelEnd3 = new DetectedBranchModel(ysh31, ysh32, a31, angle31, a32, angle32, tc31DifferentRatios,
                ptc31DifferentAngles, tc32DifferentRatios, ptc32DifferentAngles);
    }

    public BranchAdmittanceMatrix getAdmittanceMatrixEnd1() {
        return admittanceMatrixEnd1;
    }

    public BranchAdmittanceMatrix getAdmittanceMatrixEnd2() {
        return admittanceMatrixEnd2;
    }

    public BranchAdmittanceMatrix getAdmittanceMatrixEnd3() {
        return admittanceMatrixEnd3;
    }

    private Xfmr3Ratio0Data getXfmr3Ratio0(CgmesEquipmentModelMapping config, double ratedU0) {
        double ratedU1 = transformer.asDouble("ratedU1");
        double ratedU2 = transformer.asDouble("ratedU2");
        double ratedU3 = transformer.asDouble("ratedU3");
        Xfmr3Ratio0Data ratio0Data = new Xfmr3Ratio0Data();
        switch (config.getXfmr3Ratio0StarBusSide()) {
            case NETWORK_SIDE:
                ratio0Data.end1.a01 = ratedU1 / ratedU0;
                ratio0Data.end1.a02 = ratedU0 / ratedU0;
                ratio0Data.end2.a01 = ratedU2 / ratedU0;
                ratio0Data.end2.a02 = ratedU0 / ratedU0;
                ratio0Data.end3.a01 = ratedU3 / ratedU0;
                ratio0Data.end3.a02 = ratedU0 / ratedU0;
                break;
            case STAR_BUS_SIDE:
                ratio0Data.end1.a01 = ratedU1 / ratedU1;
                ratio0Data.end1.a02 = ratedU0 / ratedU1;
                ratio0Data.end2.a01 = ratedU2 / ratedU2;
                ratio0Data.end2.a02 = ratedU0 / ratedU2;
                ratio0Data.end3.a01 = ratedU3 / ratedU3;
                ratio0Data.end3.a02 = ratedU0 / ratedU3;
                break;
        }
        return ratio0Data;
    }

    private Xfmr3RatioPhaseData getXfmr3RatioPhase(CgmesEquipmentModelMapping config) {

        Xfmr3RatioPhaseData ratioPhaseData = new Xfmr3RatioPhaseData();
        // ratio end1
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
        TapChangerData tapChangerData = XfmrUtilities.getRatioTapChanger(rstep1, rns1, rsvi1, ratioTapChangerTable1);
        double rtc1a = tapChangerData.rptca;
        double rtc1A = tapChangerData.rptcA;
        boolean rtc1TabularDifferentRatios = tapChangerData.tabularDifferentRatios;

        xfmr3ParametersCorrectionEnd1(tapChangerData);

        // phase end1
        double pns1 = transformer.asDouble("pns1", 0.0);
        double psvi1 = transformer.asDouble("psvi1", 0.0);
        double pstep1 = transformer.asDouble("pstep1", 0.0);
        double pls1 = transformer.asDouble("pls1", 0.0);
        double phs1 = transformer.asDouble("phs1", 0.0);
        double xStepMin1 = transformer.asDouble("xStepMin1", 0.0);
        double xStepMax1 = transformer.asDouble("xStepMax1", 0.0);
        double pwca1 = transformer.asDouble("pwca1", 0.0);
        double stepPhaseShiftIncrement1 = transformer.asDouble("stepPhaseShiftIncrement1", 0.0);
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

        xfmr3ParametersCorrectionEnd1(tapChangerData);

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

        xfmr3ParametersCorrectionEnd2(tapChangerData);

        // phase end2
        double pns2 = transformer.asDouble("pns2", 0.0);
        double psvi2 = transformer.asDouble("psvi2", 0.0);
        double pstep2 = transformer.asDouble("pstep2", 0.0);
        double pls2 = transformer.asDouble("pls2", 0.0);
        double phs2 = transformer.asDouble("phs2", 0.0);
        double xStepMin2 = transformer.asDouble("xStepMin2", 0.0);
        double xStepMax2 = transformer.asDouble("xStepMax2", 0.0);
        double pwca2 = transformer.asDouble("pwca2", 0.0);
        double stepPhaseShiftIncrement2 = transformer.asDouble("stepPhaseShiftIncrement2", 0.0);
        String ptype2 = transformer.get("ptype2");
        String phaseTapChangerTableName2 = transformer.get("PhaseTapChangerTable2");
        PropertyBags phaseTapChangerTable2 = null;
        if (phaseTapChangerTableName2 != null) {
            phaseTapChangerTable2 = cgmes.phaseTapChangerTable(phaseTapChangerTableName2);
        }
        tapChangerData = XfmrUtilities.getPhaseTapChanger(ptype2, pstep2, pls2, phs2, pns2, psvi2, pwca2,
                stepPhaseShiftIncrement2, phaseTapChangerTable2);
        double ptc2a = tapChangerData.rptca;
        double ptc2A = tapChangerData.rptcA;
        boolean ptc2TabularDifferentRatios = tapChangerData.tabularDifferentRatios;
        boolean ptc2TabularDifferentAngles = tapChangerData.tabularDifferentAngles;
        boolean ptc2AsymmetricalDifferentRatios = tapChangerData.asymmetricalDifferentRatios;
        x2 = XfmrUtilities.getX(x2, ptc2A, ptype2, pls2, phs2, pns2, psvi2, pwca2, stepPhaseShiftIncrement2, xStepMin2,
                xStepMax2);

        xfmr3ParametersCorrectionEnd2(tapChangerData);

        // ratio end3
        double rns3 = transformer.asDouble("rns3", 0.0);
        double rsvi3 = transformer.asDouble("rsvi3", 0.0);
        double rstep3 = transformer.asDouble("rstep3", 0.0);
        double rls3 = transformer.asDouble("rls3", 0.0);
        double rhs3 = transformer.asDouble("rhs3", 0.0);
        String ratioTapChangerTableName3 = transformer.get("RatioTapChangerTable3");
        PropertyBags ratioTapChangerTable3 = null;
        if (ratioTapChangerTableName3 != null) {
            ratioTapChangerTable3 = cgmes.ratioTapChangerTable(ratioTapChangerTableName3);
        }
        tapChangerData = XfmrUtilities.getRatioTapChanger(rstep3, rns3, rsvi3, ratioTapChangerTable3);
        double rtc3a = tapChangerData.rptca;
        double rtc3A = tapChangerData.rptcA;
        boolean rtc3TabularDifferentRatios = tapChangerData.tabularDifferentRatios;

        xfmr3ParametersCorrectionEnd3(tapChangerData);

        // phase end3
        double pns3 = transformer.asDouble("pns3", 0.0);
        double psvi3 = transformer.asDouble("psvi3", 0.0);
        double pstep3 = transformer.asDouble("pstep3", 0.0);
        double pls3 = transformer.asDouble("pls3", 0.0);
        double phs3 = transformer.asDouble("phs3", 0.0);
        double xStepMin3 = transformer.asDouble("xStepMin3", 0.0);
        double xStepMax3 = transformer.asDouble("xStepMax3", 0.0);
        double pwca3 = transformer.asDouble("pwca3", 0.0);
        double stepPhaseShiftIncrement3 = transformer.asDouble("stepPhaseShiftIncrement3", 0.0);
        String ptype3 = transformer.get("ptype3");
        String phaseTapChangerTableName3 = transformer.get("PhaseTapChangerTable3");
        PropertyBags phaseTapChangerTable3 = null;
        if (phaseTapChangerTableName3 != null) {
            phaseTapChangerTable3 = cgmes.phaseTapChangerTable(phaseTapChangerTableName3);
        }
        tapChangerData = XfmrUtilities.getPhaseTapChanger(ptype3, pstep3, pls3, phs3, pns3, psvi3, pwca3,
                stepPhaseShiftIncrement3, phaseTapChangerTable3);
        double ptc3a = tapChangerData.rptca;
        double ptc3A = tapChangerData.rptcA;
        boolean ptc3TabularDifferentRatios = tapChangerData.tabularDifferentRatios;
        boolean ptc3TabularDifferentAngles = tapChangerData.tabularDifferentAngles;
        boolean ptc3AsymmetricalDifferentRatios = tapChangerData.asymmetricalDifferentRatios;
        x3 = XfmrUtilities.getX(x3, ptc3A, ptype3, pls3, phs3, pns3, psvi3, pwca3, stepPhaseShiftIncrement3, xStepMin3,
                xStepMax3);

        xfmr3ParametersCorrectionEnd3(tapChangerData);

        boolean tc1DifferentRatios = XfmrUtilities.getXfmrDifferentRatios(rsvi1, rls1, rhs1, rtc1TabularDifferentRatios,
                ptc1TabularDifferentRatios, ptc1AsymmetricalDifferentRatios);
        boolean tc2DifferentRatios = XfmrUtilities.getXfmrDifferentRatios(rsvi2, rls2, rhs2, rtc2TabularDifferentRatios,
                ptc2TabularDifferentRatios, ptc2AsymmetricalDifferentRatios);
        boolean tc3DifferentRatios = XfmrUtilities.getXfmrDifferentRatios(rsvi3, rls3, rhs3, rtc3TabularDifferentRatios,
                ptc3TabularDifferentRatios, ptc3AsymmetricalDifferentRatios);
        boolean ptc1DifferentAngles = XfmrUtilities.getXfmrDifferentAngles(psvi1, stepPhaseShiftIncrement1, pls1, phs1,
                ptc1TabularDifferentAngles);
        boolean ptc2DifferentAngles = XfmrUtilities.getXfmrDifferentAngles(psvi2, stepPhaseShiftIncrement2, pls2, phs2,
                ptc2TabularDifferentAngles);
        boolean ptc3DifferentAngles = XfmrUtilities.getXfmrDifferentAngles(psvi3, stepPhaseShiftIncrement3, pls3, phs3,
                ptc3TabularDifferentAngles);

        switch (config.getXfmr3RatioPhaseStarBusSide()) {
            case NETWORK_SIDE:
                ratioPhaseData.end1.a1 = rtc1a * ptc1a;
                ratioPhaseData.end1.angle1 = rtc1A + ptc1A;
                ratioPhaseData.end1.tc1DifferentRatios = tc1DifferentRatios;
                ratioPhaseData.end1.ptc1DifferentAngles = ptc1DifferentAngles;
                ratioPhaseData.end2.a1 = rtc2a * ptc2a;
                ratioPhaseData.end2.angle1 = rtc2A + ptc2A;
                ratioPhaseData.end2.tc1DifferentRatios = tc2DifferentRatios;
                ratioPhaseData.end2.ptc1DifferentAngles = ptc2DifferentAngles;
                ratioPhaseData.end3.a1 = rtc3a * ptc3a;
                ratioPhaseData.end3.angle1 = rtc3A + ptc3A;
                ratioPhaseData.end3.tc1DifferentRatios = tc3DifferentRatios;
                ratioPhaseData.end3.ptc1DifferentAngles = ptc3DifferentAngles;
                break;
            case STAR_BUS_SIDE:
                ratioPhaseData.end1.a2 = rtc1a * ptc1a;
                ratioPhaseData.end1.angle2 = rtc1A + ptc1A;
                ratioPhaseData.end1.tc2DifferentRatios = tc1DifferentRatios;
                ratioPhaseData.end1.ptc2DifferentAngles = ptc1DifferentAngles;
                ratioPhaseData.end2.a2 = rtc2a * ptc2a;
                ratioPhaseData.end2.angle2 = rtc2A + ptc2A;
                ratioPhaseData.end2.tc2DifferentRatios = tc2DifferentRatios;
                ratioPhaseData.end2.ptc2DifferentAngles = ptc2DifferentAngles;
                ratioPhaseData.end3.a2 = rtc3a * ptc3a;
                ratioPhaseData.end3.angle2 = rtc3A + ptc3A;
                ratioPhaseData.end3.tc2DifferentRatios = tc3DifferentRatios;
                ratioPhaseData.end3.ptc2DifferentAngles = ptc3DifferentAngles;
                break;
        }

        return ratioPhaseData;
    }

    private Xfmr3YShuntData getXfmr3YShunt(CgmesEquipmentModelMapping config) {
        Xfmr3ShuntMappingAlternative xfmr3YShunt = config.getXfmr3YShunt();
        Xfmr3YShuntData yShuntData = new Xfmr3YShuntData();
        switch (xfmr3YShunt) {
            case NETWORK_SIDE:
                yShuntData.end1.ysh1 = yShuntData.end1.ysh1.add(new Complex(g1, b1));
                yShuntData.end2.ysh1 = yShuntData.end2.ysh1.add(new Complex(g2, b2));
                yShuntData.end3.ysh1 = yShuntData.end3.ysh1.add(new Complex(g3, b3));
                break;
            case STAR_BUS_SIDE:
                yShuntData.end1.ysh2 = yShuntData.end1.ysh2.add(new Complex(g1, b1));
                yShuntData.end2.ysh2 = yShuntData.end2.ysh2.add(new Complex(g2, b2));
                yShuntData.end3.ysh2 = yShuntData.end3.ysh2.add(new Complex(g3, b3));
                break;
            case SPLIT:
                yShuntData.end1.ysh1 = yShuntData.end1.ysh1.add(new Complex(g1 * 0.5, b1 * 0.5));
                yShuntData.end2.ysh1 = yShuntData.end2.ysh1.add(new Complex(g2 * 0.5, b2 * 0.5));
                yShuntData.end3.ysh1 = yShuntData.end3.ysh1.add(new Complex(g3 * 0.5, b3 * 0.5));
                yShuntData.end1.ysh2 = yShuntData.end1.ysh2.add(new Complex(g1 * 0.5, b1 * 0.5));
                yShuntData.end2.ysh2 = yShuntData.end2.ysh2.add(new Complex(g2 * 0.5, b2 * 0.5));
                yShuntData.end3.ysh2 = yShuntData.end3.ysh2.add(new Complex(g3 * 0.5, b3 * 0.5));
                break;
        }
        return yShuntData;
    }

    private Xfmr3PhaseAngleClockData getXfmr3PhaseAngleClock(CgmesEquipmentModelMapping config) {
        int pac1 = transformer.asInt("pac1", 0);
        int pac2 = transformer.asInt("pac2", 0);
        int pac3 = transformer.asInt("pac3", 0);
        Xfmr3PhaseAngleClockAlternative xfmr3PhaseAngleClock = config.getXfmr3PhaseAngleClock();
        Xfmr3PhaseAngleClockData phaseAngleClockData = new Xfmr3PhaseAngleClockData();
        switch (xfmr3PhaseAngleClock) {
            case STAR_BUS_SIDE:
                if (pac1 != 0) {
                    phaseAngleClockData.end1.angle2 = XfmrUtilities.getPhaseAngleClock(pac1);
                }
                if (pac2 != 0) {
                    phaseAngleClockData.end2.angle2 = XfmrUtilities.getPhaseAngleClock(pac2);
                }
                if (pac3 != 0) {
                    phaseAngleClockData.end3.angle2 = XfmrUtilities.getPhaseAngleClock(pac3);
                }
                break;
            case NETWORK_SIDE:
                if (pac1 != 0) {
                    phaseAngleClockData.end1.angle1 = XfmrUtilities.getPhaseAngleClock(pac1);
                }
                if (pac2 != 0) {
                    phaseAngleClockData.end2.angle1 = XfmrUtilities.getPhaseAngleClock(pac2);
                }
                if (pac3 != 0) {
                    phaseAngleClockData.end3.angle1 = XfmrUtilities.getPhaseAngleClock(pac3);
                }
                break;
        }
        return phaseAngleClockData;
    }

    private void xfmr3ParametersCorrectionEnd1(TapChangerData tapChangerData) {
        double xc = tapChangerData.xc;
        double rc = tapChangerData.rc;
        double bc = tapChangerData.bc;
        double gc = tapChangerData.gc;

        x1 = XfmrUtilities.applyCorrection(x1, xc);
        r1 = XfmrUtilities.applyCorrection(r1, rc);
        b1 = XfmrUtilities.applyCorrection(b1, bc);
        g1 = XfmrUtilities.applyCorrection(g1, gc);
    }

    private void xfmr3ParametersCorrectionEnd2(TapChangerData tapChangerData) {
        double xc = tapChangerData.xc;
        double rc = tapChangerData.rc;
        double bc = tapChangerData.bc;
        double gc = tapChangerData.gc;

        x2 = XfmrUtilities.applyCorrection(x2, xc);
        r2 = XfmrUtilities.applyCorrection(r2, rc);
        b2 = XfmrUtilities.applyCorrection(b2, bc);
        g2 = XfmrUtilities.applyCorrection(g2, gc);
    }

    private void xfmr3ParametersCorrectionEnd3(TapChangerData tapChangerData) {
        double xc = tapChangerData.xc;
        double rc = tapChangerData.rc;
        double bc = tapChangerData.bc;
        double gc = tapChangerData.gc;

        x3 = XfmrUtilities.applyCorrection(x3, xc);
        r3 = XfmrUtilities.applyCorrection(r3, rc);
        b3 = XfmrUtilities.applyCorrection(b3, bc);
        g3 = XfmrUtilities.applyCorrection(g3, gc);
    }

    static class Xfmr3RatioPhaseData {
        RatioPhaseData end1 = new RatioPhaseData();
        RatioPhaseData end2 = new RatioPhaseData();
        RatioPhaseData end3 = new RatioPhaseData();
    }

    static class Xfmr3YShuntData {
        YShuntData end1 = new YShuntData();
        YShuntData end2 = new YShuntData();
        YShuntData end3 = new YShuntData();
    }

    static class Xfmr3Ratio0Data {
        Ratio0Data end1 = new Ratio0Data();
        Ratio0Data end2 = new Ratio0Data();
        Ratio0Data end3 = new Ratio0Data();
    }

    static class Xfmr3PhaseAngleClockData {
        PhaseAngleClockData end1 = new PhaseAngleClockData();
        PhaseAngleClockData end2 = new PhaseAngleClockData();
        PhaseAngleClockData end3 = new PhaseAngleClockData();
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
    private double                           r3;
    private double                           x3;
    private double                           b3;
    private double                           g3;

    private BranchAdmittanceMatrix           admittanceMatrixEnd1;
    private BranchAdmittanceMatrix           admittanceMatrixEnd2;
    private BranchAdmittanceMatrix           admittanceMatrixEnd3;
    private DetectedBranchModel              branchModelEnd1;
    private DetectedBranchModel              branchModelEnd2;
    private DetectedBranchModel              branchModelEnd3;
}
