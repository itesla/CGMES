/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test.balance;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;

import java.util.Comparator;

interface BalanceCollector<T extends BalanceCollector<T>> {
    T create();

    void accumulate(Balance b);

    void combine(T b);

    void report(Output output);

    default T collect(Network network, Output output) {
        return network.getBusView()
                .getBusStream()
                .sorted(Comparator.comparing(Bus::getId))
                .map(bus -> new Balance(bus, output))
                .collect(this::create, T::accumulate, T::combine);
    }
}
