package com.powsybl.cgmes.validation.test.balance;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;

interface BalanceCollector<T extends BalanceCollector<T>> {
    abstract T create();

    abstract void accumulate(Balance b);

    abstract void combine(T b);

    abstract void report(Output output);

    default T collect(Network network, Output output) {
        Supplier<T> supplier = this::create;
        BiConsumer<T, Balance> accumulator = (bs, b) -> bs.accumulate(b);
        BiConsumer<T, T> combiner = (b1, b2) -> b1.combine(b2);
        T b = network.getBusView()
                .getBusStream()
                .sorted(Comparator.comparing(Bus::getId))
                .map(bus -> new Balance(bus, output))
                .collect(supplier, accumulator, combiner);
        return b;
    }
}
