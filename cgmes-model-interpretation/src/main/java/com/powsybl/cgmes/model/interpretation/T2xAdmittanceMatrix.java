/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.EndDistribution;
import com.powsybl.cgmes.model.interpretation.TxUtilities.PhaseAngleClockData;
import com.powsybl.cgmes.model.interpretation.TxUtilities.Ratio0Data;
import com.powsybl.cgmes.model.interpretation.TxUtilities.RatioPhaseData;
import com.powsybl.cgmes.model.interpretation.TxUtilities.TapChangerData;
import com.powsybl.cgmes.model.interpretation.TxUtilities.YShuntData;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author José Antonio Marqués <marquesja at aia.es>, Marcos de Miguel <demiguelm at aia.es>
 */
// XXX LUMA Review Do not like the T2x abbreviation
// XXX LUMA Review According to the attributes in the class, it is not an admittance matrix, is a 2-winding tx model, with all its data, and eventually it may calculate an admittanceMatrix
public class T2xAdmittanceMatrix extends AdmittanceMatrix {

    public T2xAdmittanceMatrix(CgmesModel cgmes, PropertyBag transformer, CgmesEquipmentModelMapping config) {
        super();
        this.config = config;
        utils = new TxUtilities(cgmes);

        r1 = transformer.asDouble("r1", 0.0);
        x1 = transformer.asDouble("x1", 0.0);
        b1 = transformer.asDouble("b1", 0.0);
        g1 = transformer.asDouble("g1", 0.0);
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
        xStepMin1 = transformer.asDouble("xStepMin1", 0.0);
        xStepMax1 = transformer.asDouble("xStepMax1", 0.0);
        r2 = transformer.asDouble("r2", 0.0);
        x2 = transformer.asDouble("x2", 0.0);
        b2 = transformer.asDouble("b2", 0.0);
        g2 = transformer.asDouble("g2", 0.0);
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
        xStepMin2 = transformer.asDouble("xStepMin2", 0.0);
        xStepMax2 = transformer.asDouble("xStepMax2", 0.0);
        pwca1 = transformer.asDouble("pwca1", 0.0);
        pwca2 = transformer.asDouble("pwca2", 0.0);
        stepPhaseShiftIncrement1 = transformer.asDouble("pspsi1", 0.0);
        stepPhaseShiftIncrement2 = transformer.asDouble("pspsi2", 0.0);
        ptype1 = transformer.get("ptype1");
        ptype2 = transformer.get("ptype2");
        phaseTapChangerTable1 = transformer.get("PhaseTapChangerTable1");
        phaseTapChangerTable2 = transformer.get("PhaseTapChangerTable2");
        ratioTapChangerTable1 = transformer.get("RatioTapChangerTable1");
        ratioTapChangerTable2 = transformer.get("RatioTapChangerTable2");
    }

    public void calculate() {

        // ratio end1
        boolean rtc1TabularDifferentRatios = false;
        double rtc1a = 1.0;
        double rtc1A = 0.0;
        if (utils.ratioTapChangerIsTabular(ratioTapChangerTable1)) {
            TapChangerData tapChangerData = utils.getTabularRatioTapChangerData(rstep1,
                    ratioTapChangerTable1);

            rtc1a = tapChangerData.rptca;
            rtc1A = tapChangerData.rptcA;
            double xc = tapChangerData.xc;
            double rc = tapChangerData.rc;
            double bc = tapChangerData.bc;
            double gc = tapChangerData.gc;

            t2xParametersCorrectionEnd1(xc, rc, bc, gc);

            rtc1TabularDifferentRatios = utils.getTabularRatioTapChangerDifferentRatios(
                    ratioTapChangerTable1);
        } else {
            TapChangerData tapChangerData = utils.getRatioTapChangerData(rstep1, rns1, rsvi1);
            rtc1a = tapChangerData.rptca;
            rtc1A = tapChangerData.rptcA;
        }

        // phase end1
        boolean ptc1TabularDifferentRatios = false;
        boolean ptc1TabularDifferentAngles = false;
        boolean ptc1AsymmetricalDifferentRatios = false;
        double ptc1a = 1.0;
        double ptc1A = 0.0;
        if (utils.phaseTapChangerIsTabular(ptype1, phaseTapChangerTable1)) {
            TapChangerData tapChangerData = utils.getTabularPhaseTapChangerData(pstep1,
                    phaseTapChangerTable1);
            ptc1a = tapChangerData.rptca;
            ptc1A = tapChangerData.rptcA;
            double xc = tapChangerData.xc;
            double rc = tapChangerData.rc;
            double bc = tapChangerData.bc;
            double gc = tapChangerData.gc;

            t2xParametersCorrectionEnd1(xc, rc, bc, gc);

            ptc1TabularDifferentRatios = utils.getTabularPhaseTapChangerDifferentRatios(
                    phaseTapChangerTable1);
            ptc1TabularDifferentAngles = utils.getTabularPhaseTapChangerDifferentAngles(
                    phaseTapChangerTable1);

        } else if (utils.phaseTapChangerIsAsymmetrical(ptype1)) {
            TapChangerData tapChangerData = utils.getAsymmetricalPhaseTapChangerData(ptype1, pstep1, pns1,
                    psvi1, pwca1);
            ptc1a = tapChangerData.rptca;
            ptc1A = tapChangerData.rptcA;

            if (xStepMax1 > 0) {
                double alphaMax = utils.getAsymmetricalAlphaMax(ptype1, pls1, phs1, pns1, psvi1, pwca1);
                x1 = utils.getAsymmetricalX(xStepMin1, xStepMax1, ptc1A, alphaMax, pwca1);
            }

            ptc1AsymmetricalDifferentRatios = utils.getAsymmetricalPhaseTapChangerDifferentRatios(psvi1,
                    pls1, phs1);

        } else if (utils.phaseTapChangerIsSymmetrical(ptype1)) {
            TapChangerData tapChangerData = utils.getSymmetricalPhaseTapChangerData(ptype1, pstep1, pns1,
                    psvi1, stepPhaseShiftIncrement1);
            ptc1a = tapChangerData.rptca;
            ptc1A = tapChangerData.rptcA;

            if (xStepMax1 > 0) {
                double alphaMax = utils.getSymmetricalAlphaMax(ptype1, pls1, phs1, pns1, psvi1,
                        stepPhaseShiftIncrement1);
                x1 = utils.getSymmetricalX(xStepMin1, xStepMax1, ptc1A, alphaMax);
            }
        } else {
            TapChangerData tapChangerData = utils.getSymmetricalPhaseTapChangerData(ptype1, pstep1, pns1,
                    psvi1, stepPhaseShiftIncrement1);
            ptc1a = tapChangerData.rptca;
            ptc1A = tapChangerData.rptcA;

            if (xStepMax1 > 0) {
                double alphaMax = utils.getSymmetricalAlphaMax(ptype1, pls1, phs1, pns1, psvi1,
                        stepPhaseShiftIncrement1);
                x1 = utils.getSymmetricalX(xStepMin1, xStepMax1, ptc1A, alphaMax);
            }
        }

        // ratio end2
        boolean rtc2TabularDifferentRatios = false;
        double rtc2a = 1.0;
        double rtc2A = 0.0;
        if (utils.ratioTapChangerIsTabular(ratioTapChangerTable2)) {
            TapChangerData tapChangerData = utils.getTabularRatioTapChangerData(rstep2,
                    ratioTapChangerTable2);

            rtc2a = tapChangerData.rptca;
            rtc2A = tapChangerData.rptcA;
            double xc = tapChangerData.xc;
            double rc = tapChangerData.rc;
            double bc = tapChangerData.bc;
            double gc = tapChangerData.gc;

            t2xParametersCorrectionEnd2(xc, rc, bc, gc);

            rtc2TabularDifferentRatios = utils.getTabularRatioTapChangerDifferentRatios(
                    ratioTapChangerTable2);
        } else {
            TapChangerData tapChangerData = utils.getRatioTapChangerData(rstep2, rns2, rsvi2);
            rtc2a = tapChangerData.rptca;
            rtc2A = tapChangerData.rptcA;
        }

        // phase end2
        boolean ptc2TabularDifferentRatios = false;
        boolean ptc2TabularDifferentAngles = false;
        boolean ptc2AsymmetricalDifferentRatios = false;
        double ptc2a = 1.0;
        double ptc2A = 0.0;
        if (utils.phaseTapChangerIsTabular(ptype2, phaseTapChangerTable2)) {
            TapChangerData tapChangerData = utils.getTabularPhaseTapChangerData(pstep2,
                    phaseTapChangerTable2);
            ptc2a = tapChangerData.rptca;
            ptc2A = tapChangerData.rptcA;
            ptc2A = getT2xPtc2Negate(config, ptc2A);

            double xc = tapChangerData.xc;
            double rc = tapChangerData.rc;
            double bc = tapChangerData.bc;
            double gc = tapChangerData.gc;

            t2xParametersCorrectionEnd2(xc, rc, bc, gc);

            ptc2TabularDifferentRatios = utils.getTabularPhaseTapChangerDifferentRatios(
                    phaseTapChangerTable2);
            ptc2TabularDifferentAngles = utils.getTabularPhaseTapChangerDifferentAngles(
                    phaseTapChangerTable2);

        } else if (utils.phaseTapChangerIsAsymmetrical(ptype2)) {
            TapChangerData tapChangerData = utils.getAsymmetricalPhaseTapChangerData(ptype2, pstep2, pns2,
                    psvi2, pwca2);
            ptc2a = tapChangerData.rptca;
            ptc2A = tapChangerData.rptcA;

            if (xStepMax2 > 0) {
                double alphaMax = utils.getAsymmetricalAlphaMax(ptype2, pls2, phs2, pns2, psvi2, pwca2);
                x2 = utils.getAsymmetricalX(xStepMin2, xStepMax2, ptc2A, alphaMax, pwca2);
            }

            ptc2AsymmetricalDifferentRatios = utils.getAsymmetricalPhaseTapChangerDifferentRatios(psvi2,
                    pls2, phs2);

        } else if (utils.phaseTapChangerIsSymmetrical(ptype2)) {
            TapChangerData tapChangerData = utils.getSymmetricalPhaseTapChangerData(ptype2, pstep2, pns2,
                    psvi2, stepPhaseShiftIncrement2);
            ptc2a = tapChangerData.rptca;
            ptc2A = tapChangerData.rptcA;

            if (xStepMax2 > 0) {
                double alphaMax = utils.getSymmetricalAlphaMax(ptype2, pls2, phs2, pns2, psvi2,
                        stepPhaseShiftIncrement2);
                x2 = utils.getSymmetricalX(xStepMin2, xStepMax2, ptc2A, alphaMax);
            }
        } else {
            TapChangerData tapChangerData = utils.getSymmetricalPhaseTapChangerData(ptype2, pstep2, pns2,
                    psvi2, stepPhaseShiftIncrement2);
            ptc2a = tapChangerData.rptca;
            ptc2A = tapChangerData.rptcA;

            if (xStepMax2 > 0) {
                double alphaMax = utils.getSymmetricalAlphaMax(ptype2, pls2, phs2, pns2, psvi2,
                        stepPhaseShiftIncrement2);
                x2 = utils.getSymmetricalX(xStepMin2, xStepMax2, ptc2A, alphaMax);
            }
        }

        boolean tc1DifferentRatios = utils.getTxDifferentRatios(rsvi1, rls1, rhs1, rtc1TabularDifferentRatios,
                ptc1TabularDifferentRatios, ptc1AsymmetricalDifferentRatios);
        boolean tc2DifferentRatios = utils.getTxDifferentRatios(rsvi2, rls2, rhs2, rtc2TabularDifferentRatios,
                ptc2TabularDifferentRatios, ptc2AsymmetricalDifferentRatios);
        boolean ptc1DifferentAngles = utils.getTxDifferentAngles(psvi1, stepPhaseShiftIncrement1, pls1, phs1,
                ptc1TabularDifferentAngles);
        boolean ptc2DifferentAngles = utils.getTxDifferentAngles(psvi2, stepPhaseShiftIncrement2, pls2, phs2,
                ptc2TabularDifferentAngles);

        RatioPhaseData ratioPhaseData = getT2xRatioPhase(config, rtc1a, ptc1a, rtc2a, ptc2a, rtc1A,
                ptc1A, rtc2A, ptc2A, tc1DifferentRatios, ptc1DifferentAngles, tc2DifferentRatios,
                ptc2DifferentAngles);
        double a1 = ratioPhaseData.a1;
        double angle1 = ratioPhaseData.angle1;
        double a2 = ratioPhaseData.a2;
        double angle2 = ratioPhaseData.angle2;
        tc1DifferentRatios = ratioPhaseData.tc1DifferentRatios;
        ptc1DifferentAngles = ratioPhaseData.ptc1DifferentAngles;
        tc2DifferentRatios = ratioPhaseData.tc2DifferentRatios;
        ptc2DifferentAngles = ratioPhaseData.ptc2DifferentAngles;

        // yshunt
        YShuntData yShuntData = getT2xYShunt(config);
        Complex ysh1 = yShuntData.ysh1;
        Complex ysh2 = yShuntData.ysh2;

        // phaseAngleClock
        PhaseAngleClockData phaseAngleClockData = getT2xPhaseAngleClock(config);
        angle1 += phaseAngleClockData.angle1;
        angle2 += phaseAngleClockData.angle2;
        branchModel = new DetectedBranchModel(ysh1, ysh2, a1, angle1, a2, angle2, tc1DifferentRatios,
                ptc1DifferentAngles, tc2DifferentRatios, ptc2DifferentAngles);

        // add structural ratio after detected branch model
        Ratio0Data ratio0Data = getT2xRatio0(config, ratedU1, ratedU2);
        double a01 = ratio0Data.a01;
        double a02 = ratio0Data.a02;
        a1 *= a01;
        a2 *= a02;

        // admittance
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

    private Ratio0Data getT2xRatio0(CgmesEquipmentModelMapping config, double ratedU12, double ratedU22) {
        EndDistribution t2xRatio0 = config.getT2xRatio0();
        Ratio0Data ratio0Data = new Ratio0Data();
        switch (t2xRatio0) {
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

    private RatioPhaseData getT2xRatioPhase(CgmesEquipmentModelMapping config, double rtc1a, double ptc1a, double rtc2a,
            double ptc2a, double rtc1A, double ptc1A, double rtc2A, double ptc2A,
            boolean tc1DifferentRatios, boolean ptc1DifferentAngles, boolean tc2DifferentRatios,
            boolean ptc2DifferentAngles) {
        EndDistribution t2xRatioPhase = config.getT2xRatioPhase();
        RatioPhaseData ratioPhaseData = new RatioPhaseData();
        switch (t2xRatioPhase) {
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

    private double getT2xPtc2Negate(CgmesEquipmentModelMapping config, double angle) {

        double outAngle = angle;
        if (config.isT2xPtc2Negate()) {
            outAngle = -angle;
        }
        return outAngle;
    }

    private YShuntData getT2xYShunt(CgmesEquipmentModelMapping config) {
        EndDistribution t2xYShunt = config.getT2xYShunt();
        YShuntData yShuntData = new YShuntData();
        switch (t2xYShunt) {
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

    private PhaseAngleClockData getT2xPhaseAngleClock(CgmesEquipmentModelMapping config) {
        PhaseAngleClockData phaseAngleClockData = new PhaseAngleClockData();
        if (config.isT2xPhaseAngleClock()) {
            if (pac1 != 0) {
                phaseAngleClockData.angle1 = utils.getPhaseAngleClock(pac1);
            }
            if (pac2 != 0) {
                phaseAngleClockData.angle2 = utils.getPhaseAngleClock(pac2);
            }

            if (config.isT2xPac2Negate()) {
                phaseAngleClockData.angle2 = -phaseAngleClockData.angle2;
            }
        }
        return phaseAngleClockData;
    }

    private void t2xParametersCorrectionEnd1(double xc, double rc, double bc, double gc) {
        if (x1 != 0.0) {
            x1 = utils.applyCorrection(x1, xc);
        } else {
            x2 = utils.applyCorrection(x2, xc);
        }
        if (r1 != 0.0) {
            r1 = utils.applyCorrection(r1, rc);
        } else {
            r2 = utils.applyCorrection(r2, rc);
        }
        if (b1 != 0.0) {
            b1 = utils.applyCorrection(b1, bc);
        } else {
            b2 = utils.applyCorrection(b2, bc);
        }
        if (g1 != 0.0) {
            g1 = utils.applyCorrection(g1, gc);
        } else {
            g2 = utils.applyCorrection(g2, gc);
        }
    }

    private void t2xParametersCorrectionEnd2(double xc, double rc, double bc, double gc) {
        if (x2 != 0.0) {
            x2 = utils.applyCorrection(x2, xc);
        } else {
            x1 = utils.applyCorrection(x1, xc);
        }
        if (r2 != 0.0) {
            r2 = utils.applyCorrection(r2, rc);
        } else {
            r1 = utils.applyCorrection(r1, rc);
        }
        if (b2 != 0.0) {
            b2 = utils.applyCorrection(b2, bc);
        } else {
            b1 = utils.applyCorrection(b1, bc);
        }
        if (g2 != 0.0) {
            g2 = utils.applyCorrection(g2, gc);
        } else {
            g1 = utils.applyCorrection(g1, gc);
        }
    }

    private final CgmesEquipmentModelMapping config;
    private final TxUtilities                utils;

    private double                           r1;
    private double                           x1;
    private double                           b1;
    private double                           g1;
    private final int                        pac1;
    private final double                     ratedU1;
    private final double                     rns1;
    private final double                     rsvi1;
    private final double                     rstep1;
    private final double                     rls1;
    private final double                     rhs1;
    private final double                     pns1;
    private final double                     psvi1;
    private final double                     pstep1;
    private final double                     pls1;
    private final double                     phs1;
    private final double                     xStepMin1;
    private final double                     xStepMax1;
    private double                           r2;
    private double                           x2;
    private double                           b2;
    private double                           g2;
    private final int                        pac2;
    private final double                     ratedU2;
    private final double                     rns2;
    private final double                     rsvi2;
    private final double                     rstep2;
    private final double                     rls2;
    private final double                     rhs2;
    private final double                     pns2;
    private final double                     psvi2;
    private final double                     pstep2;
    private final double                     pls2;
    private final double                     phs2;
    private final double                     xStepMin2;
    private final double                     xStepMax2;
    private final double                     pwca1;
    private final double                     pwca2;
    private final double                     stepPhaseShiftIncrement1;
    private final double                     stepPhaseShiftIncrement2;
    private final String                     ptype1;
    private final String                     ptype2;
    private final String                     phaseTapChangerTable1;
    private final String                     phaseTapChangerTable2;
    private final String                     ratioTapChangerTable1;
    private final String                     ratioTapChangerTable2;

}
