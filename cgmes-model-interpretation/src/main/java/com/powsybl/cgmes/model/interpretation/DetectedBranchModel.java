/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
class DetectedBranchModel {

    enum ChangerType {
        ABSENT, FIXED, CHANGEABLE_AT_NEUTRAL, CHANGEABLE_AT_NON_NEUTRAL, REGULATING_CONTROL
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
            boolean rtc1RegulatingControl, boolean tc1DifferentRatios, boolean ptc1RegulatingControl,
            boolean ptc1DifferentAngles, boolean rtc2RegulatingControl, boolean tc2DifferentRatios,
            boolean ptc2RegulatingControl, boolean ptc2DifferentAngles) {
        this.ratio1 = xfmrRatioModel(a1, rtc1RegulatingControl, tc1DifferentRatios);
        this.phase1 = xfmrPhaseModel(angle1, ptc1RegulatingControl, ptc1DifferentAngles);
        this.shunt1 = shuntModel(ysh1);
        this.shunt2 = shuntModel(ysh2);
        this.ratio2 = xfmrRatioModel(a2, rtc2RegulatingControl, tc2DifferentRatios);
        this.phase2 = xfmrPhaseModel(angle2, ptc2RegulatingControl, ptc2DifferentAngles);
    }

    private ChangerType xfmrRatioModel(double a, boolean rtcRegulatingControl, boolean tcDifferentRatios) {
        if (rtcRegulatingControl && tcDifferentRatios) {
            return ChangerType.REGULATING_CONTROL;
        }
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

    private ChangerType xfmrPhaseModel(double angle, boolean ptcRegulatingControl, boolean ptcDifferentAngles) {
        if (ptcRegulatingControl && ptcDifferentAngles) {
            return ChangerType.REGULATING_CONTROL;
        }
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

    private boolean shuntModel(Complex ysh) {
        if (ysh.equals(Complex.ZERO)) {
            return false;
        }
        return true;
    }

    public String code() {
        StringBuilder code = new StringBuilder();
        if (ratio1 != null) {
            code.append(ratioCode(ratio1));
        }
        if (phase1 != null) {
            code.append(phaseCode(phase1));
        }
        code.append(shuntCode(shunt1));
        code.append(shuntCode(shunt2));
        if (ratio2 != null) {
            code.append(ratioCode(ratio2));
        }
        if (phase2 != null) {
            code.append(phaseCode(phase2));
        }
        return code.toString();
    }

    private String shuntCode(boolean shunt) {
        StringBuilder code = new StringBuilder();
        code.append(shunt ? "Y" : "N");
        return code.toString();
    }

    private String ratioCode(ChangerType ratio) {
        StringBuilder code = new StringBuilder();
        switch (ratio) {
            case ABSENT:
                code.append("_");
                break;
            case FIXED:
                code.append("x");
                break;
            case CHANGEABLE_AT_NEUTRAL:
                code.append("1");
                break;
            case CHANGEABLE_AT_NON_NEUTRAL:
                code.append("r");
                break;
            case REGULATING_CONTROL:
                code.append("R");
                break;
        }
        return code.toString();
    }

    private String phaseCode(ChangerType phase) {
        StringBuilder code = new StringBuilder();
        switch (phase) {
            case ABSENT:
                code.append("_");
                break;
            case FIXED:
                code.append("x");
                break;
            case CHANGEABLE_AT_NEUTRAL:
                code.append("0");
                break;
            case CHANGEABLE_AT_NON_NEUTRAL:
                code.append("p");
                break;
            case REGULATING_CONTROL:
                code.append("P");
                break;
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
