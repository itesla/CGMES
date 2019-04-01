/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

/**
 * @author José Antonio Marqués <marquesja at aia.es>, Marcos de Miguel <demiguelm at aia.es>
 */
class CgmesEquipmentModelMapping {

    enum EndDistribution {
        END1, END2, SPLIT, RTC, END1_END2, X
    }

    // XXX LUMA Review prefer abbreviation Xfmr3
    // XXX LUMA Review use of word "distribution" here may be confusing 
    enum T3xDistribution {
        // XXX LUMA Review INSIDE, OUTSIDE ==> STAR_BUS_SIDE, NETWORK_SIDE 
        INSIDE, OUTSIDE, SPLIT
    }

    enum T3xPhaseAngleClock {
        OFF, INSIDE, OUTSIDE
    }

    public EndDistribution getLineBshunt() {
        return lineBshunt;
    }

    public void setLineBshunt(EndDistribution lineBshunt) {
        this.lineBshunt = lineBshunt;
    }

    public boolean isLineRatio0() {
        return lineRatio0;
    }

    public void setLineRatio0(boolean lineRatio0) {
        this.lineRatio0 = lineRatio0;
    }

    public EndDistribution getT2xRatio0() {
        return t2xRatio0;
    }

    public void setT2xRatio0(EndDistribution t2xRatio0) {
        this.t2xRatio0 = t2xRatio0;
    }

    public EndDistribution getT2xRatioPhase() {
        return t2xRatioPhase;
    }

    public void setT2xRatioPhase(EndDistribution t2xRatioPhase) {
        this.t2xRatioPhase = t2xRatioPhase;
    }

    public boolean isT2xPtc2Negate() {
        return t2xPtc2Negate;
    }

    public void setT2xPtc2Negate(boolean t2xPtc2Negate) {
        this.t2xPtc2Negate = t2xPtc2Negate;
    }

    public EndDistribution getT2xYShunt() {
        return t2xYShunt;
    }

    public void setT2xYShunt(EndDistribution t2xYShunt) {
        this.t2xYShunt = t2xYShunt;
    }

    public boolean isT2xPhaseAngleClock() {
        return t2xPhaseAngleClock;
    }

    public void setT2xPhaseAngleClock(boolean t2xPhaseAngleClock) {
        this.t2xPhaseAngleClock = t2xPhaseAngleClock;
    }

    public boolean isT2xPac2Negate() {
        return t2xPac2Negate;
    }

    public void setT2xPac2Negate(boolean t2xPac2Negate) {
        this.t2xPac2Negate = t2xPac2Negate;
    }

    public boolean isT3xRatio0Inside() {
        return t3xRatio0Inside;
    }

    public void setT3xRatio0Inside(boolean t3xRatio0Inside) {
        this.t3xRatio0Inside = t3xRatio0Inside;
    }

    public boolean isT3xRatioPhaseInside() {
        return t3xRatioPhaseInside;
    }

    public void setT3xRatioPhaseInside(boolean t3xRatioInside) {
        this.t3xRatioPhaseInside = t3xRatioInside;
    }

    public T3xDistribution getT3xYShunt() {
        return t3xYShunt;
    }

    public void setT3xYShunt(T3xDistribution t3xYShunt) {
        this.t3xYShunt = t3xYShunt;
    }

    public T3xPhaseAngleClock getT3xPhaseAngleClock() {
        return t3xPhaseAngleClock;
    }

    public void setT3xPhaseAngleClock(T3xPhaseAngleClock t3xPhaseAngleClock) {
        this.t3xPhaseAngleClock = t3xPhaseAngleClock;
    }

    public int length() {
        return toString().length();
    }

    @Override
    public String toString() {
        StringBuilder configuration = new StringBuilder();
        switch (lineBshunt) {
            case END1:
                configuration.append("Line_end1.");
                break;
            case END2:
                configuration.append("Line_end2.");
                break;
        }
        if (lineRatio0) {
            configuration.append("Line_ratio0_on.");
        }
        switch (t2xRatio0) {
            case END1:
                configuration.append("T2x_ratio0_end1.");
                break;
            case X:
                configuration.append("T2x_ratio0_x.");
                break;
            case RTC:
                configuration.append("T2x_ratio0_rtc.");
                break;
        }
        switch (t2xRatioPhase) {
            case END1:
                configuration.append("T2x_ratio_end1.");
                break;
            case END2:
                configuration.append("T2x_ratio_end2.");
                break;
            case X:
                configuration.append("T2x_ratio_x.");
                break;
        }
        if (t2xPtc2Negate) {
            configuration.append("T2x_ptc2_tabular_negate_on.");
        }
        switch (t2xYShunt) {
            case END2:
                configuration.append("T2x_yshunt_end2.");
                break;
            case END1_END2:
                configuration.append("T2x_yshunt_end1_end2.");
                break;
            case SPLIT:
                configuration.append("T2x_yshunt_split.");
                break;
        }
        if (t2xPhaseAngleClock) {
            configuration.append("T2x_clock_on.");
        }
        if (t2xPac2Negate) {
            configuration.append("T2x_pac2_negate_on.");
        }
        if (!t3xRatio0Inside) {
            configuration.append("T3x_ratio0_outside.");
        }
        if (t3xRatioPhaseInside) {
            configuration.append("T3x_ratio_inside.");
        }
        switch (t3xYShunt) {
            case INSIDE:
                configuration.append("T3x_yshunt_inside.");
                break;
            case SPLIT:
                configuration.append("T3x_yshunt_split.");
                break;
        }
        switch (t3xPhaseAngleClock) {
            case INSIDE:
                configuration.append("T3x_clock_on_inside.");
                break;
            case OUTSIDE:
                configuration.append("T3x_clock_on_outside.");
                break;
        }
        if (configuration.length() == 0) {
            configuration.append("Default.");
        }
        return configuration.toString();

    }

    EndDistribution    lineBshunt          = EndDistribution.SPLIT;
    boolean            lineRatio0          = false;
    EndDistribution    t2xRatio0           = EndDistribution.END2;
    EndDistribution    t2xRatioPhase       = EndDistribution.END1_END2;
    boolean            t2xPtc2Negate       = false;
    EndDistribution    t2xYShunt           = EndDistribution.END1;
    boolean            t2xPhaseAngleClock  = false;
    boolean            t2xPac2Negate       = false;
    boolean            t3xRatio0Inside     = true;
    boolean            t3xRatioPhaseInside = false;
    T3xDistribution    t3xYShunt           = T3xDistribution.OUTSIDE;
    T3xPhaseAngleClock t3xPhaseAngleClock  = T3xPhaseAngleClock.OFF;
}
