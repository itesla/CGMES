/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
