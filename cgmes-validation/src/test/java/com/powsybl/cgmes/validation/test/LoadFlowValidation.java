package com.powsybl.cgmes.validation.test;

/*
 * #%L
 * CGMES conversion
 * %%
 * Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.validation.test.LoadFlowComputation.LoadFlowEngine;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.xml.XMLExporter;
import com.powsybl.iidm.xml.XMLImporter;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.mock.LoadFlowFactoryMock;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletion;
import com.powsybl.loadflow.resultscompletion.LoadFlowResultsCompletionParameters;
import com.powsybl.loadflow.validation.BusesValidation;
import com.powsybl.loadflow.validation.FlowsValidation;
import com.powsybl.loadflow.validation.GeneratorsValidation;
import com.powsybl.loadflow.validation.ValidationConfig;
import com.powsybl.loadflow.validation.ValidationType;
import com.powsybl.loadflow.validation.ValidationUtils;
import com.powsybl.loadflow.validation.io.ValidationWriter;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public final class LoadFlowValidation {

    private LoadFlowValidation(
            boolean validateInitialState,
            boolean changeSignForShuntReactivePowerFlowInitialState,
            double threshold,
            boolean specificCompatibility,
            boolean compareWithInitialState,
            BusValues tolerancesComparingWithInitialState,
            Set<String> ignoreQBusesComparingWithInitialState,
            int maxGeneratorsFailInitialState,
            int maxGeneratorsFailComputedState,
            int maxBusesFailInitialState,
            int maxBusesFailComputedState,
            Optional<LoadFlowEngine> loadFlowEngine,
            Path workingDirectory,
            boolean writeNetworksInputsResults,
            String label,
            Consumer<Network> debugNetwork) {
        this.validateInitialState = validateInitialState;
        this.changeSignForShuntReactivePowerFlowInitialState = changeSignForShuntReactivePowerFlowInitialState;
        this.threshold = threshold;
        this.compareWithInitialState = compareWithInitialState;
        this.ignoreQBusesComparingWithInitialState = ignoreQBusesComparingWithInitialState;
        this.maxGeneratorsFailInitialState = maxGeneratorsFailInitialState;
        this.maxGeneratorsFailComputedState = maxGeneratorsFailComputedState;
        this.maxBusesFailInitialState = maxBusesFailInitialState;
        this.maxBusesFailComputedState = maxBusesFailComputedState;
        this.workingDirectory = workingDirectory;
        this.writeNetworksInputsResults = writeNetworksInputsResults;
        this.loadFlowComputation = loadFlowEngine.isPresent()
                ? new LoadFlowComputation(loadFlowEngine.get())
                : new LoadFlowComputation();
        this.label = label;
        this.loadFlowParameters = new LoadFlowParameters();
        this.tolerancesComparingWithInitialState = tolerancesComparingWithInitialState;
        this.debugNetwork = debugNetwork;
        // Power flow solutions of some TSO files for DACF (EMS)
        // are valid if specificCompatibility is set to true
        loadFlowParameters.setSpecificCompatibility(specificCompatibility);
    }

    public boolean computationAvailable() {
        return loadFlowComputation.available();
    }

    public boolean validateInitialState() {
        return validateInitialState;
    }

    public boolean changeSignForShuntReactivePowerFlowInitialState() {
        return changeSignForShuntReactivePowerFlowInitialState;
    }

    public Consumer<Network> debugNetwork() {
        return debugNetwork;
    }

    public void validate(Network network) throws IOException {
        Files.createDirectories(this.workingDirectory);

        String initialStateId = network.getVariantManager().getWorkingVariantId();
        String initialLabel = "initial";
        String initialCompletedLabel = "initial-completed";
        String computedStateId = "computed";
        String computedLabel = computedStateId;

        write(network, initialLabel);
        for (Substation s : network.getSubstations()) {
            for (VoltageLevel vl : s.getVoltageLevels()) {
                String fname = String.format("%s-%s.dot", s.getName(), vl.getName()).replaceAll("\\/", "-");
                String f = workingDirectory.resolve(String.format("cgmes-initial-topo-%s", fname)).toString();
                try {
                    vl.exportTopology(f);
                } catch (IOException e) {
                    throw new PowsyblException("debug-topo", e);
                }
            }
        }
        if (validateInitialState) {
            computeMissingFlows(network);
            write(network, initialCompletedLabel);
            validateStateValues(network,
                    initialLabel,
                    maxGeneratorsFailInitialState,
                    maxBusesFailInitialState,
                    threshold);
        }

        if (loadFlowComputation.available()) {
            loadFlowComputation.compute(
                    network,
                    loadFlowParameters,
                    computedStateId,
                    workingDirectory);
            FileDataSource fds = write(network, computedLabel);
            validateStateValues(network,
                    computedLabel,
                    maxGeneratorsFailComputedState,
                    maxBusesFailComputedState,
                    threshold);

            // validate the state from the written file
            Network network1 = read(fds);
            write(network, "computed-reread");
            // FIXME debug topology of all voltage levels
            boolean recover = false;
            for (Substation s : network.getSubstations()) {
                for (VoltageLevel vl : s.getVoltageLevels()) {
                    VoltageLevel vl1 = network1.getVoltageLevel(vl.getId());
                    debugRecoverInternalConnections(recover, vl, vl1);
                }
            }
            for (Substation s : network1.getSubstations()) {
                for (VoltageLevel vl : s.getVoltageLevels()) {
                    String f = workingDirectory.resolve(
                            String.format("cgmes-reread-topo-%s-%s.dot",
                                    s.getName(),
                                    vl.getName()))
                            .toString();
                    try {
                        vl.exportTopology(f);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            validateStateValues(network1,
                    computedLabel + "-reread",
                    maxGeneratorsFailComputedState,
                    maxBusesFailComputedState,
                    threshold);
            writeSV(network, computedLabel);

            if (compareWithInitialState) {
                Map<String, BusValues> computedStateBusValues = collectBusValues(network);
                network.getVariantManager().setWorkingVariant(initialStateId);
                Map<String, BusValues> expectedBusValues = collectBusValues(network);
                compareBusValues(expectedBusValues, computedStateBusValues);
            } else {
                LOG.info("Bus values from LoadFlow results are not compared with initial values");
            }
            // After validation, leave load flow results as current state of network
            network.getVariantManager().setWorkingVariant(computedStateId);
        }
    }

    public static void debugRecoverInternalConnections(boolean recover, VoltageLevel vl, VoltageLevel vl1) {
        VoltageLevel.NodeBreakerView topo = vl.getNodeBreakerView();

        int[] nodes;
        try {
            nodes = topo.getNodes();
        } catch (PowsyblException x) {
            return;
        }
        final TIntSet encountered = new TIntHashSet();
        for (int n : nodes) {
            if (encountered.contains(n) || topo.getTerminal(n) == null) {
                continue;
            }
            encountered.add(n);
            topo.traverse(n, (n1, sw, n2) -> {
                encountered.add(n2);
                if (sw == null) {
                    System.out.printf("%s internal connection %s %s%n", recover ? "recover" : "debug", n1, n2);
                    // XXX if we recover after completing xml.read,
                    // the voltages have been set on a disconnected graph
                    if (recover) {
                        vl1.getNodeBreakerView()
                                .newInternalConnection()
                                .setNode1(n1)
                                .setNode2(n2)
                                .add();
                    }
                }
                return topo.getTerminal(n2) == null;
            });
        }
    }

    private void computeMissingFlows(Network network) {
        float epsilonX = 0;
        boolean applyXCorrection = false;
        LoadFlowResultsCompletionParameters p;
        p = new LoadFlowResultsCompletionParameters(epsilonX, applyXCorrection);
        LoadFlowResultsCompletion lf = new LoadFlowResultsCompletion(p, loadFlowParameters);
        try {
            lf.run(network, null);
        } catch (Exception e) {
            LOG.error("computeFlows, error {}", e.getMessage());
        }
    }

    static class BusValues {
        double v;
        double a;
        double p;
        double q;
    }

    private Map<String, BusValues> collectBusValues(Network network) {
        Map<String, BusValues> values = new HashMap<>();
        network.getBusBreakerView().getBuses()
                .forEach(b -> {
                    BusValues bv = new BusValues();
                    bv.v = b.getV();
                    bv.a = b.getAngle();
                    bv.p = b.getP();
                    bv.q = b.getQ();
                    values.put(b.getId(), bv);
                });
        return values;
    }

    private void compareBusValues(Map<String, BusValues> expected, Map<String, BusValues> actual)
            throws IOException {
        Path p = workingDirectory.resolve("temp-comparison.csv");
        expected.keySet().forEach(e -> LOG.info("expected " + e));
        actual.keySet().forEach(a -> LOG.info("actual " + a));
        try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            w.write(String.format(
                    "%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s\n",
                    "bus",
                    "v expected", "v actual", "v diff",
                    "a expected", "a actual", "a diff",
                    "p expected", "p actual", "p diff",
                    "q expected", "q actual", "q diff",
                    "comment"));
            boolean ok = true;
            int numBadP = 0;
            for (Iterator<String> k = expected.keySet().iterator(); k.hasNext();) {
                String b = k.next();
                BusValues e = expected.get(b);
                BusValues a = actual.get(b);

                w.write(b);
                boolean okv = compareBusValues(e.v, a.v, tolerancesComparingWithInitialState.v, w);
                boolean oka = compareBusValues(e.a, a.a, tolerancesComparingWithInitialState.a, w);
                boolean okp = compareBusValues(e.p, a.p, tolerancesComparingWithInitialState.p, w);
                boolean okq = compareBusValues(e.q, a.q, tolerancesComparingWithInitialState.q, w);
                w.write(";");
                if (!okv) {
                    w.write("Bad V ");
                }
                if (!oka) {
                    w.write("Bad A ");
                }
                if (!okp) {
                    w.write("Bad P ");
                    numBadP++;
                }
                if (!okq) {
                    w.write("Bad Q ");
                }
                if (!okq && ignoreQBusesComparingWithInitialState.contains(b)) {
                    w.write("Bad Q ignored ");
                    okq = true;
                }
                w.write("\n");
                ok = ok && okv && oka && okq;
            }
            if (numBadP > maxBusesFailComputedState) {
                LOG.error("too many bad P values : {} > {}", numBadP, maxBusesFailComputedState);
                ok = false;
            }
            String msg = "Comparison of bus values failed, check file " + p;
            if (!ok) {
                LOG.error(msg);
            }
            assertTrue(msg, ok);
        }
    }

    private boolean compareBusValues(double e, double a, double threshold, Writer w)
            throws IOException {
        w.write(String.format(";%12.6f;%12.6f;%12.8f", e, a, Math.abs(e - a)));
        if (!Double.isNaN(e) && !Double.isNaN(a)) {
            return Math.abs(e - a) < threshold;
        } else {
            return true;
        }
    }

    private void validateStateValues(Network network,
            String stateLabel,
            int maxGeneratorsFail,
            int maxBusesFail,
            double threshold)
            throws IOException {
        try (FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix())) {
            InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
            MapModuleConfig defaultConfig = platformConfig
                    .createModuleConfig("componentDefaultConfig");
            defaultConfig.setStringProperty("LoadFlowFactory",
                    LoadFlowFactoryMock.class.getCanonicalName());

            ValidationConfig config = ValidationConfig.load(platformConfig);
            config.setVerbose(true);
            config.setLoadFlowParameters(loadFlowParameters);
            LOG.info("specificCompatibility is {}", loadFlowParameters.isSpecificCompatibility());
            Path working = workingDirectory.resolve("temp-lf-validation-" + stateLabel);
            working = Files.createDirectories(working);

            // Flows should be equal within a given precision
            // Max mismatch allowed in a bus is also given with this threshold parameter
            config.setThreshold(threshold);
            // Some values could be missing
            // TODO powsybl results completion LoadFlow does not compute flows in dangling
            // lines
            config.setOkMissingValues(true);
            // config.setEpsilonX(0.02);
            // config.setApplyReactanceCorrection(true);

            boolean validBuses = validateBuses(network, config, working, maxBusesFail);
            boolean validFlows = validateFlows(network, config, working);
            boolean validGenerators = validateGenerators(network, config, working,
                    maxGeneratorsFail);
            assertTrue(validBuses);
            assertTrue(validFlows);
            assertTrue(validGenerators);
        }
    }

    private boolean validateBuses(
            Network network,
            ValidationConfig config,
            Path working,
            int maxBusesFail) throws IOException {
        boolean r;
        if (maxBusesFail == 0) {
            r = ValidationType.BUSES.check(network, config, working);
        } else {
            // Check that only given number of buses (slacks) fail the validation
            LOG.warn("Loadflow validation for buses with maxBusesFail {}",
                    maxBusesFail);
            Writer writer = Files.newBufferedWriter(
                    working.resolve("check-buses-bad.csv"),
                    StandardCharsets.UTF_8);
            ValidationWriter busesWriter = ValidationUtils.createValidationWriter(
                    network.getId(), config, writer, ValidationType.BUSES);
            long count = network.getBusView()
                    .getBusStream()
                    .sorted(Comparator.comparing(Bus::getId))
                    .filter(bus -> !BusesValidation.checkBuses(bus, config, busesWriter))
                    .count();
            writer.close();
            r = count <= maxBusesFail;
            if (!r) {
                LOG.warn("Only {} buses allowed to fail the check, found {}", maxBusesFail, count);
            }
        }
        return r;
    }

    private boolean validateGenerators(
            Network network,
            ValidationConfig config,
            Path working,
            int maxGeneratorsFail) throws IOException {
        boolean r;
        if (maxGeneratorsFail == 0) {
            r = ValidationType.GENERATORS.check(network, config, working);
        } else {
            // Check that only one generator (slack) fails the validation
            // Here we should only check that active power output is different of target
            // power
            // output ...
            // But there is no way of indicating to the validation method that we only want
            // a
            // particular type of check
            LOG.warn("Loadflow validation for generators with maxGeneratorsFail {}",
                    maxGeneratorsFail);
            Writer writer = Files.newBufferedWriter(
                    working.resolve("check-generators-bad.csv"),
                    StandardCharsets.UTF_8);
            int count = 0;
            for (Generator g : network.getGenerators()) {
                if (!GeneratorsValidation.checkGenerators(g, config, writer)) {
                    count++;
                }
            }
            r = count <= maxGeneratorsFail;
            if (!r) {
                LOG.warn("Only {} generators allowed to fail the check, {} found ",
                        maxGeneratorsFail, count);
            }
        }
        return r;
    }

    private boolean validateFlows(
            Network network,
            ValidationConfig config,
            Path working) throws IOException {
        Writer writer = Files.newBufferedWriter(
                working.resolve("check-flows-bad.csv"),
                StandardCharsets.UTF_8);
        ValidationWriter flowsWriter = ValidationUtils.createValidationWriter(
                network.getId(), config, writer, ValidationType.FLOWS);

        boolean linesValidated = network.getLineStream()
                .sorted(Comparator.comparing(Line::getId))
                .map(l -> {
                    boolean r;
                    // Do not perform the check if x is too low
                    if (l.getX() < 0.01) {
                        r = true;
                    } else {
                        r = FlowsValidation.checkFlows(l, configFor(l, config), flowsWriter);
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("flow-line r {}, x {}, b1 {}, b2 {}, ok {}, id {}", l.getR(),
                                l.getX(), l.getB1(), l.getB2(), r, l.getId());
                    }
                    return r;
                })
                .reduce(Boolean::logicalAnd).orElse(true);

        boolean transformersValidated = network.getTwoWindingsTransformerStream()
                .sorted(Comparator.comparing(TwoWindingsTransformer::getId))
                .map(t -> {
                    boolean r = FlowsValidation.checkFlows(t, config, flowsWriter);
                    if (r) {
                        Bus bus1 = t.getTerminal1().getBusView().getBus();
                        Bus bus2 = t.getTerminal2().getBusView().getBus();
                        double u1 = bus1 != null ? bus1.getV() : Double.NaN;
                        double u2 = bus2 != null ? bus2.getV() : Double.NaN;
                        double s = Math.abs(t.getTerminal1().getP()) +
                                Math.abs(t.getTerminal1().getQ()) +
                                Math.abs(t.getTerminal2().getP()) +
                                Math.abs(t.getTerminal2().getQ());

                        if (!Double.isNaN(u1) && !Double.isNaN(u2) && s > 4) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("flow-twt-ok {} u1 {} u2 {}", t.getId(), u1, u2);
                            }
                            // Check again
                            FlowsValidation.checkFlows(t, config, flowsWriter);
                        }
                    }
                    return r;
                })
                .reduce(Boolean::logicalAnd).orElse(true);

        return linesValidated && transformersValidated;
    }

    private ValidationConfig configFor(Line l, ValidationConfig config) {
        return config;
    }

    private FileDataSource write(Network network, String wlabel) {
        if (writeNetworksInputsResults && network != null) {

            String clabel = wlabel;
            if (label != null && !label.isEmpty()) {
                clabel += "-" + label;
            }
            String filename = "temp-" + clabel;

            XMLExporter xmlExporter = new XMLExporter();
            Properties params = new Properties();
            params.put(XMLExporter.THROW_EXCEPTION_IF_EXTENSION_NOT_FOUND, "false");
            FileDataSource fds = new FileDataSource(workingDirectory, filename);
            xmlExporter.export(network, params, fds);
            return fds;
        }
        return null;
    }

    private void writeSV(Network network, String wlabel) {
        if (writeNetworksInputsResults && network != null) {
            String clabel = wlabel;
            if (label != null && !label.isEmpty()) {
                clabel += "-" + label;
            }
            String filename = "temp-" + clabel + ".sv";
            PrintStream p;
            try {
                p = new PrintStream(workingDirectory.resolve(filename).toFile());
                network.getBusBreakerView().getBusStream().forEach(b -> writeSvVoltage(p, b));
                network.getLoadStream()
                        .forEach(l -> writeSvPowerFlow(p, l.getId() + "_TE", l.getTerminal()));
                network.getGeneratorStream()
                        .forEach(g -> writeSvPowerFlow(p, g.getId() + "_TE", g.getTerminal()));
                p.close();
            } catch (FileNotFoundException e) {
                LOG.error(e.getMessage());
            }
        }
    }

    private void writeSvVoltage(PrintStream p, Bus b) {
        p.printf("<cim:SvVoltage rdf:ID=\"%s_SV\">%n", b.getId());
        p.printf("<cim:SvVoltage.angle>%f</cim:SvVoltage.angle>%n", b.getAngle());
        p.printf("<cim:SvVoltage.TopologicalNode rdf:resource=\"#%s\"/>%n", b.getId());
        p.printf("<cim:SvVoltage.v>%f</cim:SvVoltage.v>%n", b.getV());
        p.printf("</cim:SvVoltage>%n");
        p.printf("DEBUG %s %f %f%n", b.getId(), b.getV(), b.getAngle());
    }

    private void writeSvPowerFlow(PrintStream p, String terminalId, Terminal t) {
        p.printf("<cim:SvPowerFlow rdf:ID=\"%s_SV\">%n", terminalId);
        p.printf("<cim:SvPowerFlow.p>%f</cim:SvPowerFlow.p>%n", t.getP());
        p.printf("<cim:SvPowerFlow.q>%f</cim:SvPowerFlow.q>%n", t.getQ());
        p.printf("<cim:SvPowerFlow.Terminal rdf:resource=\"#%s\"/>%n", terminalId);
        p.printf("</cim:SvPowerFlow>%n");
        p.printf("DEBUG %s %f %f%n", terminalId, t.getP(), t.getQ());
    }

    private Network read(FileDataSource fds) {
        XMLImporter xmlImporter = new XMLImporter();
        return xmlImporter.importData(fds, null);
    }

    public static class Builder {

        public Builder() {
            validateInitialState = true;
            compareWithInitialState = true;
            threshold = 0.1;
            specificCompatibility = false;
            tolerancesComparingWithInitialState = new BusValues();
            tolerancesComparingWithInitialState.v = 0.1; // kV
            tolerancesComparingWithInitialState.a = 0.1; // degrees
            tolerancesComparingWithInitialState.p = 0.1; // MW
            tolerancesComparingWithInitialState.q = 0.1; // MVAr
            ignoreQBusesComparingWithInitialState = new HashSet<>();
            maxGeneratorsFailInitialState = 0;
            maxGeneratorsFailComputedState = 1; // Slack
            maxBusesFailInitialState = 0;
            maxBusesFailComputedState = 1; // Slack if no generator modeled at slack bus
            workingDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
            writeNetworksInputsResults = false;
            loadFlowEngine = Optional.empty();
            label = "";
        }

        public Builder validateInitialState(boolean validateInitialState) {
            this.validateInitialState = validateInitialState;
            return this;
        }

        public Builder changeSignForShuntReactivePowerFlowInitialState(boolean b) {
            this.changeSignForShuntReactivePowerFlowInitialState = b;
            return this;
        }

        public Builder compareWithInitialState(boolean compareWithInitialState) {
            this.compareWithInitialState = compareWithInitialState;
            return this;
        }

        public Builder threshold(double threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder specificCompatibility(boolean specificCompatibility) {
            this.specificCompatibility = specificCompatibility;
            return this;
        }

        public Builder toleranceVComparingWithInitialState(float tolerance) {
            this.tolerancesComparingWithInitialState.v = tolerance;
            return this;
        }

        public Builder toleranceAComparingWithInitialState(float tolerance) {
            this.tolerancesComparingWithInitialState.a = tolerance;
            return this;
        }

        public Builder toleranceQComparingWithInitialState(float tolerance) {
            this.tolerancesComparingWithInitialState.q = tolerance;
            return this;
        }

        public Builder ignoreQBusComparingWithInitialState(String busId) {
            ignoreQBusesComparingWithInitialState.add(busId);
            return this;
        }

        public Builder maxGeneratorsFailInitialState(int n) {
            this.maxGeneratorsFailInitialState = n;
            return this;
        }

        public Builder maxGeneratorsFailComputedState(int n) {
            this.maxGeneratorsFailComputedState = n;
            return this;
        }

        public Builder maxBusesFailInitialState(int n) {
            this.maxBusesFailInitialState = n;
            return this;
        }

        public Builder maxBusesFailComputedState(int n) {
            this.maxBusesFailComputedState = n;
            return this;
        }

        public Builder workingDirectory(Path p) {
            this.workingDirectory = p;
            return this;
        }

        public Builder writeNetworksInputsResults(boolean write) {
            this.writeNetworksInputsResults = write;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder loadFlowEngine(LoadFlowEngine engine) {
            Objects.requireNonNull(engine);
            this.loadFlowEngine = Optional.of(engine);
            return this;
        }

        public Builder debugNetwork(Consumer<Network> networkConsumer) {
            this.debugNetwork = networkConsumer;
            return this;
        }

        public LoadFlowValidation build() {
            return new LoadFlowValidation(
                    validateInitialState,
                    changeSignForShuntReactivePowerFlowInitialState,
                    threshold,
                    specificCompatibility,
                    compareWithInitialState,
                    tolerancesComparingWithInitialState,
                    ignoreQBusesComparingWithInitialState,
                    maxGeneratorsFailInitialState,
                    maxGeneratorsFailComputedState,
                    maxBusesFailInitialState,
                    maxBusesFailComputedState,
                    loadFlowEngine,
                    workingDirectory,
                    writeNetworksInputsResults,
                    label,
                    debugNetwork);
        }

        private boolean validateInitialState;
        private boolean changeSignForShuntReactivePowerFlowInitialState;
        private double threshold;
        private boolean specificCompatibility;
        private boolean compareWithInitialState;
        private BusValues tolerancesComparingWithInitialState;
        private Set<String> ignoreQBusesComparingWithInitialState;
        private int maxGeneratorsFailInitialState;
        private int maxGeneratorsFailComputedState;
        private int maxBusesFailInitialState;
        private int maxBusesFailComputedState;
        private Path workingDirectory;
        private Optional<LoadFlowEngine> loadFlowEngine;
        private boolean writeNetworksInputsResults;
        private String label;
        private Consumer<Network> debugNetwork;
    }

    private final boolean validateInitialState;
    private final boolean changeSignForShuntReactivePowerFlowInitialState;
    private final double threshold;
    private final boolean compareWithInitialState;
    private final BusValues tolerancesComparingWithInitialState;
    private Set<String> ignoreQBusesComparingWithInitialState;
    private final int maxGeneratorsFailInitialState;
    private final int maxGeneratorsFailComputedState;
    private final int maxBusesFailInitialState;
    private final int maxBusesFailComputedState;
    private final Path workingDirectory;
    private final boolean writeNetworksInputsResults;
    private final String label;
    private final Consumer<Network> debugNetwork;

    private final LoadFlowParameters loadFlowParameters;
    private final LoadFlowComputation loadFlowComputation;

    private static final Logger LOG = LoggerFactory
            .getLogger(LoadFlowValidation.class);
}
