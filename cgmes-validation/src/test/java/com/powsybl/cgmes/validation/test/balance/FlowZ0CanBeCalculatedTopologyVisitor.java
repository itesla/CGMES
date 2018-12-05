package com.powsybl.cgmes.validation.test.balance;

import com.powsybl.iidm.network.Branch.Side;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TopologyVisitor;
import com.powsybl.iidm.network.TwoWindingsTransformer;

class FlowZ0CanBeCalculatedTopologyVisitor implements TopologyVisitor {

    private final Line lineZ0;
    private final Bus bus;
    private boolean known;
    private double netP;
    private double netQ;

    public FlowZ0CanBeCalculatedTopologyVisitor(Line line, Bus bus) {
        this.lineZ0 = line;
        this.bus = bus;
        this.known = true;
        this.netP = 0;
        this.netQ = 0;
        bus.visitConnectedEquipments(this);
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
        } else {
            netP += t.getP();
            netQ += t.getQ();
        }
    }

    private void addQ(Terminal t) {
        if (Double.isNaN(t.getQ())) {
            known = false;
        } else {
            netQ += t.getQ();
        }
    }

    void setZ0Flows() {
        Terminal t = Terminals.get(lineZ0, bus);
        t.setP(-netP);
        t.setQ(-netQ);
        Terminal othert = Terminals.get(lineZ0, bus);
        othert.setP(netP);
        othert.setQ(netQ);
    }

    @Override
    public void visitBusbarSection(BusbarSection section) {
    }

    @Override
    public void visitLine(Line line, Side side) {
        if (line != this.lineZ0) {
            if (ReportBusBalances.isZ0(line)) {
                known = false;
            } else {
                addFlow(line.getTerminal(side));
            }
        }
    }

    @Override
    public void visitTwoWindingsTransformer(TwoWindingsTransformer transformer, Side side) {
        known = false;
    }

    @Override
    public void visitThreeWindingsTransformer(ThreeWindingsTransformer transformer,
            com.powsybl.iidm.network.ThreeWindingsTransformer.Side side) {
        known = false;
    }

    @Override
    public void visitGenerator(Generator generator) {
        known = false;
    }

    @Override
    public void visitLoad(Load load) {
        known = false;
    }

    @Override
    public void visitShuntCompensator(ShuntCompensator sc) {
        known = false;
    }

    @Override
    public void visitDanglingLine(DanglingLine danglingLine) {
        known = false;
    }

    @Override
    public void visitStaticVarCompensator(StaticVarCompensator staticVarCompensator) {
        known = false;
    }
}
