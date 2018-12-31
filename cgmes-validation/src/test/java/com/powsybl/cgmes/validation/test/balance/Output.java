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

interface Output {
    void bus(Bus bus);

    void pq(String equipmentType, Identifiable<?> equipment, Terminal t);

    void pq(String label, double p, double q, String name, String id);

    void pq(String label, double p, double q);

    void q(String equipmentType, Identifiable<?> equipment, Terminal t);

    void value(String label, double value);
}
