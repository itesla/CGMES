package com.powsybl.cgmes.validation.test.balance;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.cgmes.conversion.elements.extensions.RatioTapChangerExtension;
import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.VoltageLevel.BusBreakerView;
import com.powsybl.iidm.network.VoltageLevel.BusView;
import com.powsybl.iidm.network.util.BranchData;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletion;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletionParameters;

public class ReportBusBalances {

    public ReportBusBalances(TestGridModel gm) {
        this(gm.name(), gm);
    }

    public ReportBusBalances(String label, TestGridModel gm) {
        this.label = label;
        this.gm = gm;
    }

    public void report() throws IOException {
        CgmesImport i = new CgmesImport();
        Properties importParams = buildImportParams();
        LOG.info("Preparing data source ...");
        ReadOnlyDataSource ds = gm.dataSource();
        LOG.info("Importing data ...");
        Network network = i.importData(ds, importParams);
        if (resetFlowsAfterImport) {
            network.getShuntCompensators().forEach(sc -> {
                sc.getTerminal().setP(Double.NaN);
                sc.getTerminal().setQ(Double.NaN);
            });
            network.getLines().forEach(line -> {
                line.getTerminal1().setP(Double.NaN);
                line.getTerminal1().setQ(Double.NaN);
                line.getTerminal2().setP(Double.NaN);
                line.getTerminal2().setQ(Double.NaN);
            });
            network.getTwoWindingsTransformers().forEach(tx -> {
                tx.getTerminal1().setP(Double.NaN);
                tx.getTerminal1().setQ(Double.NaN);
                tx.getTerminal2().setP(Double.NaN);
                tx.getTerminal2().setQ(Double.NaN);
            });
            network.getThreeWindingsTransformers().forEach(tx -> {
                tx.getLeg1().getTerminal().setP(Double.NaN);
                tx.getLeg1().getTerminal().setQ(Double.NaN);
                tx.getLeg2().getTerminal().setP(Double.NaN);
                tx.getLeg2().getTerminal().setQ(Double.NaN);
                tx.getLeg3().getTerminal().setP(Double.NaN);
                tx.getLeg3().getTerminal().setQ(Double.NaN);
            });
        }
        report(network);
    }

    void report(Network network) throws IOException {
        if (analyzeShuntCompensators) {
            network.getShuntCompensators().forEach(sc -> {
                Bus bus = sc.getTerminal().getBusView().getBus();
                if (bus != null) {
                    double v = bus.getV();
                    if (Double.isNaN(v)) {
                        LOG.info("SC voltage NaN {}", bus.getId());
                    } else {
                        double q = sc.getTerminal().getQ();
                        double q1 = -1 * (v * v * sc.getbPerSection() * sc.getCurrentSectionCount());
                        double dq = Math.abs(q - q1);
                        LOG.info(String.format("SC %8.4f %8.4f %8.4f   %8.2f  %d  %s %s", dq, q, q1, v,
                                sc.getCurrentSectionCount(), sc.getName(), sc.getId()));
                    }
                }
            });
        }
        if (analyzePhaseAngleClocks) {
            LOG.info("Analyzing phase angle clocks ...");
            new PhaseAngleClocksAnalyzer().analyze(network);
        }
        LOG.info("Computing missing flows ...");
        computeMissingFlows(network);
        LOG.info("Recomputing flows for 3-windings transformers ...");
        recomputeFlowsFor3wtx(network);
        LOG.info("Calculating flows for z = 0 lines ...");
        boolean considerPhaseTapChangesUnknown = ignoreBusesWithPhaseTapChanges;
        LineZ0FlowCalculator.calc(network, considerPhaseTapChangesUnknown);

        Balance.configureCalc3wtxFlows(true);
        reportWorst(network);
        if (comparePowsyblExtra) {
            compare3wFlowsPowsyblExtra(network);
        }
        detailedDebugForSomeCases(network);
    }

    private void detailedDebugForSomeCases(Network network) throws IOException {
        if (debugVoltAvenue) {
            debug("Volt Avenue");
            debug(network.getThreeWindingsTransformer("_56b6724d-862a-437e-b98f-ea942ac24f35"));
            debug(network.getThreeWindingsTransformer("_729bee5e-b938-4992-972e-a6918d93fbda"));
        }
        if (debugStatcomGT2RaceBankOffshoreWindFarm) {
            // a two winding transformer with phase angle clock
            debug("Race Bank Offshore Wind Farm, StatCom GT-2");
            debug(network.getTwoWindingsTransformer("_fa059040-ca1e-4306-85ef-c53cb24ba183"));
        }
        if (debugElstreeSgt8) {
            ThreeWindingsTransformer tx = network.getThreeWindingsTransformer("_e67767ee-da47-4a30-81f2-0263b0294d43");
            debugTaps("Elstree", tx, tx.getLeg2().getRatioTapChanger(), t -> {
                calcFlows(t);
                return debug(t);
            });
        }
        if (debugDrax) {
            debug("Drax");
            debug(network.getTwoWindingsTransformer("_41690665-1707-40fe-897b-ecac7b73497d"));
            debug(network.getThreeWindingsTransformer("_eb3680a9-d90a-41fe-a229-58373e3f11c9"));
        }
        if (debugTwineHamGT2) {
            TwoWindingsTransformer tx = network.getTwoWindingsTransformer("_2f620d32-fecc-4ee2-b272-3ff60b84d62e");
            debugTaps("Twineham GT2", tx, tx.getRatioTapChanger(), t -> {
                calcFlows(t);
                return debug(t);
            });
        }
        if (debugBurboBank) {
            ThreeWindingsTransformer tx = network.getThreeWindingsTransformer("_c5e82e04-f0fc-4ab2-b040-8d2d71a41b1f");
            if (tx != null) {
                LOG.info("1 : {}", tx.getExtension(RatioTapChangerExtension.class));
                LOG.info("2 : {}", tx.getLeg2().getRatioTapChanger());
                LOG.info("3 : {}", tx.getLeg3().getRatioTapChanger());
            }
            debug(tx);
        }
        if (debugStJohnsWood) {
            ThreeWindingsTransformer tx = network.getThreeWindingsTransformer("_72b18c01-830a-4f5b-9438-ce3d8dcab1b5");
            debugTaps("St.Johns Wood", tx, tx.getLeg2().getRatioTapChanger(), t -> {
                calcFlows(t);
                return debug(t);
            });
        }
        if (debugSellindge) {
            debug("Sellindge SELL4 SCT 1");
            debug(network.getTwoWindingsTransformer("_bc136f79-617b-4326-93ec-67588d2b8cf3"));
        }
        if (debugViikinmaki) {
            debug("Viikinmäki");
            debug(network.getLoad("_4585abd7-d211-4649-8bc6-0108673dc967"));
        }
        if (debugPetajaskoski) {
            debug("Petajaskoski");
            debug(network.getThreeWindingsTransformer("_6bb9c946-4b0c-473b-8cd1-c8ee520d485b"));
        }
        if (debugKymiACT1) {
            debug("Kymi ACT1");
            debug(network.getThreeWindingsTransformer("_070c30f8-2a6d-455d-afcb-83efe6cab42b"));
        }
        if (debugKiisa) {
            debug("Kiisa");
            debug(network.getShuntCompensator("_d268b1c9-c8e9-4449-b0b0-df3fac607897"));
        }
        if (debugKaxapnOmega) {
            debug("IPTO K_AXAPNO");
            debug(network.getShuntCompensator("_f645e213-d828-4137-8f51-b95d32c87c34"));
        }
        if (debugEopdaia) {
            debug("IPTO ΕΟΡΔΑΙΑ");
            VoltageLevel vl = network.getVoltageLevel("_4a95e787-f70f-4132-9743-83bbb0fc8932");
            if (vl != null) {
                String fname = Files
                        .createTempDirectory("cgmes-validation-report-" + label)
                        .resolve(String.format("vl-%s.dot", vl.getName()))
                        .toString();
                debug("topology exported to " + fname);
                vl.exportTopology(fname);
            }
        }
    }

    private <T> void debugTaps(String label, T tx, RatioTapChanger rtc, ToDoubleFunction<T> consumer) {
        debug(label + " with given tap");
        double minError = consumer.applyAsDouble(tx);
        int bestTap = rtc.getTapPosition();
        double[] errors = new double[rtc.getStepCount()];
        for (int k = rtc.getLowTapPosition(); k <= rtc.getHighTapPosition(); k++) {
            rtc.setTapPosition(k);
            LOG.info("");
            LOG.info("{} with tap = {}", label, k);
            double error = consumer.applyAsDouble(tx);
            errors[k - rtc.getLowTapPosition()] = error;
            if (error < minError) {
                minError = error;
                bestTap = k;
            }
        }
        LOG.info("");
        LOG.info("{} best tap = {}, error = {}", label, bestTap, minError);
        LOG.info("");
        for (int k = 0; k < errors.length; k++) {
            LOG.info(String.format("    %2d  %10.4f", k + rtc.getLowTapPosition(), errors[k]));
        }
    }

    private void calcFlows(ThreeWindingsTransformer tx) {
        ThreeWindingsTransformerFlows f = new ThreeWindingsTransformerFlows(tx);
        Complex f1 = f.calc(tx.getLeg1().getTerminal().getBusView().getBus());
        tx.getLeg1().getTerminal().setP(f1.getReal());
        tx.getLeg1().getTerminal().setQ(f1.getImaginary());
        Complex f2 = f.calc(tx.getLeg2().getTerminal().getBusView().getBus());
        tx.getLeg2().getTerminal().setP(f2.getReal());
        tx.getLeg2().getTerminal().setQ(f2.getImaginary());
        Complex f3 = f.calc(tx.getLeg2().getTerminal().getBusView().getBus());
        tx.getLeg3().getTerminal().setP(f3.getReal());
        tx.getLeg3().getTerminal().setQ(f3.getImaginary());
    }

    private void calcFlows(TwoWindingsTransformer tx) {
        BranchData b = new BranchData(tx, 0, false, false);
        tx.getTerminal1().setP(b.getComputedP1());
        tx.getTerminal1().setQ(b.getComputedQ1());
        tx.getTerminal2().setP(b.getComputedP2());
        tx.getTerminal2().setQ(b.getComputedQ2());
    }

    private void debug(String title) {
        LOG.info("");
        LOG.info("");
        LOG.info("");
        LOG.info(title);
    }

    private double debug(Load l) {
        Output out = new OutputLogger(LOG);
        Bus bus = l.getTerminal().getBusView().getBus();
        if (bus == null) {
            return Double.NaN;
        }
        Balance b = new Balance(bus, out);
        return b.error();
    }

    private double debug(ShuntCompensator sc) {
        if (sc == null) {
            return Double.NaN;
        }
        Output out = new OutputLogger(LOG);
        Bus bus = sc.getTerminal().getBusView().getBus();
        if (bus == null) {
            return Double.NaN;
        }
        Balance b = new Balance(bus, out);
        return b.error();
    }

    private double debug(TwoWindingsTransformer tx) {
        if (tx == null) {
            return Double.NaN;
        }
        Output out = new OutputLogger(LOG);
        Bus bus1 = tx.getTerminals().get(0).getBusView().getBus();
        Bus bus2 = tx.getTerminals().get(0).getBusView().getBus();
        if (bus1 == null || bus2 == null) {
            return Double.NaN;
        }
        Balance b1 = new Balance(bus1, out);
        Balance b2 = new Balance(bus2, out);
        return b1.error() + b2.error();
    }

    private double debug(ThreeWindingsTransformer tx) {
        if (tx == null) {
            return Double.NaN;
        }
        Output out = new OutputLogger(LOG);
        Bus bus1 = tx.getTerminals().get(0).getBusView().getBus();
        Bus bus2 = tx.getTerminals().get(1).getBusView().getBus();
        Bus bus3 = tx.getTerminals().get(2).getBusView().getBus();
        if (bus1 == null || bus2 == null || bus3 == null) {
            return Double.NaN;
        }
        Balance b1 = new Balance(bus1, out);
        Balance b2 = new Balance(bus2, out);
        Balance b3 = new Balance(bus3, out);
        return b1.error() + b2.error() + b3.error();
    }

    private void reportWorst(Network network) {
        LOG.info("Summarizing balances ...");
        Output outnull = new OutputNull();
        Output out = new OutputLogger(LOG);
        WorstErrors bs = new WorstErrors(ignoreBusesWithPhaseTapChanges).collect(network, outnull);
        String title = ignoreBusesWithPhaseTapChanges
                ? "Ignored buses with Phase Tap Changes"
                : "All buses";
        LOG.info("Report for case {}. {}, {}", gm.name(), title, bs.balances().size());
        bs.report(out);

        LOG.info("Totals, {}", title);
        double error = bs.balances().stream().mapToDouble(Balance::error).sum();
        double thr = 1.0;
        long numErrorsBelowThr = bs.balances().stream().filter(b -> b.error() < thr).count();
        long num = bs.balances().size();
        LOG.info(String.format("    error    %10.0f", error));
        LOG.info(String.format("    num      %10d", num));
        LOG.info(String.format("    avg      %10.1f", error / num));
        LOG.info(String.format("    pct<thr  %10.1f %% < %.2f MVA", 100.0 * numErrorsBelowThr / num, thr));
        dumpCsv(bs.balances(), label + "-detail-balances.csv");
        dumpCsv(bs.ignored(), label + "-detail-ignored.csv");
    }

    private void dumpCsv(Set<Balance> balances, String filename0) {
        String filename = Paths.get(System.getProperty("java.io.tmpdir"), filename0).toString();
        try (PrintWriter p = new PrintWriter(filename)) {
            p.printf("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s%n",
                    "error",
                    "hasPtc", "alpha", "elementPtc",
                    "hasRho2w", "rho2w",
                    "hasRho3w2", "rho3w2",
                    "hasRho3w3", "rho3w3",
                    "bus");
            balances.stream().forEach(b -> {
                p.printf("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s%n",
                        b.error(),
                        b.hasPhaseTapChange(), b.alpha(),
                        b.elementPhaseTapChanger() != null ? b.elementPhaseTapChanger().getId() : "-",
                        b.hasRho2w(), b.rho2w(),
                        b.hasRho3w2(), b.rho3w2(),
                        b.hasRho3w3(), b.rho3w3(),
                        b.bus().getId());
            });
        } catch (FileNotFoundException e) {
            LOG.error("file not found");
        }
    }

    private void compare3wFlowsPowsyblExtra(Network network) {
        Output outnull = new OutputNull();

        Balance.configureCalc3wtxFlows(false);
        Summary powsybl = new Summary().collect(network, outnull);
        Balance.configureCalc3wtxFlows(true);
        Summary extra = new Summary().collect(network, outnull);
        LOG.info("results from powsybl {}", powsybl);
        LOG.info("results from extra   {}", extra);
        if (!powsybl.equals(extra)) {
            // If results from powsybl and extra are not equal, let's review in detail
            Balance.configureCalc3wtxFlows(false);
            Set<Balance> powsyblDetailed = ((WorstErrors) new WorstErrors()
                    .collect(network, outnull))
                            .balances();
            Balance.configureCalc3wtxFlows(true);
            Set<Balance> extraDetailed = ((WorstErrors) new WorstErrors()
                    .collect(network, outnull))
                            .balances();
            Set<Bus> powsyblBuses = powsyblDetailed.stream().map(b -> b.bus()).collect(Collectors.toSet());
            LOG.info("cgmes-extra buses not reviewed by powsybl:");
            extraDetailed.stream()
                    .filter(b -> !powsyblBuses.contains(b.bus()))
                    .forEach(b -> {
                        LOG.info("    {}", b.bus());
                        // Find all the configured buses inside this bus
                        BusView bview = b.bus().getVoltageLevel().getBusView();
                        BusBreakerView b0view = b.bus().getVoltageLevel().getBusBreakerView();
                        // For each configured bus ...
                        b0view.getBusStream().forEach(b0 -> {
                            // Check if its merged bus is the current bus
                            LOG.info("        {} merged on {}", b0, bview.getMergedBus(b0.getId()));
                            if (b.bus() == bview.getMergedBus(b0.getId())) {
                                LOG.info("        verified it is merged on current bus {}", b);
                            }
                        });
                    });
        }
        assertEquals(powsybl, extra);
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

    private void recomputeFlowsFor3wtx(Network network) {
        network.getThreeWindingsTransformerStream().forEach(tx -> {
            ThreeWindingsTransformerFlows txf = new ThreeWindingsTransformerFlows(tx);
            recomputeFlowsFor3wtx(txf, tx.getLeg1().getTerminal());
            recomputeFlowsFor3wtx(txf, tx.getLeg2().getTerminal());
            recomputeFlowsFor3wtx(txf, tx.getLeg3().getTerminal());
        });
    }

    private void recomputeFlowsFor3wtx(ThreeWindingsTransformerFlows txf, Terminal t) {
        Bus bus = t.getBusView().getBus();
        Complex flow = txf.calc(bus);
        t.setP(flow.getReal());
        t.setQ(flow.getImaginary());
    }

    public void setComparePowsyblExtra(boolean comparePowsyblExtra) {
        this.comparePowsyblExtra = comparePowsyblExtra;
    }

    public ReportBusBalances setStrict(boolean strict) {
        this.strict = strict;
        return this;
    }

    public ReportBusBalances setIgnoreBusesWithPhaseTapChanges(boolean ignoreBusesWithPhaseTapChanges) {
        this.ignoreBusesWithPhaseTapChanges = ignoreBusesWithPhaseTapChanges;
        return this;
    }

    public ReportBusBalances setConsiderPhaseAngleClock(boolean considerPhaseAngleClock) {
        this.considerPhaseAngleClock = considerPhaseAngleClock;
        return this;
    }

    public ReportBusBalances setChangeSignForPhaseShiftInPhaseTapChangerTable(boolean b) {
        this.changeSignForPhaseShiftInPhaseTapChangerTable = b;
        return this;
    }

    public ReportBusBalances setConsiderRatioTapChangersFor3wTxAtNetworkSide(boolean b) {
        this.considerRatioTapChangersFor3wTxAtNetworkSide = b;
        return this;
    }

    public ReportBusBalances setLowImpedanceLine(double r, double x) {
        lowImpedanceLineR = r;
        lowImpedanceLineX = x;
        return this;
    }

    public ReportBusBalances setAnalyzePhaseAngleClocks(boolean analyzePhaseAngleClocks) {
        this.analyzePhaseAngleClocks = analyzePhaseAngleClocks;
        return this;
    }

    private Properties buildImportParams() {
        Properties p = new Properties();
        p.put("strict", Boolean.toString(strict));
        p.put("storeCgmesModelAsNetworkExtension", "true");
        p.put("powsyblTripleStore", "rdf4j");
        p.put("considerPhaseAngleClock", Boolean.toString(considerPhaseAngleClock));
        p.put("considerRatioTapChangersFor3wTxAtNetworkSide",
                Boolean.toString(considerRatioTapChangersFor3wTxAtNetworkSide));
        if (!Double.isNaN(lowImpedanceLineR)) {
            p.put("lowImpedanceLineR", Double.toString(lowImpedanceLineR));
        }
        if (!Double.isNaN(lowImpedanceLineX)) {
            p.put("lowImpedanceLineX", Double.toString(lowImpedanceLineX));
        }
        p.put("changeSignForPhaseShiftInPhaseTapChangerTable",
                Boolean.toString(changeSignForPhaseShiftInPhaseTapChangerTable));
        return p;
    }

    private final String label;
    private final TestGridModel gm;

    private boolean comparePowsyblExtra = false;
    private boolean resetFlowsAfterImport = true;
    private boolean ignoreBusesWithPhaseTapChanges = false;
    private boolean considerPhaseAngleClock = false;
    private boolean changeSignForPhaseShiftInPhaseTapChangerTable = false;
    private boolean considerRatioTapChangersFor3wTxAtNetworkSide = false;
    private boolean analyzePhaseAngleClocks = true;
    private boolean analyzeShuntCompensators = true;

    private boolean strict = true;
    private double lowImpedanceLineR = Double.NaN;
    private double lowImpedanceLineX = Double.NaN;

    private boolean debugVoltAvenue = false;
    private boolean debugStatcomGT2RaceBankOffshoreWindFarm = false;
    private boolean debugElstreeSgt8 = false;
    private boolean debugDrax = false;
    private boolean debugTwineHamGT2 = false;
    private boolean debugBurboBank = false;
    private boolean debugStJohnsWood = false;
    private boolean debugSellindge = false;
    private boolean debugViikinmaki = false;
    private boolean debugPetajaskoski = false;
    private boolean debugKymiACT1 = false;
    private boolean debugKiisa = false;
    private boolean debugKaxapnOmega = false;
    private boolean debugEopdaia = false;

    private static final Logger LOG = LoggerFactory.getLogger(ReportBusBalances.class);
}
