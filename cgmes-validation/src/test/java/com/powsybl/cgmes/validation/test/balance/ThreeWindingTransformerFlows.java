package com.powsybl.cgmes.validation.test.balance;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexUtils;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.RatioTapChangerStep;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.ThreeWindingsTransformer.Leg2or3;
import com.powsybl.iidm.network.ThreeWindingsTransformer.LegBase;
import com.powsybl.iidm.network.util.BranchData;

public final class ThreeWindingTransformerFlows {

    private ThreeWindingTransformerFlows() {
    }

    public static Complex calc(ThreeWindingsTransformer tx, Bus bus) {
        Terminal t = Terminals.get(tx, bus);
        Complex v0 = calcStarBusVoltage(tx);

        LegBase<?> leg = getLeg(tx, t, bus);
        String id = tx.getId() + "." + bus.getId();
        Complex ztr = ztr(leg, bus, tx);
        double r = ztr.getReal();
        double x = ztr.getImaginary();
        double gk = 0;
        double bk = 0;
        double g0 = 0;
        double b0 = 0;
        // In IIDM only the Leg1 has admittance to ground
        // And it is modeled at end corresponding to star bus
        // All (gk, bk) are zero in the IIDM model
        if (leg == tx.getLeg1()) {
            g0 = tx.getLeg1().getG();
            b0 = tx.getLeg1().getB();
        }
        Complex ratiok = ratiok(tx, leg);
        Complex rhoalphak = ratiok.reciprocal();
        double rhok = rhoalphak.abs();
        double alphak = rhoalphak.getArgument();
        double rho0 = 1;
        double alpha0 = 0;
        boolean buskMainComponent = true;
        boolean bus0MainComponent = true;
        boolean buskConnected = true;
        boolean bus0Connected = true;
        boolean applyReactanceCorrection = false;
        double epsilonX = 0;
        double expectedFlowPk = Double.NaN;
        double expectedFlowQk = Double.NaN;
        double expectedFlowP0 = Double.NaN;
        double expectedFlowQ0 = Double.NaN;
        BranchData branch = new BranchData(id,
                r, x,
                rhok, rho0,
                bus.getV(), v0.abs(),
                Math.toRadians(bus.getAngle()), v0.getArgument(),
                alphak, alpha0,
                gk, g0, bk, b0,
                expectedFlowPk, expectedFlowQk,
                expectedFlowP0, expectedFlowQ0,
                buskConnected, bus0Connected,
                buskMainComponent, bus0MainComponent,
                epsilonX, applyReactanceCorrection);
        return new Complex(branch.getComputedP1(), branch.getComputedQ1());
    }

    private static Complex calcStarBusVoltage(ThreeWindingsTransformer tx) {
        Bus bus1 = tx.getLeg1().getTerminal().getBusView().getBus();
        Bus bus2 = tx.getLeg2().getTerminal().getBusView().getBus();
        Bus bus3 = tx.getLeg3().getTerminal().getBusView().getBus();
        Complex v1 = voltage(bus1);
        Complex v2 = voltage(bus2);
        Complex v3 = voltage(bus3);
        Complex ytr1 = ztr(tx.getLeg1(), bus1, tx).reciprocal();
        Complex ytr2 = ztr(tx.getLeg2(), bus2, tx).reciprocal();
        Complex ytr3 = ztr(tx.getLeg3(), bus3, tx).reciprocal();
        Complex ysh01 = new Complex(tx.getLeg1().getG(), tx.getLeg1().getB());
        Complex ysh02 = new Complex(0, 0);
        Complex ysh03 = new Complex(0, 0);
        Complex a01 = new Complex(1, 0);
        Complex a1 = ratiok(tx, tx.getLeg1());
        Complex a02 = new Complex(1, 0);
        Complex a2 = ratiok(tx, tx.getLeg2());
        Complex a03 = new Complex(1, 0);
        Complex a3 = ratiok(tx, tx.getLeg3());

        // At star bus sum of currents from each end must be zero
        Complex y01 = ytr1.negate().divide(a01.conjugate().multiply(a1));
        Complex y02 = ytr2.negate().divide(a02.conjugate().multiply(a2));
        Complex y03 = ytr3.negate().divide(a03.conjugate().multiply(a3));
        Complex y0101 = ytr1.add(ysh01).divide(a01.conjugate().multiply(a01));
        Complex y0202 = ytr2.add(ysh02).divide(a02.conjugate().multiply(a02));
        Complex y0303 = ytr3.add(ysh03).divide(a03.conjugate().multiply(a03));
        return y01.multiply(v1).add(y02.multiply(v2)).add(y03.multiply(v3)).negate()
                .divide(y0101.add(y0202).add(y0303));
    }

    private static LegBase<?> getLeg(ThreeWindingsTransformer tx, Terminal t, Bus bus) {
        tx.getSide(t);
        switch (tx.getSide(t)) {
            case ONE:
                return tx.getLeg1();
            case TWO:
                return tx.getLeg2();
            case THREE:
                return tx.getLeg3();
            default:
                throw new PowsyblException("no leg for bus " + bus.getId() + " in transformer " + tx.getId());
        }
    }

    private static Complex voltage(Bus b) {
        if (b == null) {
            return Complex.ZERO;
        }
        return ComplexUtils.polar2Complex(b.getV(), Math.toRadians(b.getAngle()));
    }

    private static Complex ztr(LegBase<?> leg, Bus b, ThreeWindingsTransformer tx) {
        if (b == null) {
            return Complex.ZERO;
        }
        double dr = 0;
        double dx = 0;
        if (leg != tx.getLeg1()) {
            Leg2or3 leg2or3 = (Leg2or3) leg;
            if (leg2or3.getRatioTapChanger() != null) {
                RatioTapChangerStep step = leg2or3.getRatioTapChanger().getCurrentStep();
                dr = step.getR();
                dx = step.getX();
            }
        }
        return new Complex(leg.getR() * (1 + dr / 100), leg.getX() * (1 + dx / 100));
    }

    private static Complex ratiok(ThreeWindingsTransformer tx, LegBase<?> leg) {
        double ratedU0 = tx.getLeg1().getRatedU();
        double rhok = ratedU0 / leg.getRatedU();
        if (leg != tx.getLeg1()) {
            Leg2or3 leg2or3 = (Leg2or3) leg;
            if (leg2or3.getRatioTapChanger() != null) {
                RatioTapChangerStep step = leg2or3.getRatioTapChanger().getCurrentStep();
                rhok *= step.getRho();
            }
        }
        double alphak = 0;
        return ComplexUtils.polar2Complex(rhok, alphak).reciprocal();
    }
}
