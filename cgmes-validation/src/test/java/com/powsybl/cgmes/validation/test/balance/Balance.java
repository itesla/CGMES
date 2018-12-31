/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test.balance;

import java.util.Optional;
import java.util.stream.Stream;

import com.powsybl.iidm.network.*;
import org.apache.commons.math3.complex.Complex;

class Balance {
    static final Balance PERFECT = new Balance(Complex.ZERO);

    private Balance(Complex balance) {
        bus = null;
        output = null;
        this.balance = balance;
        hasPhaseTapChange = false;
        alpha = Double.NaN;
        elementPhaseTapChanger = null;
        hasRho2w = false;
        rho2w = Double.NaN;
        hasRho3w2 = false;
        rho3w2 = Double.NaN;
        hasRho3w3 = false;
        rho3w3 = Double.NaN;
    }

    Balance(Bus bus, Output output) {
        this.bus = bus;
        this.output = output;
        balance = calc();

        Optional<TwoWindingsTransformer> ptx = bus.getTwoWindingsTransformerStream()
                .filter(t -> hasPhaseTapChange(t.getPhaseTapChanger()))
                .findAny();
        if (ptx.isPresent()) {
            hasPhaseTapChange = true;
            elementPhaseTapChanger = ptx.get();
            alpha = ptx.get().getPhaseTapChanger().getCurrentStep().getAlpha();
        } else {
            hasPhaseTapChange = false;
            elementPhaseTapChanger = null;
            alpha = Double.NaN;
        }

        Optional<TwoWindingsTransformer> rtx = bus.getTwoWindingsTransformerStream()
                .filter(tx -> tx.getRatioTapChanger() != null &&
                        tx.getRatioTapChanger().getCurrentStep() != null &&
                        tx.getRatioTapChanger().getCurrentStep().getRho() != 1)
                .findAny();
        if (rtx.isPresent()) {
            hasRho2w = true;
            rho2w = rtx.get().getRatioTapChanger().getCurrentStep().getRho();
        } else {
            hasRho2w = false;
            rho2w = Double.NaN;
        }

        Optional<ThreeWindingsTransformer> r32tx = bus.getThreeWindingsTransformerStream()
                .filter(tx -> tx.getLeg2().getRatioTapChanger() != null &&
                        tx.getLeg2().getRatioTapChanger().getCurrentStep() != null &&
                        tx.getLeg2().getRatioTapChanger().getCurrentStep().getRho() != 1)
                .findAny();
        if (r32tx.isPresent()) {
            hasRho3w2 = true;
            rho3w2 = r32tx.get().getLeg2().getRatioTapChanger().getCurrentStep().getRho();
        } else {
            hasRho3w2 = false;
            rho3w2 = Double.NaN;

        }
        Optional<ThreeWindingsTransformer> r33tx = bus.getThreeWindingsTransformerStream()
                .filter(tx -> tx.getLeg2().getRatioTapChanger() != null &&
                        tx.getLeg2().getRatioTapChanger().getCurrentStep() != null &&
                        tx.getLeg2().getRatioTapChanger().getCurrentStep().getRho() != 1)
                .findAny();
        if (r33tx.isPresent()) {
            hasRho3w3 = true;
            rho3w3 = r33tx.get().getLeg2().getRatioTapChanger().getCurrentStep().getRho();
        } else {
            hasRho3w3 = false;
            rho3w3 = Double.NaN;
        }
    }

    public String toString() {
        return String.format("{bus: %s, dp: %f, dq: %f, ptc: %b, ptc.alpha: %f}",
                bus,
                balance.getReal(),
                balance.getImaginary(),
                hasPhaseTapChange,
                alpha);
    }

    Bus bus() {
        return bus;
    }

    Complex balance() {
        return balance;
    }

    double error() {
        return balance.abs();
    }

    boolean hasPhaseTapChange() {
        return hasPhaseTapChange;
    }

    TwoWindingsTransformer elementPhaseTapChanger() {
        return elementPhaseTapChanger;
    }

    double alpha() {
        return alpha;
    }

    boolean hasRho2w() {
        return hasRho2w;
    }

    double rho2w() {
        return rho2w;
    }

    boolean hasRho3w2() {
        return hasRho3w2;
    }

    double rho3w2() {
        return rho3w2;
    }

    boolean hasRho3w3() {
        return hasRho3w3;
    }

    double rho3w3() {
        return rho3w3;
    }

    private Complex calc() {
        output.bus(bus);
        Stream<Stream<Complex>> ss = Stream.of(
                bus.getLoadStream()
                        .map(e -> flow("load", e, e.getTerminal())),
                bus.getGeneratorStream()
                        .map(e -> flow("gen", e, e.getTerminal())),
                bus.getDanglingLineStream()
                        .map(e -> flow("dngl", e, e.getTerminal())),
                bus.getShuntCompensatorStream()
                        .map(e -> flowq("shunt", e, e.getTerminal())),
                bus.getStaticVarCompensatorStream()
                        .map(e -> flowq("svc", e, e.getTerminal())),
                bus.getLineStream()
                        .map(e -> flow("line", e, Terminals.get(e, bus))),
                bus.getTwoWindingsTransformerStream()
                        .map(e -> flow("2wtx", e, Terminals.get(e, bus))),
                threeWindingsTransformerFlows());
        Complex b = ss
                .flatMap(c -> c)
                .filter(c -> !c.isNaN())
                .reduce(Complex::add)
                .orElse(Complex.NaN);
        output.pq("SUM", b.getReal(), b.getImaginary());
        return b;
    }

    static void configureCalc3wtxFlows(boolean b) {
        calc3wtxFlows = b;
    }

    private Stream<Complex> threeWindingsTransformerFlows() {
        if (calc3wtxFlows) {
            return bus.getThreeWindingsTransformerStream()
                    .map(tx -> {
                        Complex f = new ThreeWindingsTransformerFlows(tx).calc(bus);
                        output.pq("3wtx", f.getReal(), f.getImaginary(), tx.getName(), tx.getId());
                        return f;
                    });
        } else {
            return bus.getThreeWindingsTransformerStream()
                    .map(tx -> flow("3wtx", tx, Terminals.get(tx, bus)));
        }
    }

    private Complex flow(String eqt, Identifiable<?> eq, Terminal t) {
        output.pq(eqt, eq, t);
        return new Complex(t.getP(), t.getQ());
    }

    private Complex flowq(String eqt, Identifiable<?> eq, Terminal t) {
        output.q(eqt, eq, t);
        return new Complex(0, t.getQ());
    }

    private boolean hasPhaseTapChange(PhaseTapChanger ptc) {
        // Only if it is a "genuine" phase tap change,
        // We allow phase tap changes from "phase angle clock"
        if (PhaseAngleClocksAnalyzer.isPhaseAngleClock(ptc)) {
            return false;
        }
        return ptc != null &&
                ptc.getCurrentStep() != null &&
                ptc.getCurrentStep().getAlpha() != 0;
    }

    private final Bus bus;
    private final Output output;
    private final Complex balance;

    private final boolean hasPhaseTapChange;
    private final double alpha;
    private final TwoWindingsTransformer elementPhaseTapChanger;
    private final boolean hasRho2w;
    private final double rho2w;
    private final boolean hasRho3w2;
    private final double rho3w2;
    private final boolean hasRho3w3;
    private final double rho3w3;

    private static boolean calc3wtxFlows = true;
}
