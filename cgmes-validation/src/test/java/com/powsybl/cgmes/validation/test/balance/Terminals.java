package com.powsybl.cgmes.validation.test.balance;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;

public final class Terminals {

    private Terminals() {
    }

    static Terminal getOther(Branch<?> branch, Bus bus) {
        if (branch.getTerminal1().isConnected()
                && bus.getId().equals(branch.getTerminal1().getBusView().getBus().getId())) {
            return branch.getTerminal2();
        } else {
            return branch.getTerminal1();
        }
    }

    static Terminal get(Branch<?> branch, Bus bus) {
        if (branch.getTerminal1().isConnected()
                && bus.getId().equals(branch.getTerminal1().getBusView().getBus().getId())) {
            return branch.getTerminal1();
        } else {
            return branch.getTerminal2();
        }
    }

    static Terminal get(ThreeWindingsTransformer tlt, Bus bus) {
        if (tlt.getLeg1().getTerminal().isConnected()
                && bus.getId().equals(tlt.getLeg1().getTerminal().getBusView().getBus().getId())) {
            return tlt.getLeg1().getTerminal();
        } else if (tlt.getLeg2().getTerminal().isConnected()
                && bus.getId().equals(tlt.getLeg2().getTerminal().getBusView().getBus().getId())) {
            return tlt.getLeg2().getTerminal();
        } else {
            return tlt.getLeg3().getTerminal();
        }
    }
}
