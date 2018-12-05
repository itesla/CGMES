package com.powsybl.cgmes.validation.test.balance;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

class BalanceCollectorWorstErrors extends AbstractBalanceCollector {

    @Override
    public boolean equals(Object other0) {
        if (!(other0 instanceof BalanceCollectorWorstErrors)) {
            return false;
        }
        BalanceCollectorWorstErrors other = (BalanceCollectorWorstErrors) other0;
        return balances.equals(other.balances);
    }

    @Override
    public int hashCode() {
        return balances.hashCode();
    }

    @Override
    AbstractBalanceCollector create() {
        return new BalanceCollectorWorstErrors();
    }

    @Override
    void accept(Balance b) {
        balances.add(b);
    }

    @Override
    void combine(AbstractBalanceCollector b0) {
        BalanceCollectorWorstErrors b = (BalanceCollectorWorstErrors) b0;
        balances.addAll(b.balances);
    }

    @Override
    public void report(Output output) {
        // For the 10 worst balances,
        // repeat the balance calculation on given output
        balances.stream().limit(10).forEach(b -> new Balance(b.bus(), output));
    }

    Set<Balance> balances = new TreeSet<>(Comparator.comparing(Balance::error).reversed());
}
