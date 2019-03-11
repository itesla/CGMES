package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.T3xDistribution;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.T3xPhaseAngleClock;
import com.powsybl.cgmes.model.interpretation.TxUtilities.PhaseAngleClockData;
import com.powsybl.cgmes.model.interpretation.TxUtilities.Ratio0Data;
import com.powsybl.cgmes.model.interpretation.TxUtilities.RatioPhaseData;
import com.powsybl.cgmes.model.interpretation.TxUtilities.TapChangerData;
import com.powsybl.cgmes.model.interpretation.TxUtilities.YShuntData;
import com.powsybl.triplestore.api.PropertyBag;

public class T3xAdmittanceMatrix extends AbstractAdmittanceMatrix3 {

    public T3xAdmittanceMatrix(CgmesModel cgmes, PropertyBag transformer, CgmesEquipmentModelMapping config) {
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
        r3 = transformer.asDouble("r3", 0.0);
        x3 = transformer.asDouble("x3", 0.0);
        b3 = transformer.asDouble("b3", 0.0);
        g3 = transformer.asDouble("g3", 0.0);
        pac3 = transformer.asInt("pac3", 0);
        ratedU3 = transformer.asDouble("ratedU3");
        rns3 = transformer.asDouble("rns3", 0.0);
        rsvi3 = transformer.asDouble("rsvi3", 0.0);
        rstep3 = transformer.asDouble("rstep3", 0.0);
        rls3 = transformer.asDouble("rls3", 0.0);
        rhs3 = transformer.asDouble("rhs3", 0.0);
        pns3 = transformer.asDouble("pns3", 0.0);
        psvi3 = transformer.asDouble("psvi3", 0.0);
        pstep3 = transformer.asDouble("pstep3", 0.0);
        pls3 = transformer.asDouble("pls3", 0.0);
        phs3 = transformer.asDouble("phs3", 0.0);
        pwca1 = transformer.asDouble("pwca1", 0.0);
        pwca2 = transformer.asDouble("pwca2", 0.0);
        pwca3 = transformer.asDouble("pwca3", 0.0);
        stepPhaseShiftIncrement1 = transformer.asDouble("stepPhaseShiftIncrement1", 0.0);
        stepPhaseShiftIncrement2 = transformer.asDouble("stepPhaseShiftIncrement2", 0.0);
        stepPhaseShiftIncrement3 = transformer.asDouble("stepPhaseShiftIncrement3", 0.0);
        ptype1 = transformer.get("ptype1");
        ptype2 = transformer.get("ptype2");
        ptype3 = transformer.get("ptype3");
        phaseTapChangerTable1 = transformer.get("PhaseTapChangerTable1");
        phaseTapChangerTable2 = transformer.get("PhaseTapChangerTable2");
        phaseTapChangerTable3 = transformer.get("PhaseTapChangerTable3");
        ratioTapChangerTable1 = transformer.get("RatioTapChangerTable1");
        ratioTapChangerTable2 = transformer.get("RatioTapChangerTable2");
        ratioTapChangerTable3 = transformer.get("RatioTapChangerTable3");
    }

    public void calculate() {

        // ratio end1
        boolean rct1TabularDifferentRatios = false;
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

            t3xParametersCorrectionEnd1(xc, rc, bc, gc);

            rct1TabularDifferentRatios = utils.getTabularRatioTapChangerDifferentRatios(
                    ratioTapChangerTable1);
        } else {
            TapChangerData tapChangerData = utils.getRatioTapChangerData(rstep1, rns1, rsvi1);
            rtc1a = tapChangerData.rptca;
            rtc1A = tapChangerData.rptcA;
        }

        // phase end1
        boolean pct1TabularDifferentRatios = false;
        boolean pct1TabularDifferentAngles = false;
        boolean pct1AsymmetricalDifferentRatios = false;
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

            t3xParametersCorrectionEnd1(xc, rc, bc, gc);

            pct1TabularDifferentRatios = utils.getTabularPhaseTapChangerDifferentRatios(
                    phaseTapChangerTable1);
            pct1TabularDifferentAngles = utils.getTabularPhaseTapChangerDifferentAngles(
                    phaseTapChangerTable1);

        } else if (utils.phaseTapChangerIsAsymmetrical(ptype1)) {
            TapChangerData tapChangerData = utils.getAsymmetricalPhaseTapChangerData(ptype1, pstep1, pns1,
                    psvi1, pwca1);
            ptc1a = tapChangerData.rptca;
            ptc1A = tapChangerData.rptcA;

            pct1AsymmetricalDifferentRatios = utils.getAsymmetricalPhaseTapChangerDifferentRatios(psvi1,
                    pls1, phs1);

        } else if (utils.phaseTapChangerIsSymmetrical(ptype1)) {
            TapChangerData tapChangerData = utils.getSymmetricalPhaseTapChangerData(ptype1, pstep1, pns1,
                    psvi1,
                    stepPhaseShiftIncrement1);
            ptc1a = tapChangerData.rptca;
            ptc1A = tapChangerData.rptcA;
        } else {
            TapChangerData tapChangerData = utils.getSymmetricalPhaseTapChangerData(ptype1, pstep1, pns1,
                    psvi1,
                    stepPhaseShiftIncrement1);
            ptc1a = tapChangerData.rptca;
            ptc1A = tapChangerData.rptcA;
        }

        // ratio end2
        boolean rct2TabularDifferentRatios = false;
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

            t3xParametersCorrectionEnd2(xc, rc, bc, gc);

            rct2TabularDifferentRatios = utils.getTabularRatioTapChangerDifferentRatios(
                    ratioTapChangerTable2);
        } else {
            TapChangerData tapChangerData = utils.getRatioTapChangerData(rstep2, rns2, rsvi2);
            rtc2a = tapChangerData.rptca;
            rtc2A = tapChangerData.rptcA;
        }

        // phase end2
        boolean pct2TabularDifferentRatios = false;
        boolean pct2TabularDifferentAngles = false;
        boolean pct2AsymmetricalDifferentRatios = false;
        double ptc2a = 1.0;
        double ptc2A = 0.0;
        if (utils.phaseTapChangerIsTabular(ptype2, phaseTapChangerTable2)) {
            TapChangerData tapChangerData = utils.getTabularPhaseTapChangerData(pstep2,
                    phaseTapChangerTable2);
            ptc2a = tapChangerData.rptca;
            ptc2A = tapChangerData.rptcA;
            double xc = tapChangerData.xc;
            double rc = tapChangerData.rc;
            double bc = tapChangerData.bc;
            double gc = tapChangerData.gc;

            t3xParametersCorrectionEnd2(xc, rc, bc, gc);

            pct2TabularDifferentRatios = utils.getTabularPhaseTapChangerDifferentRatios(
                    phaseTapChangerTable2);
            pct2TabularDifferentAngles = utils.getTabularPhaseTapChangerDifferentAngles(
                    phaseTapChangerTable2);

        } else if (utils.phaseTapChangerIsAsymmetrical(ptype2)) {
            TapChangerData tapChangerData = utils.getAsymmetricalPhaseTapChangerData(ptype2, pstep2, pns2,
                    psvi2, pwca2);
            ptc2a = tapChangerData.rptca;
            ptc2A = tapChangerData.rptcA;

            pct2AsymmetricalDifferentRatios = utils.getAsymmetricalPhaseTapChangerDifferentRatios(psvi2,
                    pls2, phs2);

        } else if (utils.phaseTapChangerIsSymmetrical(ptype2)) {
            TapChangerData tapChangerData = utils.getSymmetricalPhaseTapChangerData(ptype2, pstep2, pns2,
                    psvi2,
                    stepPhaseShiftIncrement2);
            ptc2a = tapChangerData.rptca;
            ptc2A = tapChangerData.rptcA;
        } else {
            TapChangerData tapChangerData = utils.getSymmetricalPhaseTapChangerData(ptype2, pstep2, pns2,
                    psvi2,
                    stepPhaseShiftIncrement2);
            ptc2a = tapChangerData.rptca;
            ptc2A = tapChangerData.rptcA;
        }

        // ratio end3
        boolean rct3TabularDifferentRatios = false;
        double rtc3a = 1.0;
        double rtc3A = 0.0;
        if (utils.ratioTapChangerIsTabular(ratioTapChangerTable3)) {
            TapChangerData tapChangerData = utils.getTabularRatioTapChangerData(rstep3,
                    ratioTapChangerTable3);

            rtc3a = tapChangerData.rptca;
            rtc3A = tapChangerData.rptcA;
            double xc = tapChangerData.xc;
            double rc = tapChangerData.rc;
            double bc = tapChangerData.bc;
            double gc = tapChangerData.gc;

            t3xParametersCorrectionEnd3(xc, rc, bc, gc);

            rct3TabularDifferentRatios = utils.getTabularRatioTapChangerDifferentRatios(
                    ratioTapChangerTable3);
        } else {
            TapChangerData tapChangerData = utils.getRatioTapChangerData(rstep3, rns3, rsvi3);
            rtc3a = tapChangerData.rptca;
            rtc3A = tapChangerData.rptcA;
        }

        // phase end3
        boolean pct3TabularDifferentRatios = false;
        boolean pct3TabularDifferentAngles = false;
        boolean pct3AsymmetricalDifferentRatios = false;
        double ptc3a = 1.0;
        double ptc3A = 0.0;
        if (utils.phaseTapChangerIsTabular(ptype3, phaseTapChangerTable3)) {
            TapChangerData tapChangerData = utils.getTabularPhaseTapChangerData(pstep3,
                    phaseTapChangerTable3);
            ptc3a = tapChangerData.rptca;
            ptc3A = tapChangerData.rptcA;
            double xc = tapChangerData.xc;
            double rc = tapChangerData.rc;
            double bc = tapChangerData.bc;
            double gc = tapChangerData.gc;

            t3xParametersCorrectionEnd3(xc, rc, bc, gc);

            pct3TabularDifferentRatios = utils.getTabularPhaseTapChangerDifferentRatios(
                    phaseTapChangerTable3);
            pct3TabularDifferentAngles = utils.getTabularPhaseTapChangerDifferentAngles(
                    phaseTapChangerTable3);

        } else if (utils.phaseTapChangerIsAsymmetrical(ptype3)) {
            TapChangerData tapChangerData = utils.getAsymmetricalPhaseTapChangerData(ptype3, pstep3, pns3,
                    psvi3, pwca3);
            ptc3a = tapChangerData.rptca;
            ptc3A = tapChangerData.rptcA;

            pct3AsymmetricalDifferentRatios = utils.getAsymmetricalPhaseTapChangerDifferentRatios(psvi3,
                    pls3, phs3);

        } else if (utils.phaseTapChangerIsSymmetrical(ptype3)) {
            TapChangerData tapChangerData = utils.getSymmetricalPhaseTapChangerData(ptype3, pstep3, pns3,
                    psvi3,
                    stepPhaseShiftIncrement3);
            ptc3a = tapChangerData.rptca;
            ptc3A = tapChangerData.rptcA;
        } else {
            TapChangerData tapChangerData = utils.getSymmetricalPhaseTapChangerData(ptype3, pstep3, pns3,
                    psvi3,
                    stepPhaseShiftIncrement3);
            ptc3a = tapChangerData.rptca;
            ptc3A = tapChangerData.rptcA;
        }

        T3xRatioPhaseData ratioPhaseData = getT3xRatioPhase(config, rtc1a, ptc1a, rtc2a, ptc2a,
                rtc3a, ptc3a,
                rtc1A, ptc1A, rtc2A, ptc2A, rtc3A, ptc3A);
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

        // yshunt
        T3xYShuntData yShuntData = getT3xYShunt(config);
        Complex ysh11 = yShuntData.end1.ysh1;
        Complex ysh12 = yShuntData.end1.ysh2;
        Complex ysh21 = yShuntData.end2.ysh1;
        Complex ysh22 = yShuntData.end2.ysh2;
        Complex ysh31 = yShuntData.end3.ysh1;
        Complex ysh32 = yShuntData.end3.ysh2;

        // phaseAngleClock
        T3xPhaseAngleClockData phaseAngleClockData = getT3xPhaseAngleClock(config);
        angle11 += phaseAngleClockData.end1.angle1;
        angle12 += phaseAngleClockData.end1.angle2;
        angle21 += phaseAngleClockData.end2.angle1;
        angle22 += phaseAngleClockData.end2.angle2;
        angle31 += phaseAngleClockData.end3.angle1;
        angle32 += phaseAngleClockData.end3.angle2;

        end1.branchModel = new DetectedBranchModel(ysh11, ysh12, a11, angle11, a12, angle12, rsvi1, rls1,
                rhs1, rct1TabularDifferentRatios, pct1TabularDifferentRatios,
                pct1AsymmetricalDifferentRatios, psvi1, stepPhaseShiftIncrement1, pls1, phs1,
                pct1TabularDifferentAngles);

        end2.branchModel = new DetectedBranchModel(ysh21, ysh22, a21, angle21, a22, angle22, rsvi2, rls2,
                rhs2, rct2TabularDifferentRatios, pct2TabularDifferentRatios,
                pct2AsymmetricalDifferentRatios, psvi2, stepPhaseShiftIncrement2, pls2, phs2,
                pct2TabularDifferentAngles);

        end3.branchModel = new DetectedBranchModel(ysh31, ysh32, a31, angle31, a32, angle32, rsvi3, rls3,
                rhs3, rct3TabularDifferentRatios, pct3TabularDifferentRatios,
                pct3AsymmetricalDifferentRatios, psvi3, stepPhaseShiftIncrement3, pls3, phs3,
                pct3TabularDifferentAngles);

        // add structural ratio after detected branch model
        double ratedU0 = 1.0;
        T3xRatio0Data ratio0Data = getT3xRatio0(config, ratedU0, ratedU1, ratedU2, ratedU3);
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
        angle11 = Math.toRadians(angle11);
        angle12 = Math.toRadians(angle12);
        angle21 = Math.toRadians(angle21);
        angle22 = Math.toRadians(angle22);
        angle31 = Math.toRadians(angle31);
        angle32 = Math.toRadians(angle32);
        Complex aA11 = new Complex(a11 * Math.cos(angle11), a11 * Math.sin(angle11));
        Complex aA12 = new Complex(a12 * Math.cos(angle12), a12 * Math.sin(angle12));
        Complex aA21 = new Complex(a21 * Math.cos(angle21), a21 * Math.sin(angle21));
        Complex aA22 = new Complex(a22 * Math.cos(angle22), a22 * Math.sin(angle22));
        Complex aA31 = new Complex(a31 * Math.cos(angle31), a31 * Math.sin(angle31));
        Complex aA32 = new Complex(a32 * Math.cos(angle32), a32 * Math.sin(angle32));

        Complex z1 = new Complex(r1, x1);
        end1.yff = z1.reciprocal().add(ysh11).divide(aA11.conjugate().multiply(aA11));
        end1.yft = z1.reciprocal().negate().divide(aA11.conjugate().multiply(aA12));
        end1.ytf = z1.reciprocal().negate().divide(aA12.conjugate().multiply(aA11));
        end1.ytt = z1.reciprocal().add(ysh12).divide(aA12.conjugate().multiply(aA12));

        Complex z2 = new Complex(r2, x2);
        end2.yff = z2.reciprocal().add(ysh21).divide(aA21.conjugate().multiply(aA21));
        end2.yft = z2.reciprocal().negate().divide(aA21.conjugate().multiply(aA22));
        end2.ytf = z2.reciprocal().negate().divide(aA22.conjugate().multiply(aA21));
        end2.ytt = z2.reciprocal().add(ysh22).divide(aA22.conjugate().multiply(aA22));

        Complex z3 = new Complex(r3, x3);
        end3.yff = z3.reciprocal().add(ysh31).divide(aA31.conjugate().multiply(aA31));
        end3.yft = z3.reciprocal().negate().divide(aA31.conjugate().multiply(aA32));
        end3.ytf = z3.reciprocal().negate().divide(aA32.conjugate().multiply(aA31));
        end3.ytt = z3.reciprocal().add(ysh32).divide(aA32.conjugate().multiply(aA32));
    }

    private T3xRatio0Data getT3xRatio0(CgmesEquipmentModelMapping config, double ratedU0, double ratedU1,
            double ratedU2,
            double ratedU3) {
        T3xRatio0Data ratio0Data = new T3xRatio0Data();
        if (config.isT3xRatio0Inside()) {
            ratio0Data.end1.a01 = ratedU1 / ratedU1;
            ratio0Data.end1.a02 = ratedU0 / ratedU1;
            ratio0Data.end2.a01 = ratedU2 / ratedU2;
            ratio0Data.end2.a02 = ratedU0 / ratedU2;
            ratio0Data.end3.a01 = ratedU3 / ratedU3;
            ratio0Data.end3.a02 = ratedU0 / ratedU3;
        } else {
            ratio0Data.end1.a01 = ratedU1 / ratedU0;
            ratio0Data.end1.a02 = ratedU0 / ratedU0;
            ratio0Data.end2.a01 = ratedU2 / ratedU0;
            ratio0Data.end2.a02 = ratedU0 / ratedU0;
            ratio0Data.end3.a01 = ratedU3 / ratedU0;
            ratio0Data.end3.a02 = ratedU0 / ratedU0;
        }
        return ratio0Data;
    }

    private T3xRatioPhaseData getT3xRatioPhase(CgmesEquipmentModelMapping config, double rtc1a, double ptc1a,
            double rtc2a,
            double ptc2a,
            double rtc3a, double ptc3a, double rtc1A, double ptc1A, double rtc2A, double ptc2A,
            double rtc3A, double ptc3A) {
        T3xRatioPhaseData ratioPhaseData = new T3xRatioPhaseData();
        if (config.isT3xRatioPhaseInside()) {
            ratioPhaseData.end1.a2 = rtc1a * ptc1a;
            ratioPhaseData.end1.angle2 = rtc1A + ptc1A;
            ratioPhaseData.end2.a2 = rtc2a * ptc2a;
            ratioPhaseData.end2.angle2 = rtc2A + ptc2A;
            ratioPhaseData.end3.a2 = rtc3a * ptc3a;
            ratioPhaseData.end3.angle2 = rtc3A + ptc3A;
        } else {
            ratioPhaseData.end1.a1 = rtc1a * ptc1a;
            ratioPhaseData.end1.angle1 = rtc1A + ptc1A;
            ratioPhaseData.end2.a1 = rtc2a * ptc2a;
            ratioPhaseData.end2.angle1 = rtc2A + ptc2A;
            ratioPhaseData.end3.a1 = rtc3a * ptc3a;
            ratioPhaseData.end3.angle1 = rtc3A + ptc3A;
        }
        return ratioPhaseData;
    }

    private T3xYShuntData getT3xYShunt(CgmesEquipmentModelMapping config) {
        T3xDistribution t3xYShunt = config.getT3xYShunt();
        T3xYShuntData yShuntData = new T3xYShuntData();
        switch (t3xYShunt) {
            case OUTSIDE:
                yShuntData.end1.ysh1 = yShuntData.end1.ysh1.add(new Complex(g1, b1));
                yShuntData.end2.ysh1 = yShuntData.end2.ysh1.add(new Complex(g2, b2));
                yShuntData.end3.ysh1 = yShuntData.end3.ysh1.add(new Complex(g3, b3));
                break;
            case INSIDE:
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

    private T3xPhaseAngleClockData getT3xPhaseAngleClock(CgmesEquipmentModelMapping config) {
        T3xPhaseAngleClock t3xPhaseAngleClock = config.getT3xPhaseAngleClock();
        T3xPhaseAngleClockData phaseAngleClockData = new T3xPhaseAngleClockData();
        switch (t3xPhaseAngleClock) {
            case INSIDE:
                if (pac1 != 0) {
                    phaseAngleClockData.end1.angle2 = utils.getPhaseAngleClock(pac1);
                }
                if (pac2 != 0) {
                    phaseAngleClockData.end2.angle2 = utils.getPhaseAngleClock(pac2);
                }
                if (pac3 != 0) {
                    phaseAngleClockData.end3.angle2 = utils.getPhaseAngleClock(pac3);
                }
                break;
            case OUTSIDE:
                if (pac1 != 0) {
                    phaseAngleClockData.end1.angle1 = utils.getPhaseAngleClock(pac1);
                }
                if (pac2 != 0) {
                    phaseAngleClockData.end2.angle1 = utils.getPhaseAngleClock(pac2);
                }
                if (pac3 != 0) {
                    phaseAngleClockData.end3.angle1 = utils.getPhaseAngleClock(pac3);
                }
                break;
        }
        return phaseAngleClockData;
    }

    private void t3xParametersCorrectionEnd1(double xc, double rc, double bc, double gc) {
        x1 = utils.applyCorrection(x1, xc);
        r1 = utils.applyCorrection(r1, rc);
        b1 = utils.applyCorrection(b1, bc);
        g1 = utils.applyCorrection(g1, gc);
    }

    private void t3xParametersCorrectionEnd2(double xc, double rc, double bc, double gc) {
        x2 = utils.applyCorrection(x2, xc);
        r2 = utils.applyCorrection(r2, rc);
        b2 = utils.applyCorrection(b2, bc);
        g2 = utils.applyCorrection(g2, gc);
    }

    private void t3xParametersCorrectionEnd3(double xc, double rc, double bc, double gc) {
        x3 = utils.applyCorrection(x3, xc);
        r3 = utils.applyCorrection(r3, rc);
        b3 = utils.applyCorrection(b3, bc);
        g3 = utils.applyCorrection(g3, gc);
    }

    static class T3xRatioPhaseData {
        RatioPhaseData end1 = new RatioPhaseData();
        RatioPhaseData end2 = new RatioPhaseData();
        RatioPhaseData end3 = new RatioPhaseData();
    }

    static class T3xYShuntData {
        YShuntData end1 = new YShuntData();
        YShuntData end2 = new YShuntData();
        YShuntData end3 = new YShuntData();
    }

    static class T3xRatio0Data {
        Ratio0Data end1 = new Ratio0Data();
        Ratio0Data end2 = new Ratio0Data();
        Ratio0Data end3 = new Ratio0Data();
    }

    static class T3xPhaseAngleClockData {
        PhaseAngleClockData end1 = new PhaseAngleClockData();
        PhaseAngleClockData end2 = new PhaseAngleClockData();
        PhaseAngleClockData end3 = new PhaseAngleClockData();
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
    private double                           r3;
    private double                           x3;
    private double                           b3;
    private double                           g3;
    private final int                        pac3;
    private final double                     ratedU3;
    private final double                     rns3;
    private final double                     rsvi3;
    private final double                     rstep3;
    private final double                     rls3;
    private final double                     rhs3;
    private final double                     pns3;
    private final double                     psvi3;
    private final double                     pstep3;
    private final double                     pls3;
    private final double                     phs3;
    private final double                     pwca1;
    private final double                     pwca2;
    private final double                     pwca3;
    private final double                     stepPhaseShiftIncrement1;
    private final double                     stepPhaseShiftIncrement2;
    private final double                     stepPhaseShiftIncrement3;
    private final String                     ptype1;
    private final String                     ptype2;
    private final String                     ptype3;
    private final String                     phaseTapChangerTable1;
    private final String                     phaseTapChangerTable2;
    private final String                     phaseTapChangerTable3;
    private final String                     ratioTapChangerTable1;
    private final String                     ratioTapChangerTable2;
    private final String                     ratioTapChangerTable3;
}
