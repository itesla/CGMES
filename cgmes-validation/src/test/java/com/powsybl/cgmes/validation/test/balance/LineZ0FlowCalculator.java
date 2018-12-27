package com.powsybl.cgmes.validation.test.balance;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.iidm.network.Branch.Side;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TopologyVisitor;
import com.powsybl.iidm.network.TwoWindingsTransformer;

class LineZ0FlowCalculator implements TopologyVisitor {

    private final Line lineZ0;
    private final Bus bus;
    private final boolean considerPhaseTapChangesUnknown;
    private final Set<Line> failIfFoundLines; // Avoid loops

    private boolean known;
    private double netP;
    private double netQ;
    private String reasonUnknown;

    private final boolean debug;
    private final String indent;

    public LineZ0FlowCalculator(Line line, Bus bus, boolean considerPhaseTapChangesUnknown) {
        this(line, bus, considerPhaseTapChangesUnknown, Collections.emptySet(), false);
    }

    public LineZ0FlowCalculator(
            Line line,
            Bus bus,
            boolean considerPhaseTapChangesUnknown,
            Set<Line> failIfFoundLines,
            boolean debug) {
        this.lineZ0 = line;
        this.failIfFoundLines = failIfFoundLines;
        this.bus = bus;
        this.considerPhaseTapChangesUnknown = considerPhaseTapChangesUnknown;
        this.known = true;
        this.netP = 0;
        this.netQ = 0;
        this.indent = String.join("", Collections.nCopies(failIfFoundLines.size(), "    "));

        this.debug = debug
                // Twineham TWIN-2
                || lineZ0.getId().equals("_6f827cbe-0aeb-435f-8441-ccd4dfbc0e7d");

        log("entering");
        bus.visitConnectedEquipments(this);
        if (!known) {
            log("unknown : " + reasonUnknown);
        }
        log("leaving");
    }

    static void calc(Network network, boolean considerPhaseTapChangesUnknown) {
        network.getLines().forEach(line -> {
            if (isZ0(line)) {
                Terminal[] ts = sortedTerminals(line);
                if (!calc(line, ts[0], considerPhaseTapChangesUnknown)) {
                    calc(line, ts[1], considerPhaseTapChangesUnknown);
                }
            }
        });
    }

    private static Terminal[] sortedTerminals(Line line) {
        // Try to calculate first the balance at the bus with less connected terminals
        Terminal t1 = line.getTerminal1();
        Terminal t2 = line.getTerminal2();
        Bus b1 = t1.getBusView().getBus();
        Bus b2 = t2.getBusView().getBus();
        int n1 = b1 != null ? b1.getConnectedTerminalCount() : Integer.MAX_VALUE;
        int n2 = b2 != null ? b2.getConnectedTerminalCount() : Integer.MAX_VALUE;
        if (n1 < n2) {
            return new Terminal[] {t1, t2};
        } else {
            return new Terminal[] {t2, t1};
        }
    }

    private static boolean calc(Line line, Terminal t, boolean considerPhaseTapChangesUnknown) {
        return calc(line, t, considerPhaseTapChangesUnknown, Collections.emptySet(), false);
    }

    private static boolean calc(
            Line line,
            Terminal t,
            boolean considerPhaseTapChangesUnknown,
            Set<Line> failIfFoundLines,
            boolean debug) {
        // A line segment where end buses are the same bus can be assigned flow zero
        if (line.getTerminal1().getBusView().getBus() == line.getTerminal2().getBusView().getBus()) {
            line.getTerminal1().setP(0);
            line.getTerminal1().setQ(0);
            line.getTerminal2().setP(0);
            line.getTerminal2().setQ(0);
            return true;
        }
        Bus bus = t.getBusView().getBus();
        if (bus != null) {
            LineZ0FlowCalculator around = new LineZ0FlowCalculator(
                    line,
                    bus,
                    considerPhaseTapChangesUnknown,
                    failIfFoundLines,
                    debug);
            if (around.flowsAreKnown()) {
                around.setZ0Flows();
                return true;
            }
        }
        return false;
    }

    public boolean flowsAreKnown() {
        return known;
    }

    public double getNetP() {
        return netP;
    }

    public double getNetQ() {
        return netQ;
    }

    private void addFlow(Terminal t) {
        if (Double.isNaN(t.getP()) || Double.isNaN(t.getQ())) {
            known = false;
            reasonUnknown = String.format("flow NaN %10.4f %10.4f %s", t.getP(), t.getQ(), t.getConnectable().getId());
        } else {
            netP += t.getP();
            netQ += t.getQ();
            log(String.format("add flow = %10.4f %10.4f %s", t.getP(), t.getQ(), t.getConnectable().getName()));
        }
    }

    private void addFlowQ(Terminal t) {
        if (Double.isNaN(t.getQ())) {
            known = false;
            reasonUnknown = String.format("flowQ NaN %s", t.getConnectable().getId());
        } else {
            netQ += t.getQ();
        }
    }

    void setZ0Flows() {
        Terminal t = Terminals.get(lineZ0, bus);
        t.setP(-netP);
        t.setQ(-netQ);
        log(String.format("assign net flow = %10.4f %10.4f", t.getP(), t.getQ()));
        Terminal othert = Terminals.getOther(lineZ0, bus);
        othert.setP(netP);
        othert.setQ(netQ);
    }

    @Override
    public void visitBusbarSection(BusbarSection section) {
    }

    @Override
    public void visitLine(Line line, Side side) {
        if (failIfFoundLines.contains(line)) {
            known = false;
            reasonUnknown = String.format("Fail if found line %s %s", line.getName(), line.getId());
        } else if (line != this.lineZ0) {
            if (isZ0(line)) {
                // Try to solve the flow for the other line only on its other side
                // If we succeed we can continue and keep the balance "known"
                Terminal other = bus != line.getTerminal1().getBusView().getBus()
                        ? line.getTerminal1()
                        : line.getTerminal2();
                assert other.getBusView().getBus() != bus;
                Set<Line> failIfFoundLinesPlusThis = new HashSet<>(failIfFoundLines);
                failIfFoundLinesPlusThis.add(lineZ0);
                if (calc(line, other, considerPhaseTapChangesUnknown, failIfFoundLinesPlusThis, debug)) {
                    addFlow(line.getTerminal(side));
                } else {
                    known = false;
                    reasonUnknown = String.format("Other z0 line %s %s", line.getName(), line.getId());
                }
            } else {
                addFlow(line.getTerminal(side));
            }
        }
    }

    @Override
    public void visitTwoWindingsTransformer(TwoWindingsTransformer transformer, Side side) {
        if (considerPhaseTapChangesUnknown && hasPhaseTapChange(transformer.getPhaseTapChanger())) {
            known = false;
            reasonUnknown = String.format("Phase tap change not clock %s", transformer.getId());
        } else {
            addFlow(transformer.getTerminal(side));
        }
    }

    @Override
    public void visitThreeWindingsTransformer(ThreeWindingsTransformer transformer,
            com.powsybl.iidm.network.ThreeWindingsTransformer.Side side) {
        addFlow(transformer.getTerminal(side));
    }

    @Override
    public void visitGenerator(Generator generator) {
        addFlow(generator.getTerminal());
    }

    @Override
    public void visitLoad(Load load) {
        addFlow(load.getTerminal());
    }

    @Override
    public void visitShuntCompensator(ShuntCompensator sc) {
        addFlowQ(sc.getTerminal());
    }

    @Override
    public void visitDanglingLine(DanglingLine danglingLine) {
        addFlow(danglingLine.getTerminal());
    }

    @Override
    public void visitStaticVarCompensator(StaticVarCompensator staticVarCompensator) {
        addFlowQ(staticVarCompensator.getTerminal());
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

    private static boolean isZ0(Line line) {
        return line.getR() <= 1e-4 && line.getX() <= 1e-4;
    }

    private void log(String s) {
        if (debug) {
            LOG.info("{} {} {}", indent, lineZ0.getName(), s);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LineZ0FlowCalculator.class);
}
