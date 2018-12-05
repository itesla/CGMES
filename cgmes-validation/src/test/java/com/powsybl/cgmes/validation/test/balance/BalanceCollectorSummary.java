package com.powsybl.cgmes.validation.test.balance;

import java.util.Objects;

class BalanceCollectorSummary extends AbstractBalanceCollector {

    @Override
    public boolean equals(Object other0) {
        if (!(other0 instanceof BalanceCollectorSummary)) {
            return false;
        }
        BalanceCollectorSummary other = (BalanceCollectorSummary) other0;
        return num == other.num
                && sum == other.sum
                && worst.error() == other.worst.error()
                && worst.bus() == other.worst.bus();
    }

    @Override
    public int hashCode() {
        return Objects.hash(num, sum, worst.error(), worst.bus());
    }

    @Override
    AbstractBalanceCollector create() {
        return new BalanceCollectorSummary();
    }

    @Override
    public void accept(Balance b) {
        num++;
        sum += b.error();
        worst = b.error() > worst.error() ? b : worst;
    }

    @Override
    public void combine(AbstractBalanceCollector b0) {
        BalanceCollectorSummary b = (BalanceCollectorSummary) b0;
        num = num + b.num;
        sum += b.sum;
        worst = b.worst.error() > worst.error() ? b.worst : worst;
    }

    @Override
    public void report(Output output) {
        if (num > 0) {
            output.pq("WORST error",
                    worst.balance().getReal(),
                    worst.balance().getImaginary(),
                    worst.bus().getName(),
                    worst.bus().getId());
            output.value("SUM   error", sum);
        }
    }

    int num = 0;
    double sum = 0;
    Balance worst = Balance.PERFECT;
}
