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

    protected double applyCorrection(double v, double vc) {
        return v * (1.0 + vc / 100.0);
    }

    protected double[] getTabularPhaseConfiguration(double pstep, String phaseTapChangerTableName) {
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
                return new double[] {ptca, ptcA, xc, rc, bc, gc};
            }
        }
        return new double[] {ptca, ptcA, xc, rc, bc, gc};
    }

    protected double[] getRatioConfiguration(double rstep, double rns,
            double rsvi) {
        double rtca = 1.0 + (rstep - rns) * (rsvi / 100.0);
        double rtcA = 0.0;
        return new double[] {rtca, rtcA};
    }

    protected double[] getPhaseConfiguration(String ptype, double pstep, double pns,
            double psvi, double pwca, double stepPhaseShiftIncrement1) {
        double ptca = 1.0;
        double ptcA = 0.0;
        if (ptype != null && ptype.endsWith("asymmetrical")) {
            double dx = 1.0
                    + (pstep - pns) * (psvi / 100.0) * Math.cos(Math.toRadians(pwca));
            double dy = (pstep - pns) * (psvi / 100.0) * Math.sin(Math.toRadians(pwca));
            ptca = Math.hypot(dx, dy);
            ptcA = Math.toDegrees(Math.atan2(dy, dx));
        } else if (ptype != null && ptype.endsWith("symmetrical")) {
            if (stepPhaseShiftIncrement1 != 0.0) {
                ptca = 1.0;
                ptcA = (pstep - pns) * stepPhaseShiftIncrement1;
            } else {
                double dy = (pstep - pns) * (psvi / 100.0);
                ptca = 1.0;
                ptcA = Math.toDegrees(Math.atan2(dy, 1.0));
            }
        }
        return new double[] {ptca, ptcA};
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

    protected String bShuntCode(double bsh1, double bsh2) {
        StringBuilder code = new StringBuilder();
        if (bsh1 == 0.0) {
            code.append("N");
        } else {
            code.append("Y");
        }
        if (bsh2 == 0.0) {
            code.append("N");
        } else {
            code.append("Y");
        }
        return code.toString();
    }

    protected String endCode(PropertyBag transformer, Complex ysh1, Complex ysh2, double a1,
            double angle1, double a2, double angle2, double rstep1, double rls1, double rhs1,
            double pstep1, double pls1, double phs1, double rstep2, double rls2, double rhs2,
            double pstep2, double pls2, double phs2) {
        StringBuilder code = new StringBuilder();
        code.append(ratioCode(a1, rstep1, rls1, rhs1));
        code.append(phaseCode(angle1, pstep1, pls1, phs1));
        code.append(yShuntCode(ysh1, ysh2));
        code.append(ratioCode(a2, rstep2, rls2, rhs2));
        code.append(phaseCode(angle2, pstep2, pls2, phs2));
        return code.toString();
    }

    protected String ratioCode(double a, double rstep, double rls, double rhs) {
        StringBuilder code = new StringBuilder();
        if (a == 1.0) {
            if (!Double.isNaN(rstep)) {
                if (!Double.isNaN(rhs) && rls == rhs) {
                    code.append("*");
                } else {
                    code.append("r");
                }
            } else {
                code.append("_");
            }
        } else {
            if (!Double.isNaN(rhs) && rls == rhs) {
                code.append("*");
            } else {
                code.append("R");
            }
        }
        return code.toString();
    }

    protected String phaseCode(double angle, double pstep, double pls, double phs) {
        StringBuilder code = new StringBuilder();
        if (angle == 0.0) {
            if (!Double.isNaN(pstep)) {
                if (!Double.isNaN(phs) && pls == phs) {
                    code.append("*");
                } else {
                    code.append("p");
                }
            } else {
                code.append("_");
            }
        } else {
            if (!Double.isNaN(phs) && pls == phs) {
                code.append("*");
            } else {
                code.append("P");
            }
        }
        return code.toString();
    }

    protected String yShuntCode(Complex ysh1, Complex ysh2) {
        StringBuilder code = new StringBuilder();
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
        return code.toString();
    }

    protected CgmesModel           cgmes;
    protected Complex              yff;
    protected Complex              yft;
    protected Complex              ytf;
    protected Complex              ytt;
    protected StringBuilder        code;

    protected static final Logger  LOG = LoggerFactory
            .getLogger(AdmittanceMatrix.class);
}
