package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

class DetectedBranchModel {

    enum ChangerType {
        ABSENT, FIXED, CHANGEABLE_AT_NEUTRAL, CHANGEABLE_AT_NON_NEUTRAL
    }

    // Line
    public DetectedBranchModel(double bsh1, double bsh2) {
        this.ratio1 = null;
        this.phase1 = null;
        this.shunt1 = shuntModel(new Complex(0.0, bsh1));
        this.shunt2 = shuntModel(new Complex(0.0, bsh2));
        this.ratio2 = null;
        this.phase2 = null;
    }

    public DetectedBranchModel(Complex ysh1, Complex ysh2, double a1, double angle1, double a2, double angle2,
            boolean tc1DifferentRatios, boolean ptc1DifferentAngles, boolean tc2DifferentRatios, boolean ptc2DifferentAngles) {
        this.ratio1 = t2xRatioModel(a1, tc1DifferentRatios);
        this.phase1 = t2xPhaseModel(angle1, ptc1DifferentAngles);
        this.shunt1 = shuntModel(ysh1);
        this.shunt2 = shuntModel(ysh2);
        this.ratio2 = t2xRatioModel(a2, tc2DifferentRatios);
        this.phase2 = t2xPhaseModel(angle2, ptc2DifferentAngles);
    }

    private ChangerType t2xRatioModel(double a, boolean tcDifferentRatios) {
        if (a == 1.0) {
            if (tcDifferentRatios) {
                return ChangerType.CHANGEABLE_AT_NEUTRAL;
            } else {
                return ChangerType.ABSENT;
            }
        } else {
            if (tcDifferentRatios) {
                return ChangerType.CHANGEABLE_AT_NON_NEUTRAL;
            } else {
                return ChangerType.FIXED;
            }
        }
    }

    private ChangerType t2xRatioModelNoRatio() {
        return ChangerType.ABSENT;
    }

    private ChangerType t2xPhaseModel(double angle, boolean ptcDifferentAngles) {
        StringBuilder model = new StringBuilder();
        if (angle == 0.0) {
            if (ptcDifferentAngles) {
                return ChangerType.CHANGEABLE_AT_NEUTRAL;
            } else {
                return ChangerType.ABSENT;
            }
        } else {
            if (ptcDifferentAngles) {
                return ChangerType.CHANGEABLE_AT_NON_NEUTRAL;
            } else {
                return ChangerType.FIXED;
            }
        }
    }

    private ChangerType t2xPhaseModelNoPhase() {
        return ChangerType.ABSENT;
    }

    private boolean shuntModel(Complex ysh) {
        if (ysh.equals(Complex.ZERO)) {
            return false;
        }
        return true;
    }

    public String code() {
        StringBuilder code = new StringBuilder();
        if (ratio1 != null) {
            switch (ratio1) {
                case ABSENT:
                    code.append("_");
                    break;
                case FIXED:
                    code.append("*");
                    break;
                case CHANGEABLE_AT_NEUTRAL:
                    code.append("r");
                    break;
                case CHANGEABLE_AT_NON_NEUTRAL:
                    code.append("R");
                    break;
            }
        }
        if (phase1 != null) {
            switch (phase1) {
                case ABSENT:
                    code.append("_");
                    break;
                case FIXED:
                    code.append("*");
                    break;
                case CHANGEABLE_AT_NEUTRAL:
                    code.append("p");
                    break;
                case CHANGEABLE_AT_NON_NEUTRAL:
                    code.append("P");
                    break;
            }
        }
        code.append(shunt1 ? "Y" : "N");
        code.append(shunt2 ? "Y" : "N");
        if (ratio2 != null) {
            switch (ratio2) {
                case ABSENT:
                    code.append("_");
                    break;
                case FIXED:
                    code.append("*");
                    break;
                case CHANGEABLE_AT_NEUTRAL:
                    code.append("r");
                    break;
                case CHANGEABLE_AT_NON_NEUTRAL:
                    code.append("R");
                    break;
            }
        }
        if (phase2 != null) {
            switch (phase2) {
                case ABSENT:
                    code.append("_");
                    break;
                case FIXED:
                    code.append("*");
                    break;
                case CHANGEABLE_AT_NEUTRAL:
                    code.append("p");
                    break;
                case CHANGEABLE_AT_NON_NEUTRAL:
                    code.append("P");
                    break;
            }
        }
        return code.toString();
    }

    final ChangerType ratio1;
    final ChangerType phase1;
    final boolean     shunt1;
    final boolean     shunt2;
    final ChangerType ratio2;
    final ChangerType phase2;
}
