package com.powsybl.cgmes.validation.test.balance;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Terminal;

interface Output {
    void bus(Bus bus);

    void pq(String equipmentType, Identifiable<?> equipment, Terminal t);

    void pq(String label, double p, double q, String name, String id);

    void pq(String label, double p, double q);

    void q(String equipmentType, Identifiable<?> equipment, Terminal t);

    void value(String label, double value);
}
