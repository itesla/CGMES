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

    // T2x
    public DetectedBranchModel(Complex ysh1, Complex ysh2, double a1, double angle1, double a2, double angle2,
            double rsvi1, double rls1, double rhs1, boolean rct1TabularDifferentRatios,
            boolean pct1TabularDifferentRatios,
            boolean pct1AsymmetricalDifferentRatios, double psvi1, double stepPhaseShiftIncrement1, double pls1,
            double phs1, boolean pct1TabularDifferentAngles,
            double rsvi2, double rls2, double rhs2, boolean rct2TabularDifferentRatios,
            boolean pct2TabularDifferentRatios,
            boolean pct2AsymmetricalDifferentRatios, double psvi2, double stepPhaseShiftIncrement2, double pls2,
            double phs2, boolean pct2TabularDifferentAngles) {
        this.ratio1 = t2xRatioModel(a1, rsvi1, rls1, rhs1, rct1TabularDifferentRatios,
                pct1TabularDifferentRatios, pct1AsymmetricalDifferentRatios);
        this.phase1 = t2xPhaseModel(angle1, psvi1, stepPhaseShiftIncrement1, pls1, phs1,
                pct1TabularDifferentAngles);
        this.shunt1 = shuntModel(ysh1);
        this.shunt2 = shuntModel(ysh2);
        this.ratio2 = t2xRatioModel(a2, rsvi2, rls2, rhs2, rct2TabularDifferentRatios,
                pct2TabularDifferentRatios, pct2AsymmetricalDifferentRatios);
        this.phase2 = t2xPhaseModel(angle2, psvi2, stepPhaseShiftIncrement1, pls2, phs2,
                pct2TabularDifferentAngles);
    }

    // T3x End
    public DetectedBranchModel(Complex ysh1, Complex ysh2, double a1, double angle1, double a2, double angle2,
            double rsvi, double rls, double rhs, boolean rctTabularDifferentRatios,
            boolean pctTabularDifferentRatios, boolean pctAsymmetricalDifferentRatios, double psvi,
            double stepPhaseShiftIncrement, double pls, double phs, boolean pctTabularDifferentAngles) {
        if (a1 != 1.0) {
            this.ratio1 = t2xRatioModel(a1, rsvi, rls, rhs, rctTabularDifferentRatios,
                    pctTabularDifferentRatios, pctAsymmetricalDifferentRatios);
        } else {
            this.ratio1 = t2xRatioModelNoRatio();
        }

        if (angle1 != 0.0) {
            this.phase1 = t2xPhaseModel(angle1, psvi, stepPhaseShiftIncrement, pls, phs,
                    pctTabularDifferentAngles);
        } else {
            this.phase1 = t2xPhaseModelNoPhase();
        }

        this.shunt1 = shuntModel(ysh1);
        this.shunt2 = shuntModel(ysh2);

        if (a2 != 1.0) {
            this.ratio2 = t2xRatioModel(a2, rsvi, rls, rhs, rctTabularDifferentRatios,
                    pctTabularDifferentRatios, pctAsymmetricalDifferentRatios);
        } else {
            this.ratio2 = t2xRatioModelNoRatio();
        }

        if (angle2 != 0.0) {
            this.phase2 = t2xPhaseModel(angle2, psvi, stepPhaseShiftIncrement, pls, phs,
                    pctTabularDifferentAngles);
        } else {
            this.phase2 = t2xPhaseModelNoPhase();
        }
    }

    private ChangerType t2xRatioModel(double a, double rsvi, double rls, double rhs,
            boolean rctTabularDifferentRatios,
            boolean pctTabularDifferentRatios, boolean pctAsymmetricalDifferentRatios) {
        if (a == 1.0) {
            if (rsvi != 0.0 && rls != rhs) {
                return ChangerType.CHANGEABLE_AT_NEUTRAL;
            } else if (rctTabularDifferentRatios) {
                return ChangerType.CHANGEABLE_AT_NEUTRAL;
            } else if (pctTabularDifferentRatios) {
                return ChangerType.CHANGEABLE_AT_NEUTRAL;
            } else if (pctAsymmetricalDifferentRatios) {
                return ChangerType.CHANGEABLE_AT_NEUTRAL;
            } else {
                return ChangerType.ABSENT;
            }
        } else {
            if (rsvi != 0.0 && rls != rhs) {
                return ChangerType.CHANGEABLE_AT_NON_NEUTRAL;
            } else if (rctTabularDifferentRatios) {
                return ChangerType.CHANGEABLE_AT_NON_NEUTRAL;
            } else if (pctTabularDifferentRatios) {
                return ChangerType.CHANGEABLE_AT_NON_NEUTRAL;
            } else if (pctAsymmetricalDifferentRatios) {
                return ChangerType.CHANGEABLE_AT_NON_NEUTRAL;
            } else {
                return ChangerType.FIXED;
            }
        }
    }

    private ChangerType t2xRatioModelNoRatio() {
        return ChangerType.ABSENT;
    }

    private ChangerType t2xPhaseModel(double angle, double psvi, double stepPhaseShiftIncrement,
            double pls, double phs, boolean pct1TabularDifferentAngles) {
        StringBuilder model = new StringBuilder();
        if (angle == 0.0) {
            if (psvi != 0.0 && pls != phs) {
                return ChangerType.CHANGEABLE_AT_NEUTRAL;
            } else if (stepPhaseShiftIncrement != 0 && pls != phs) {
                return ChangerType.CHANGEABLE_AT_NEUTRAL;
            } else if (pct1TabularDifferentAngles) {
                return ChangerType.CHANGEABLE_AT_NEUTRAL;
            } else {
                return ChangerType.ABSENT;
            }
        } else {
            if (psvi != 0.0 && pls != phs) {
                return ChangerType.CHANGEABLE_AT_NON_NEUTRAL;
            } else if (stepPhaseShiftIncrement != 0 && pls != phs) {
                return ChangerType.CHANGEABLE_AT_NON_NEUTRAL;
            } else if (pct1TabularDifferentAngles) {
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
