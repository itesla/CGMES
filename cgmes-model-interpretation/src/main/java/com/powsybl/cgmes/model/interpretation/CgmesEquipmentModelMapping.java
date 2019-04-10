/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class CgmesEquipmentModelMapping {

    public enum LineShuntMappingAlternative {
        END1, END2, SPLIT
    }

    public enum Xfmr2ShuntMappingAlternative {
        END1, END2, SPLIT, END1_END2
    }

    public enum Xfmr2RatioPhaseMappingAlternative {
        END1, END2, END1_END2, RTC, X
    }

    public enum Xfmr2PhaseAngleClockAlternative {
        OFF, END1_END2
    }

    public enum Xfmr3RatioPhaseMappingAlternative {
        STAR_BUS_SIDE, NETWORK_SIDE
    }

    public enum Xfmr3ShuntMappingAlternative {
        STAR_BUS_SIDE, NETWORK_SIDE, SPLIT
    }

    public enum Xfmr3PhaseAngleClockAlternative {
        OFF, STAR_BUS_SIDE, NETWORK_SIDE
    }

    public LineShuntMappingAlternative getLineBshunt() {
        return lineBshunt;
    }

    public void setLineBshunt(LineShuntMappingAlternative lineBshunt) {
        this.lineBshunt = lineBshunt;
    }

    public boolean isLineRatio0() {
        return lineRatio0;
    }

    public void setLineRatio0(boolean lineRatio0) {
        this.lineRatio0 = lineRatio0;
    }

    public Xfmr2RatioPhaseMappingAlternative getXfmr2Ratio0() {
        return xfmr2Ratio0;
    }

    public void setXfmr2Ratio0(Xfmr2RatioPhaseMappingAlternative xfmr2Ratio0) {
        this.xfmr2Ratio0 = xfmr2Ratio0;
    }

    public Xfmr2RatioPhaseMappingAlternative getXfmr2RatioPhase() {
        return xfmr2RatioPhase;
    }

    public void setXfmr2RatioPhase(Xfmr2RatioPhaseMappingAlternative xfmr2RatioPhase) {
        this.xfmr2RatioPhase = xfmr2RatioPhase;
    }

    public boolean isXfmr2Ptc2Negate() {
        return xfmr2Ptc2Negate;
    }

    public void setXfmr2Ptc2Negate(boolean xfmr2Ptc2Negate) {
        this.xfmr2Ptc2Negate = xfmr2Ptc2Negate;
    }

    public Xfmr2ShuntMappingAlternative getXfmr2YShunt() {
        return xfmr2YShunt;
    }

    public void setXfmr2YShunt(Xfmr2ShuntMappingAlternative xfmr2YShunt) {
        this.xfmr2YShunt = xfmr2YShunt;
    }

    public Xfmr2PhaseAngleClockAlternative getXfmr2PhaseAngleClock() {
        return xfmr2PhaseAngleClock;
    }

    public void setXfmr2PhaseAngleClock(Xfmr2PhaseAngleClockAlternative xfmr2PhaseAngleClock) {
        this.xfmr2PhaseAngleClock = xfmr2PhaseAngleClock;
    }

    public boolean isXfmr2Pac2Negate() {
        return xfmr2Pac2Negate;
    }

    public void setXfmr2Pac2Negate(boolean xfmr2Pac2Negate) {
        this.xfmr2Pac2Negate = xfmr2Pac2Negate;
    }

    public Xfmr3RatioPhaseMappingAlternative getXfmr3Ratio0StarBusSide() {
        return xfmr3Ratio0StarBusSide;
    }

    public void setXfmr3Ratio0StarBusSide(Xfmr3RatioPhaseMappingAlternative xfmr3Ratio0StarBusSide) {
        this.xfmr3Ratio0StarBusSide = xfmr3Ratio0StarBusSide;
    }

    public Xfmr3RatioPhaseMappingAlternative getXfmr3RatioPhaseStarBusSide() {
        return xfmr3RatioPhaseStarBusSide;
    }

    public void setXfmr3RatioPhaseStarBusSide(Xfmr3RatioPhaseMappingAlternative xfmr3RatioStarBusSide) {
        this.xfmr3RatioPhaseStarBusSide = xfmr3RatioStarBusSide;
    }

    public Xfmr3ShuntMappingAlternative getXfmr3YShunt() {
        return xfmr3YShunt;
    }

    public void setXfmr3YShunt(Xfmr3ShuntMappingAlternative xfmr3YShunt) {
        this.xfmr3YShunt = xfmr3YShunt;
    }

    public Xfmr3PhaseAngleClockAlternative getXfmr3PhaseAngleClock() {
        return xfmr3PhaseAngleClock;
    }

    public void setXfmr3PhaseAngleClock(Xfmr3PhaseAngleClockAlternative xfmr3PhaseAngleClock) {
        this.xfmr3PhaseAngleClock = xfmr3PhaseAngleClock;
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
            default:
                break;
        }
        if (lineRatio0) {
            configuration.append("Line_ratio0_on.");
        }
        switch (xfmr2Ratio0) {
            case END1:
                configuration.append("Xfmr2_ratio0_end1.");
                break;
            case X:
                configuration.append("Xfmr2_ratio0_x.");
                break;
            case RTC:
                configuration.append("Xfmr2_ratio0_rtc.");
                break;
            default:
                break;
        }
        switch (xfmr2RatioPhase) {
            case END1:
                configuration.append("Xfmr2_ratio_end1.");
                break;
            case END2:
                configuration.append("Xfmr2_ratio_end2.");
                break;
            case X:
                configuration.append("Xfmr2_ratio_x.");
                break;
            default:
                break;
        }
        if (xfmr2Ptc2Negate) {
            configuration.append("Xfmr2_ptc2_tabular_negate_on.");
        }
        switch (xfmr2YShunt) {
            case END2:
                configuration.append("Xfmr2_yshunt_end2.");
                break;
            case END1_END2:
                configuration.append("Xfmr2_yshunt_end1_end2.");
                break;
            case SPLIT:
                configuration.append("Xfmr2_yshunt_split.");
                break;
            default:
                break;
        }
        switch (xfmr2PhaseAngleClock) {
            case END1_END2:
                configuration.append("Xfmr2_clock_on_end1_end2.");
                break;
            default:
                break;
        }
        if (xfmr2Pac2Negate) {
            configuration.append("Xfmr2_pac2_negate_on.");
        }
        switch (xfmr3Ratio0StarBusSide) {
            case NETWORK_SIDE:
                configuration.append("Xfmr3_ratio0_network_side.");
                break;
            default:
                break;
        }
        switch (xfmr3RatioPhaseStarBusSide) {
            case STAR_BUS_SIDE:
                configuration.append("Xfmr3_ratio_star_bus_side.");
                break;
            default:
                break;
        }
        switch (xfmr3YShunt) {
            case STAR_BUS_SIDE:
                configuration.append("Xfmr3_yshunt_star_bus_side.");
                break;
            case SPLIT:
                configuration.append("Xfmr3_yshunt_split.");
                break;
            default:
                break;
        }
        switch (xfmr3PhaseAngleClock) {
            case STAR_BUS_SIDE:
                configuration.append("Xfmr3_clock_on_star_bus_side.");
                break;
            case NETWORK_SIDE:
                configuration.append("Xfmr3_clock_on_network_side.");
                break;
            default:
                break;
        }
        if (configuration.length() == 0) {
            configuration.append("Default.");
        }
        return configuration.toString();

    }

    LineShuntMappingAlternative       lineBshunt                 = LineShuntMappingAlternative.SPLIT;
    boolean                           lineRatio0                 = false;
    Xfmr2RatioPhaseMappingAlternative xfmr2Ratio0                = Xfmr2RatioPhaseMappingAlternative.END2;
    Xfmr2RatioPhaseMappingAlternative xfmr2RatioPhase            = Xfmr2RatioPhaseMappingAlternative.END1_END2;
    boolean                           xfmr2Ptc2Negate            = false;
    Xfmr2ShuntMappingAlternative      xfmr2YShunt                = Xfmr2ShuntMappingAlternative.END1;
    Xfmr2PhaseAngleClockAlternative   xfmr2PhaseAngleClock       = Xfmr2PhaseAngleClockAlternative.OFF;
    boolean                           xfmr2Pac2Negate            = false;
    Xfmr3RatioPhaseMappingAlternative xfmr3Ratio0StarBusSide     = Xfmr3RatioPhaseMappingAlternative.STAR_BUS_SIDE;
    Xfmr3RatioPhaseMappingAlternative xfmr3RatioPhaseStarBusSide = Xfmr3RatioPhaseMappingAlternative.NETWORK_SIDE;
    Xfmr3ShuntMappingAlternative      xfmr3YShunt                = Xfmr3ShuntMappingAlternative.NETWORK_SIDE;
    Xfmr3PhaseAngleClockAlternative   xfmr3PhaseAngleClock       = Xfmr3PhaseAngleClockAlternative.OFF;
}
