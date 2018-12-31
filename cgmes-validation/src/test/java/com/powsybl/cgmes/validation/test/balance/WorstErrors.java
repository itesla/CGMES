/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test.balance;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

class WorstErrors implements BalanceCollector<WorstErrors> {

    public WorstErrors() {
        this.ignoreBusesWithPhaseTapChanges = false;
    }

    public WorstErrors(boolean ignoreBusesWithPhaseTapChanges) {
        this.ignoreBusesWithPhaseTapChanges = ignoreBusesWithPhaseTapChanges;
    }

    @Override
    public boolean equals(Object other0) {
        if (!(other0 instanceof WorstErrors)) {
            return false;
        }
        WorstErrors other = (WorstErrors) other0;
        return balances.equals(other.balances);
    }

    @Override
    public int hashCode() {
        return balances.hashCode();
    }

    @Override
    public WorstErrors create() {
        return new WorstErrors(ignoreBusesWithPhaseTapChanges);
    }

    @Override
    public void accumulate(Balance b) {
        if (b.balance().isNaN()
                || ignoreBusesWithPhaseTapChanges && b.hasPhaseTapChange()) {
            ignored.add(b);
        } else {
            balances.add(b);
        }
    }

    @Override
    public void combine(WorstErrors b) {
        balances.addAll(b.balances);
        ignored.addAll(b.ignored);
    }

    @Override
    public void report(Output output) {
        // For the 10 worst balances,
        // repeat the balance calculation on the output given now
        balances.stream().limit(10).forEach(b -> new Balance(b.bus(), output));
    }

    public Set<Balance> balances() {
        return balances;
    }

    public Set<Balance> ignored() {
        return ignored;
    }

    private final Comparator<Balance> comparator = Comparator
            .comparing(Balance::error)
            .reversed()
            .thenComparing(b -> b.bus().getId());
    private final Set<Balance> balances = new TreeSet<>(comparator);
    private final Set<Balance> ignored = new TreeSet<>(comparator);
    private final boolean ignoreBusesWithPhaseTapChanges;
}
