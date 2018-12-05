package com.powsybl.cgmes.validation.test.balance;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;

abstract class AbstractBalanceCollector {
    abstract AbstractBalanceCollector create();

    abstract void accept(Balance b);

    abstract void combine(AbstractBalanceCollector b);

    abstract void report(Output output);

    AbstractBalanceCollector collect(Network network, Output output) {
        Supplier<AbstractBalanceCollector> supplier = this::create;
        BiConsumer<AbstractBalanceCollector, Balance> accumulator = (bs, b) -> bs.accept(b);
        BiConsumer<AbstractBalanceCollector, AbstractBalanceCollector> combiner = (b1, b2) -> b1.combine(b2);
        AbstractBalanceCollector b = network.getBusView()
                .getBusStream()
                .sorted(Comparator.comparing(Bus::getId))
                .map(bus -> new Balance(bus, output))
                .filter(f -> !f.balance().isNaN())
                .collect(supplier, accumulator, combiner);
        return b;
    }
}
