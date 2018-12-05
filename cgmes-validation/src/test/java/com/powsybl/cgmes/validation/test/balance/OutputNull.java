package com.powsybl.cgmes.validation.test.balance;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Terminal;

class OutputNull implements Output {
    @Override
    public void bus(Bus bus) {
    }

    @Override
    public void pq(String equipmentType, Identifiable<?> equipment, Terminal t) {
    }

    @Override
    public void q(String equipmentType, Identifiable<?> equipment, Terminal t) {
    }

    @Override
    public void pq(String label, double p, double q, String name, String id) {
    }

    @Override
    public void pq(String label, double p, double q) {
    }

    @Override
    public void value(String label, double value) {
    }
}
