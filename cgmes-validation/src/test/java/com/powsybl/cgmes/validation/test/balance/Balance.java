package com.powsybl.cgmes.validation.test.balance;

import java.util.stream.Stream;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoWindingsTransformer;

class Balance {
    static final Balance PERFECT = new Balance(Complex.ZERO);

    private Balance(Complex balance) {
        this.balance = balance;
    }

    Balance(Bus bus, Output output) {
        this.bus = bus;
        this.output = output;
        this.balance = calc();
        this.alpha = Double.NaN;
        this.hasPhaseTapChange = bus.getTwoWindingsTransformerStream()
                .filter(tx -> hasPhaseTapChange(tx.getPhaseTapChanger()))
                .findAny()
                .map(tx -> {
                    alpha = tx.getPhaseTapChanger().getCurrentStep().getAlpha();
                    elementPhaseTapChanger = tx;
                    return tx;
                })
                .isPresent();
        this.hasRho2w = bus.getTwoWindingsTransformerStream()
                .filter(tx -> tx.getRatioTapChanger() != null &&
                        tx.getRatioTapChanger().getCurrentStep() != null &&
                        tx.getRatioTapChanger().getCurrentStep().getRho() != 1)
                .findAny()
                .map(tx -> {
                    rho2w = tx.getRatioTapChanger().getCurrentStep().getRho();
                    return tx;
                })
                .isPresent();
        this.hasRho3w2 = bus.getThreeWindingsTransformerStream()
                .filter(tx -> tx.getLeg2().getRatioTapChanger() != null &&
                        tx.getLeg2().getRatioTapChanger().getCurrentStep() != null &&
                        tx.getLeg2().getRatioTapChanger().getCurrentStep().getRho() != 1)
                .findAny()
                .map(tx -> {
                    rho3w2 = tx.getLeg2().getRatioTapChanger().getCurrentStep().getRho();
                    return tx;
                })
                .isPresent();
        this.hasRho3w3 = bus.getThreeWindingsTransformerStream()
                .filter(tx -> tx.getLeg3().getRatioTapChanger() != null &&
                        tx.getLeg3().getRatioTapChanger().getCurrentStep() != null &&
                        tx.getLeg3().getRatioTapChanger().getCurrentStep().getRho() != 1)
                .findAny()
                .map(tx -> {
                    rho3w3 = tx.getLeg3().getRatioTapChanger().getCurrentStep().getRho();
                    return tx;
                })
                .isPresent();
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

    Complex calc() {
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

    private Bus bus;
    private Output output;
    private Complex balance;

    private boolean hasPhaseTapChange;
    private double alpha;
    private TwoWindingsTransformer elementPhaseTapChanger;
    private boolean hasRho2w;
    private double rho2w;
    private boolean hasRho3w2;
    private double rho3w2;
    private boolean hasRho3w3;
    private double rho3w3;

    private static boolean calc3wtxFlows = true;
}
