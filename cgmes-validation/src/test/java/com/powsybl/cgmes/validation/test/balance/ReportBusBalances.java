package com.powsybl.cgmes.validation.test.balance;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletion;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletionParameters;

public class ReportBusBalances {

    public ReportBusBalances(TestGridModel gm, Properties importParams) {
        this.gm = gm;
        this.importParams = importParams;
    }

    public void report() throws IOException {
        CgmesImport i = new CgmesImport();
        try (FileSystem fs = Jimfs.newFileSystem()) {
            LOG.info("Preparing data source ...");
            ReadOnlyDataSource ds = gm.dataSourceBasedOn(fs);
            LOG.info("Importing data ...");
            Network network = i.importData(ds, importParams);
            report(network);
        }
    }

    void report(Network network) {
        compare3wFlowsPowsyblExtra(network);
        // reportWorst10(network);
    }

    void compare3wFlowsPowsyblExtra(Network network) {
        computeMissingFlows(network);
        Output outnull = new OutputNull();
        Balance.configureCalc3wtxFlows(false);
        AbstractBalanceCollector bs1 = new BalanceCollectorSummary().collect(network, outnull);
        Balance.configureCalc3wtxFlows(true);
        AbstractBalanceCollector bs2 = new BalanceCollectorSummary().collect(network, outnull);

        // Compare in detail for one bus
        Bus bus = ((BalanceCollectorSummary) bs1).worst.bus();
        LOG.info("");
        LOG.info("powsybl:");
        OutputLogger log = new OutputLogger(LOG);
        bs1.report(log);
        Balance.configureCalc3wtxFlows(false);
        new Balance(bus, log);
        LOG.info("");
        LOG.info("cgmes-extra:");
        bs2.report(log);
        Balance.configureCalc3wtxFlows(true);
        new Balance(bus, log);
        assertEquals(bs1, bs2);
    }

    void reportWorst10(Network network) {
        LOG.info("Computing missing flows ...");
        computeMissingFlows(network);
        LOG.info("Calculating flows for z = 0 lines ...");
        calculateFlowsForZ0Lines(network);
        LOG.info("Summarizing balances ...");
        Output outnull = new OutputNull();
        AbstractBalanceCollector b = new BalanceCollectorWorstErrors().collect(network, outnull);

        LOG.info("Report for case {}", gm.name());
        Output out = new OutputLogger(LOG);
        b.report(out);
        // Transformer with phase tap changer
        TwoWindingsTransformer tx = network.getTwoWindingsTransformer("_bee3437a-b0af-4924-bc15-c2a338a865b0");
        new Balance(tx.getTerminal1().getBusView().getBus(), out);
        new Balance(tx.getTerminal2().getBusView().getBus(), out);
    }

    static boolean isZ0(Line line) {
        return line.getR() == 0 && line.getX() == 0;
    }

    private static void calculateFlowsForZ0Lines(Network network) {
        network.getLines().forEach(l -> {
            if (isZ0(l)) {
                Bus b1 = l.getTerminal1().getBusView().getBus();
                Bus b2 = l.getTerminal2().getBusView().getBus();

                FlowZ0CanBeCalculatedTopologyVisitor around1 = new FlowZ0CanBeCalculatedTopologyVisitor(l, b1);
                if (around1.flowsAreKnown()) {
                    around1.setZ0Flows();
                } else {
                    FlowZ0CanBeCalculatedTopologyVisitor around2 = new FlowZ0CanBeCalculatedTopologyVisitor(l, b2);
                    if (around2.flowsAreKnown()) {
                        around2.setZ0Flows();
                    }
                }
            }
        });
    }

    private void computeMissingFlows(Network network) {
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

        float epsilonX = 0;
        boolean applyXCorrection = false;
        LoadFlowResultsCompletionParameters p;
        p = new LoadFlowResultsCompletionParameters(epsilonX, applyXCorrection);
        LoadFlowResultsCompletion lf = new LoadFlowResultsCompletion(p, loadFlowParameters);
        try {
            lf.run(network, null);
        } catch (Exception e) {
            throw new PowsyblException("completing", e);
        }
    }

    private final TestGridModel gm;
    private final Properties importParams;

    private static final Logger LOG = LoggerFactory.getLogger(ReportBusBalances.class);
}
