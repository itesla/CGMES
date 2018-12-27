package com.powsybl.cgmes.validation.test.balance;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.conversion.elements.extensions.PhaseAngleClocksExtension;
import com.powsybl.cgmes.conversion.elements.extensions.RatioTapChangerExtension;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.RatioTapChangerStep;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.ThreeWindingsTransformer.Leg2or3;
import com.powsybl.iidm.network.ThreeWindingsTransformer.LegBase;
import com.powsybl.iidm.network.ThreeWindingsTransformer.Side;
import com.powsybl.iidm.network.util.BranchData;

public final class ThreeWindingsTransformerFlows {

    ThreeWindingsTransformerFlows(ThreeWindingsTransformer tx) {
        this.tx = tx;
        debug = false; // tx.getId().equals("_e67767ee-da47-4a30-81f2-0263b0294d43");
    }

    Complex calc(Bus bus) {
        if (bus == null) {
            return Complex.NaN;
        }
        Complex v0 = calcStarBusVoltage();

        Side side = tx.getSide(Terminals.get(tx, bus));
        String id = tx.getId() + "." + bus.getId();
        Complex ztr = ztr(side);
        double r = ztr.getReal();
        double x = ztr.getImaginary();
        double gk = 0;
        double bk = 0;
        double g0 = 0;
        double b0 = 0;
        // In IIDM only the Leg1 has admittance to ground
        // And it is modeled at end corresponding to star bus
        // All (gk, bk) are zero in the IIDM model
        if (side == Side.ONE) {
            g0 = tx.getLeg1().getG();
            b0 = tx.getLeg1().getB();
        }
        Complex ratiok = ratiok(side);
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

        // We do not get the voltage directly from the bus,
        // it could be adjusted by phase angle clocks on transformer ends
        Complex v = voltage(side);

        BranchData branch = new BranchData(id,
                r, x,
                rhok, rho0,
                v.abs(), v0.abs(),
                v.getArgument(), v0.getArgument(),
                alphak, alpha0,
                gk, g0, bk, b0,
                expectedFlowPk, expectedFlowQk,
                expectedFlowP0, expectedFlowQ0,
                buskConnected, bus0Connected,
                buskMainComponent, bus0MainComponent,
                epsilonX, applyReactanceCorrection);

        if (debug && LOG.isInfoEnabled()) {
            LOG.info("    3wtx flow {} {}", tx.getName(), tx.getId());
            LOG.info(String.format("        Vk     %10.4f  %10.4f", v.abs(), Math.toDegrees(v.getArgument())));
            LOG.info(String.format("        V0     %10.4f  %10.4f", v0.abs(), Math.toDegrees(v0.getArgument())));
            LOG.info(String.format("        r,x    %10.4f  %10.4f", r, x));
            LOG.info(String.format("        gk,bk  %10.4f  %10.4f", gk, bk));
            LOG.info(String.format("        g0,b0  %10.4f  %10.4f", g0, b0));
            LOG.info(String.format("        rho_k  %10.4f  %10.4f", rhok, alphak));
            LOG.info(String.format("        rho_0  %10.4f  %10.4f", rho0, alpha0));
            LOG.info(String.format("        S      %10.4f  %10.4f", branch.getComputedP1(), branch.getComputedQ1()));
        }
        return new Complex(branch.getComputedP1(), branch.getComputedQ1());
    }

    private Complex calcStarBusVoltage() {
        Complex v1 = voltage(Side.ONE);
        Complex v2 = voltage(Side.TWO);
        Complex v3 = voltage(Side.THREE);
        Complex ytr1 = ztr(Side.ONE).reciprocal();
        Complex ytr2 = ztr(Side.TWO).reciprocal();
        Complex ytr3 = ztr(Side.THREE).reciprocal();
        Complex ysh01 = new Complex(tx.getLeg1().getG(), tx.getLeg1().getB());
        Complex ysh02 = new Complex(0, 0);
        Complex ysh03 = new Complex(0, 0);
        Complex a01 = new Complex(1, 0);
        Complex a1 = ratiok(Side.ONE);
        Complex a02 = new Complex(1, 0);
        Complex a2 = ratiok(Side.TWO);
        Complex a03 = new Complex(1, 0);
        Complex a3 = ratiok(Side.THREE);

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

    private LegBase<?> getLeg(Side side) {
        switch (side) {
            case ONE:
                return tx.getLeg1();
            case TWO:
                return tx.getLeg2();
            case THREE:
                return tx.getLeg3();
        }
        String msg = String.format("getLeg. Bad side in transformer %s %s side %s", tx.getName(), tx.getId(), side);
        throw new PowsyblException(msg);
    }

    private Bus getBus(Side side) {
        Bus bus = getLeg(side).getTerminal().getBusView().getBus();
        if (bus == null) {
            String msg = String.format("voltage. Null bus at transformer %s %s side %s", tx.getName(), tx.getId(),
                    side);
            LOG.error(msg);
        }
        return bus;
    }

    private Complex voltage(Side side) {
        Bus bus = getBus(side);
        // Elering has some transformers with bus == null
        if (bus == null) {
            return Complex.NaN;
        }
        PhaseAngleClocksExtension clocks = tx.getExtension(PhaseAngleClocksExtension.class);
        int clock = 0;
        switch (side) {
            case ONE:
                clock = clocks == null ? 0 : clocks.clock1();
                break;
            case TWO:
                clock = clocks == null ? 0 : clocks.clock2();
                break;
            case THREE:
                clock = clocks == null ? 0 : clocks.clock3();
                break;
        }
        double theta = bus.getAngle() + clock * 30;
        return ComplexUtils.polar2Complex(bus.getV(), Math.toRadians(theta));
    }

    private Complex ztr(Side side) {
        LegBase<?> leg = getLeg(side);
        double dr = 0;
        double dx = 0;
        RatioTapChangerStep step = getCurrentStep(side);
        if (step != null) {
            dr = step.getR();
            dx = step.getX();
            if (debug) {
                LOG.info("ztr side {} impedance correction:", side);
                LOG.info("    r, x   = {}  {}", leg.getR(), leg.getX());
                LOG.info("    dr, dx = {}  {}", dr, dx);
                LOG.info("    r',x'  = {}  {}", leg.getR() * (1 + dr / 100), leg.getX() * (1 + dx / 100));
            }
        }
        return new Complex(leg.getR() * (1 + dr / 100), leg.getX() * (1 + dx / 100));
    }

    private RatioTapChangerStep getCurrentStep(Side side) {
        if (side == Side.ONE) {
            RatioTapChangerExtension e = tx.getExtension(RatioTapChangerExtension.class);
            if (e != null && e.getRatioTapChanger() != null) {
                return e.getRatioTapChanger().getCurrentStep();
            }
        } else {
            Leg2or3 leg2or3 = (Leg2or3) getLeg(side);
            if (leg2or3.getRatioTapChanger() != null) {
                return leg2or3.getRatioTapChanger().getCurrentStep();
            }
        }
        return null;
    }

    private Complex ratiok(Side side) {
        double ratedU0 = tx.getLeg1().getRatedU();
        LegBase<?> leg = getLeg(side);
        double rhok = ratedU0 / leg.getRatedU();
        RatioTapChangerStep step = getCurrentStep(side);
        if (step != null) {
            rhok *= step.getRho();
        }
        double alphak = 0;
        return ComplexUtils.polar2Complex(rhok, alphak).reciprocal();
    }

    private final ThreeWindingsTransformer tx;
    private final boolean debug;

    private static final Logger LOG = LoggerFactory.getLogger(ThreeWindingsTransformerFlows.class);
}
