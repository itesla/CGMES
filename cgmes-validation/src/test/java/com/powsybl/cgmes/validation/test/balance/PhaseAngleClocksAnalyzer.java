/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test.balance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.conversion.elements.extensions.PhaseAngleClocksExtension;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChangerStep;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;

class PhaseAngleClocksAnalyzer {

    void analyze(Network network) {
        Results r = new Results();
        network.getThreeWindingsTransformerStream().forEach(tx -> {
            int[] clocks = clocks(tx);
            r.seen(clocks);
            if (validClocks(clocks)) {
                if (tx.getLeg1().getTerminal().getBusView().getBus() != null
                        && tx.getLeg2().getTerminal().getBusView().getBus() != null
                        && tx.getLeg3().getTerminal().getBusView().getBus() != null) {
                    double theta1 = tx.getLeg1().getTerminal().getBusView().getBus().getAngle();
                    double theta2 = tx.getLeg2().getTerminal().getBusView().getBus().getAngle();
                    double theta3 = tx.getLeg3().getTerminal().getBusView().getBus().getAngle();
                    boolean validAngles = !(Double.isNaN(theta1) || Double.isNaN(theta2) || Double.isNaN(theta3));
                    if (validAngles) {
                        LOG.info("PAC 3wtx {} {}", tx.getName(), tx.getId());
                        double[] thetas = {theta1, theta2, theta3};
                        analyze(thetas, clocks, r);
                    }
                }
            }
        });
        network.getTwoWindingsTransformerStream().forEach(tx -> {
            int[] clocks = clocks(tx);
            r.seen(clocks);
            if (validClocks(clocks)) {
                if (tx.getTerminal1().getBusView().getBus() != null
                        && tx.getTerminal2().getBusView().getBus() != null) {
                    double theta1 = tx.getTerminal1().getBusView().getBus().getAngle();
                    double theta2 = tx.getTerminal2().getBusView().getBus().getAngle();
                    boolean validAngles = !(Double.isNaN(theta1) || Double.isNaN(theta2));
                    if (validAngles) {
                        LOG.info("PAC 2wtx {} {}", tx.getName(), tx.getId());
                        double[] thetas = {theta1, theta2};
                        analyze(thetas, clocks, r);
                    }
                }
            }
        });
        r.log();
    }

    private int[] clocks(ThreeWindingsTransformer tx) {
        PhaseAngleClocksExtension clocks = tx.getExtension(PhaseAngleClocksExtension.class);
        if (clocks == null) {
            return new int[]{0, 0, 0};
        }
        return new int[]{clocks.clock1(), clocks.clock2(), clocks.clock3()};
    }

    private int[] clocks(TwoWindingsTransformer tx) {
        PhaseTapChanger ptc = tx.getPhaseTapChanger();
        int[] clocks = {0, 0};
        if (isPhaseAngleClock(ptc)) {
            double alpha = ptc.getCurrentStep().getAlpha();
            int side = alpha > 0 ? 1 : 2;
            int clock = (int) Math.floor(alpha / 30);
            clocks[side - 1] = Math.abs(clock);
        }
        return clocks;
    }

    public static boolean isPhaseAngleClock(PhaseTapChanger ptc) {
        // A phase tap changer with a single step where the alpha is 30x and ratio 1
        if (ptc == null) {
            return false;
        }
        if (ptc.getStepCount() > 1) {
            return false;
        }
        PhaseTapChangerStep step = ptc.getCurrentStep();
        if (step.getRho() != 1.0) {
            return false;
        }
        double alpha = step.getAlpha();
        return alpha % 30 == 0;
    }

    private boolean validClocks(int[] clocks) {
        // At least one clock > 0 and != 6
        for (int clock : clocks) {
            if (clock > 0 && clock != 6) {
                return true;
            }
        }
        return false;
    }

    private void analyze(double[] thetas, int[] clocks, Results r) {
        double[] thetasadd = new double[thetas.length];
        double[] thetassub = new double[thetas.length];
        for (int k = 0; k < thetas.length; k++) {
            thetasadd[k] = normalizeAngleDegrees(thetas[k] + 30 * clocks[k]);
            thetassub[k] = normalizeAngleDegrees(thetas[k] - 30 * clocks[k]);
        }
        double diff = 0;
        double diffadd = 0;
        double diffsub = 0;
        for (int k = 1; k < thetas.length; k++) {
            diff += Math.abs(thetas[k] - thetas[k - 1]);
            diffadd += Math.abs(thetasadd[k] - thetasadd[k - 1]);
            diffsub += Math.abs(thetassub[k] - thetassub[k - 1]);
        }

        log("thetas", thetas, diff);
        log("clocks", clocks);
        log("add   ", thetasadd, diffadd);
        log("sub   ", thetassub, diffsub);
        r.newResult(diff, diffadd, diffsub);
    }

    private double normalizeAngleDegrees(double alpha) {
        return alpha - 360 * Math.floor((alpha + 180) / 360);
    }

    private void log(String label, double[] angles, double diff) {
        StringBuilder s = new StringBuilder("PAC    ");
        s.append(label);
        for (double angle : angles) {
            s.append(String.format("%10.4f  ", angle));
        }
        s.append(String.format("%10.4f", diff));
        LOG.info(s.toString());
    }

    private void log(String label, int[] clocks) {
        StringBuilder s = new StringBuilder("PAC    ");
        s.append(label);
        for (int clock : clocks) {
            s.append(String.format("%10d  ", clock));
        }
        LOG.info(s.toString());
    }

    static class Results {
        int betterIgnore;
        int betterAdd;
        int betterSub;
        final int[] seen = new int[12];

        void seen(int[] clocks) {
            for (int k = 1; k < clocks.length; k++) {
                seen[clocks[k]]++;
            }
        }

        void newResult(double diff, double diffadd, double diffsub) {
            boolean isBetterIgnore = diffadd >= diff && diffsub >= diff;
            boolean isBetterAdd = !isBetterIgnore && diffadd <= diffsub;
            LOG.info("PAC    {} is better", isBetterIgnore ? "ignore" : isBetterAdd ? "add" : "sub");
            if (isBetterIgnore) {
                betterIgnore++;
            } else {
                if (isBetterAdd) {
                    betterAdd++;
                } else {
                    betterSub++;
                }
            }
        }

        void log() {
            LOG.info("PAC Totals. Is better to ...");
            LOG.info("PAC     ignore {}", betterIgnore);
            LOG.info("PAC     add    {}", betterAdd);
            LOG.info("PAC     sub    {}", betterSub);
            LOG.info("PAC Seen values for phase angle clock");
            for (int k = 1; k < seen.length; k++) {
                LOG.info(String.format("PAC     seen[%2d] = %4d", k, seen[k]));
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PhaseAngleClocksAnalyzer.class);
}
