package com.powsybl.cgmes.validation.test.balance;

import java.util.stream.Stream;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Terminal;

class Balance {
    static final Balance PERFECT = new Balance(Complex.ZERO);

    private Balance(Complex balance) {
        this.balance = balance;
    }

    Balance(Bus bus, Output output) {
        this.bus = bus;
        this.output = output;
        this.balance = calc();
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
                    .map(e -> ThreeWindingTransformerFlows.calc(e, bus));
        } else {
            return bus.getThreeWindingsTransformerStream()
                    .map(e -> flow("3wtx", e, Terminals.get(e, bus)));
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

    private Bus bus;
    private Output output;
    private Complex balance;

    private static boolean calc3wtxFlows = true;
}
