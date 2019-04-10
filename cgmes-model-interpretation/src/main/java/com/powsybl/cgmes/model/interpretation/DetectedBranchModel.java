/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.interpretation.XfmrUtilities.PhaseData;
import com.powsybl.cgmes.model.interpretation.XfmrUtilities.RatioData;

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

    public DetectedBranchModel(Complex ysh1, Complex ysh2, RatioData ratio1, PhaseData phase1,
            RatioData ratio2, PhaseData phase2) {
        this.ratio1 = xfmrRatioModel(ratio1);
        this.phase1 = xfmrPhaseModel(phase1);
        this.shunt1 = shuntModel(ysh1);
        this.shunt2 = shuntModel(ysh2);
        this.ratio2 = xfmrRatioModel(ratio2);
        this.phase2 = xfmrPhaseModel(phase2);
    }

    private ChangerType xfmrRatioModel(RatioData ratio) {
        if (ratio.regulatingControl && ratio.changeable) {
            return ChangerType.REGULATING_CONTROL;
        }
        if (ratio.a == 1.0) {
            if (ratio.changeable) {
                return ChangerType.CHANGEABLE_AT_NEUTRAL;
            } else {
                return ChangerType.ABSENT;
            }
        } else {
            if (ratio.changeable) {
                return ChangerType.CHANGEABLE_AT_NON_NEUTRAL;
            } else {
                return ChangerType.FIXED;
            }
        }
    }

    private ChangerType xfmrPhaseModel(PhaseData phase) {
        if (phase.regulatingControl && phase.changeable) {
            return ChangerType.REGULATING_CONTROL;
        }
        if (phase.a == 1.0 && phase.angle == 0.0) {
            if (phase.changeable) {
                return ChangerType.CHANGEABLE_AT_NEUTRAL;
            } else {
                return ChangerType.ABSENT;
            }
        } else {
            if (phase.changeable) {
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
                code.append("n");
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
                code.append("m");
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
