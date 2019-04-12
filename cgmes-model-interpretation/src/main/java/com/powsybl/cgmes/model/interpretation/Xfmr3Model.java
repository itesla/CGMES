/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr3PhaseAngleClockAlternative;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr3RatioPhaseMappingAlternative;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr3ShuntMappingAlternative;
import com.powsybl.cgmes.model.interpretation.XfmrUtilities.PhaseAngleClockData;
import com.powsybl.cgmes.model.interpretation.XfmrUtilities.PhaseData;
import com.powsybl.cgmes.model.interpretation.XfmrUtilities.Ratio0Data;
import com.powsybl.cgmes.model.interpretation.XfmrUtilities.RatioData;
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
        RatioData ratio11 = ratioPhaseData.end1.ratio1;
        PhaseData phase11 = ratioPhaseData.end1.phase1;
        RatioData ratio12 = ratioPhaseData.end1.ratio2;
        PhaseData phase12 = ratioPhaseData.end1.phase2;
        RatioData ratio21 = ratioPhaseData.end2.ratio1;
        PhaseData phase21 = ratioPhaseData.end2.phase1;
        RatioData ratio22 = ratioPhaseData.end2.ratio2;
        PhaseData phase22 = ratioPhaseData.end2.phase2;
        RatioData ratio31 = ratioPhaseData.end3.ratio1;
        PhaseData phase31 = ratioPhaseData.end3.phase1;
        RatioData ratio32 = ratioPhaseData.end3.ratio2;
        PhaseData phase32 = ratioPhaseData.end3.phase2;

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
        phase11.angle += phaseAngleClockData.end1.angle1;
        phase12.angle += phaseAngleClockData.end1.angle2;
        phase21.angle += phaseAngleClockData.end2.angle1;
        phase22.angle += phaseAngleClockData.end2.angle2;
        phase31.angle += phaseAngleClockData.end3.angle1;
        phase32.angle += phaseAngleClockData.end3.angle2;

        detectBranchModel(ysh11, ysh12, ratio11, phase11, ratio12, phase12,
                ysh21, ysh22, ratio21, phase21, ratio22, phase22,
                ysh31, ysh32, ratio31, phase31, ratio32, phase32);

        double a11 = ratio11.a * phase11.a;
        double a12 = ratio12.a * phase12.a;
        double a21 = ratio21.a * phase21.a;
        double a22 = ratio22.a * phase22.a;
        double a31 = ratio31.a * phase31.a;
        double a32 = ratio32.a * phase32.a;

        // add structural ratio after detected branch model
        double ratedU0 = 1.0;
        Xfmr3Ratio0Data ratio0Data = getXfmr3Ratio0(config, ratedU0);
        a11 *= ratio0Data.end1.a01;
        a12 *= ratio0Data.end1.a02;
        a21 *= ratio0Data.end2.a01;
        a22 *= ratio0Data.end2.a02;
        a31 *= ratio0Data.end3.a01;
        a32 *= ratio0Data.end3.a02;

        // admittance
        admittanceMatrixEnd1.calculateAdmittance(r1, x1, a11, phase11.angle, ysh11, a12, phase12.angle, ysh12);
        admittanceMatrixEnd2.calculateAdmittance(r2, x2, a21, phase21.angle, ysh21, a22, phase22.angle, ysh22);
        admittanceMatrixEnd3.calculateAdmittance(r3, x3, a31, phase31.angle, ysh31, a32, phase32.angle, ysh32);
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

    private void detectBranchModel(Complex ysh11, Complex ysh12, RatioData ratio11, PhaseData phase11,
            RatioData ratio12, PhaseData phase12,
            Complex ysh21, Complex ysh22, RatioData ratio21, PhaseData phase21,
            RatioData ratio22, PhaseData phase22,
            Complex ysh31, Complex ysh32, RatioData ratio31, PhaseData phase31,
            RatioData ratio32, PhaseData phase32) {
        branchModelEnd1 = new DetectedBranchModel(ysh11, ysh12, ratio11, phase11, ratio12, phase12);
        branchModelEnd2 = new DetectedBranchModel(ysh21, ysh22, ratio21, phase21, ratio22, phase22);
        branchModelEnd3 = new DetectedBranchModel(ysh31, ysh32, ratio31, phase31, ratio32, phase32);
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
        Xfmr3RatioPhaseMappingAlternative xfmr3Ratio0StarBusSide = config.getXfmr3Ratio0StarBusSide();
        switch (xfmr3Ratio0StarBusSide) {
            case STAR_BUS_SIDE:
                ratio0Data.end1.a01 = ratedU1 / ratedU1;
                ratio0Data.end1.a02 = ratedU0 / ratedU1;
                ratio0Data.end2.a01 = ratedU2 / ratedU2;
                ratio0Data.end2.a02 = ratedU0 / ratedU2;
                ratio0Data.end3.a01 = ratedU3 / ratedU3;
                ratio0Data.end3.a02 = ratedU0 / ratedU3;
                break;
            case NETWORK_SIDE:
                ratio0Data.end1.a01 = ratedU1 / ratedU0;
                ratio0Data.end1.a02 = ratedU0 / ratedU0;
                ratio0Data.end2.a01 = ratedU2 / ratedU0;
                ratio0Data.end2.a02 = ratedU0 / ratedU0;
                ratio0Data.end3.a01 = ratedU3 / ratedU0;
                ratio0Data.end3.a02 = ratedU0 / ratedU0;
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
        double stepPhaseShiftIncrement3 = transformer.asDouble("pspsi3", 0.0);
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

        boolean rtc1DifferentRatios = XfmrUtilities.getXfmrDifferentRatios(rsvi1, rls1, rhs1,
                rtc1TabularDifferentRatios);
        boolean rtc2DifferentRatios = XfmrUtilities.getXfmrDifferentRatios(rsvi2, rls2, rhs2,
                rtc2TabularDifferentRatios);
        boolean rtc3DifferentRatios = XfmrUtilities.getXfmrDifferentRatios(rsvi3, rls3, rhs3,
                rtc3TabularDifferentRatios);
        boolean ptc1DifferentRatiosAngles = XfmrUtilities.getXfmrDifferentAngles(psvi1, stepPhaseShiftIncrement1,
                pls1, phs1, ptc1TabularDifferentRatios, ptc1AsymmetricalDifferentRatios,
                ptc1TabularDifferentAngles);
        boolean ptc2DifferentRatiosAngles = XfmrUtilities.getXfmrDifferentAngles(psvi2, stepPhaseShiftIncrement2,
                pls2, phs2, ptc2TabularDifferentRatios, ptc2AsymmetricalDifferentRatios,
                ptc2TabularDifferentAngles);
        boolean ptc3DifferentRatiosAngles = XfmrUtilities.getXfmrDifferentAngles(psvi3, stepPhaseShiftIncrement3,
                pls3, phs3, ptc3TabularDifferentRatios, ptc3AsymmetricalDifferentRatios,
                ptc3TabularDifferentAngles);

        boolean rtc1RegulatingControl = transformer.asBoolean("ratioRegulatingControlEnabled1", false);
        boolean ptc1RegulatingControl = transformer.asBoolean("phaseRegulatingControlEnabled1", false);
        boolean rtc2RegulatingControl = transformer.asBoolean("ratioRegulatingControlEnabled2", false);
        boolean ptc2RegulatingControl = transformer.asBoolean("phaseRegulatingControlEnabled2", false);
        boolean rtc3RegulatingControl = transformer.asBoolean("ratioRegulatingControlEnabled3", false);
        boolean ptc3RegulatingControl = transformer.asBoolean("phaseRegulatingControlEnabled3", false);

        // network side always at end1
        Xfmr3RatioPhaseMappingAlternative xfmr3RatioPhaseStarBusSide = config.getXfmr3RatioPhaseStarBusSide();
        switch (xfmr3RatioPhaseStarBusSide) {
            case STAR_BUS_SIDE:
                ratioPhaseData.end1.ratio2.a = rtc1a;
                ratioPhaseData.end1.ratio2.regulatingControl = rtc1RegulatingControl;
                ratioPhaseData.end1.ratio2.changeable = rtc1DifferentRatios;
                ratioPhaseData.end1.phase2.a = ptc1a;
                ratioPhaseData.end1.phase2.angle = ptc1A;
                ratioPhaseData.end1.phase2.regulatingControl = ptc1RegulatingControl;
                ratioPhaseData.end1.phase2.changeable = ptc1DifferentRatiosAngles;
                ratioPhaseData.end2.ratio2.a = rtc2a;
                ratioPhaseData.end2.ratio2.regulatingControl = rtc2RegulatingControl;
                ratioPhaseData.end2.ratio2.changeable = rtc2DifferentRatios;
                ratioPhaseData.end2.phase2.a = ptc2a;
                ratioPhaseData.end2.phase2.angle = ptc2A;
                ratioPhaseData.end2.phase2.regulatingControl = ptc2RegulatingControl;
                ratioPhaseData.end2.phase2.changeable = ptc2DifferentRatiosAngles;
                ratioPhaseData.end3.ratio2.a = rtc3a;
                ratioPhaseData.end3.ratio2.regulatingControl = rtc3RegulatingControl;
                ratioPhaseData.end3.ratio2.changeable = rtc3DifferentRatios;
                ratioPhaseData.end3.phase2.a = ptc3a;
                ratioPhaseData.end3.phase2.angle = ptc3A;
                ratioPhaseData.end3.phase2.regulatingControl = ptc3RegulatingControl;
                ratioPhaseData.end3.phase2.changeable = ptc3DifferentRatiosAngles;
                break;
            case NETWORK_SIDE:
                ratioPhaseData.end1.ratio1.a = rtc1a;
                ratioPhaseData.end1.ratio1.regulatingControl = rtc1RegulatingControl;
                ratioPhaseData.end1.ratio1.changeable = rtc1DifferentRatios;
                ratioPhaseData.end1.phase1.a = ptc1a;
                ratioPhaseData.end1.phase1.angle = ptc1A;
                ratioPhaseData.end1.phase1.regulatingControl = ptc1RegulatingControl;
                ratioPhaseData.end1.phase1.changeable = ptc1DifferentRatiosAngles;
                ratioPhaseData.end2.ratio1.a = rtc2a;
                ratioPhaseData.end2.ratio1.regulatingControl = rtc2RegulatingControl;
                ratioPhaseData.end2.ratio1.changeable = rtc2DifferentRatios;
                ratioPhaseData.end2.phase1.a = ptc2a;
                ratioPhaseData.end2.phase1.angle = ptc2A;
                ratioPhaseData.end2.phase1.regulatingControl = ptc2RegulatingControl;
                ratioPhaseData.end2.phase1.changeable = ptc2DifferentRatiosAngles;
                ratioPhaseData.end3.ratio1.a = rtc3a;
                ratioPhaseData.end3.ratio1.regulatingControl = rtc3RegulatingControl;
                ratioPhaseData.end3.ratio1.changeable = rtc3DifferentRatios;
                ratioPhaseData.end3.phase1.a = ptc3a;
                ratioPhaseData.end3.phase1.angle = ptc3A;
                ratioPhaseData.end3.phase1.regulatingControl = ptc3RegulatingControl;
                ratioPhaseData.end3.phase1.changeable = ptc3DifferentRatiosAngles;
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
