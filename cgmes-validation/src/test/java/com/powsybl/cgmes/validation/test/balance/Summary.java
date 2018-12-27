package com.powsybl.cgmes.validation.test.balance;

import java.util.Objects;

class Summary implements BalanceCollector<Summary> {

    @Override
    public boolean equals(Object other0) {
        if (!(other0 instanceof Summary)) {
            return false;
        }
        Summary other = (Summary) other0;
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
    public Summary create() {
        return new Summary();
    }

    @Override
    public void accumulate(Balance b) {
        num++;
        sum += b.error();
        worst = b.error() > worst.error() ? b : worst;
    }

    @Override
    public void combine(Summary b) {
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

    public String toString() {
        return String.format("{num: %d, worst: %s, sum: %f}", num, worst, sum);
    }

    int num = 0;
    double sum = 0;
    Balance worst = Balance.PERFECT;
}
