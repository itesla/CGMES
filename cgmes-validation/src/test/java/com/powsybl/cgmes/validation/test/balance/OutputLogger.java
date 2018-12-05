package com.powsybl.cgmes.validation.test.balance;

import org.slf4j.Logger;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Terminal;

class OutputLogger implements Output {

    public OutputLogger(Logger log) {
        this.log = log;
    }

    @Override
    public void bus(Bus bus) {
        if (log.isInfoEnabled()) {
            log.info("Bus {} {}", bus.getName(), bus.getId());
            log.info("Voltage {} kV, {} deg", bus.getV(), bus.getAngle());
        }
    }

    @Override
    public void pq(String equipmentType, Identifiable<?> equipment, Terminal t) {
        pq(equipmentType, t.getP(), t.getQ(), equipment.getName(), equipment.getId());
    }

    @Override
    public void pq(String label, double p, double q, String name, String id) {
        if (log.isInfoEnabled()) {
            log.info(String.format("    %-5s  %10.4f  %10.4f  %s  %s",
                    label,
                    p,
                    q,
                    name,
                    id));
        }
    }

    @Override
    public void pq(String label, double p, double q) {
        if (log.isInfoEnabled()) {
            log.info(String.format("    %-5s  %10.4f  %10.4f",
                    label,
                    p,
                    q));
        }
    }

    @Override
    public void q(String equipmentType, Identifiable<?> equipment, Terminal t) {
        if (log.isInfoEnabled()) {
            log.info(String.format("    %-5s  --          %10.4f  %s  %s",
                    equipmentType,
                    t.getQ(),
                    equipment.getName(),
                    equipment.getId()));
        }
    }

    @Override
    public void value(String label, double value) {
        if (log.isInfoEnabled()) {
            log.info(String.format("    %-5s  %10.4f", label, value));
        }
    }

    private final Logger log;
}
