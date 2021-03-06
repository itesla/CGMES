/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test.loadflow;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class LoadFlowComputation {

    public enum LoadFlowEngine {
        HADES(HADES_CLASS_NAME), HELM(HELM_CLASS_NAME), MOCK(MOCK_CLASS_NAME);

        LoadFlowEngine(String className) {
            this.className = className;
        }

        String className() {
            return className;
        }

        private final String className;
    }

    public LoadFlowComputation() {
        this.loadFlowFactory = defaultFactory();
    }

    public LoadFlowComputation(LoadFlowEngine engine) {
        if (engine == null) {
            this.loadFlowFactory = defaultFactory();
        } else {
            this.loadFlowFactory = loadFlowFactory(engine);
        }
    }

    public boolean available() {
        return loadFlowFactory != null;
    }

    public boolean isMock() {
        return loadFlowFactory.getClass().getName().contains("Mock");
    }

    public void compute(
            Network network,
            LoadFlowParameters loadFlowParameters,
            String targetStateId,
            Path workingDir) throws IOException {
        if (loadFlowFactory == null) {
            throw new PowsyblException("Can't compute LoadFlow. LoadFlowFactory not available");
        }
        network.getVariantManager().cloneVariant(
                network.getVariantManager().getWorkingVariantId(),
                targetStateId);
        network.getVariantManager().setWorkingVariant(targetStateId);
        ComputationManager computationManager = new LocalComputationManager(workingDir);
        int priority = 1;
        LoadFlow loadFlow = loadFlowFactory.create(network, computationManager, priority);
        LoadFlowResult loadFlowResult = loadFlow.run(targetStateId, loadFlowParameters).join();
        LOG.info("Loadflow isOk = {}", loadFlowResult.isOk());
    }

    private static LoadFlowFactory defaultFactory() {
        return validationFactory().orElse(platformDefaultFactory());
    }

    private static Optional<LoadFlowFactory> validationFactory() {
        PlatformConfig platformConfig = PlatformConfig.defaultConfig();
        if (platformConfig.moduleExists("loadflow-validation")) {
            ModuleConfig config = platformConfig.getModuleConfig("loadflow-validation");
            if (config.hasProperty("load-flow-factory")) {
                try {
                    return Optional
                            .of(config.getClassProperty("load-flow-factory", LoadFlowFactory.class)
                                    .newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new PowsyblException("Could not instantiate load flow factory.", e);
                }
            }
        }
        return Optional.empty();
    }

    private static LoadFlowFactory platformDefaultFactory() {
        return ComponentDefaultConfig.load().newFactoryImpl(LoadFlowFactory.class);
    }

    private static LoadFlowFactory loadFlowFactory(LoadFlowEngine engine) {
        Objects.requireNonNull(engine);
        LOG.info("Explicitly available LoadFlow engines  : {}",
                Arrays.toString(LoadFlowEngine.values()));
        LOG.info("Selected LoadFlow engine               : {}", engine);
        LOG.info("Selected LoadFlow engine factory class : {}", engine.className());
        try {
            Class<? extends LoadFlowFactory> loadFlowFactoryClass;
            loadFlowFactoryClass = Class.forName(engine.className())
                    .asSubclass(LoadFlowFactory.class);
            return loadFlowFactoryClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException x) {
            LOG.error("Can not setup LoadFlowFactory {} for engine {}, error {}",
                    engine.className(), engine, x);
            return null;
        }
    }

    private final LoadFlowFactory loadFlowFactory;

    private static final String HADES_CLASS_NAME = "com.rte_france.itesla.hades2.Hades2Factory";
    private static final String HELM_CLASS_NAME = "com.elequant.helmflow.powsybl.HelmFlowFactory";
    private static final String MOCK_CLASS_NAME = "com.powsybl.loadflow.mock.LoadFlowFactoryMock";

    private static final Logger LOG = LoggerFactory
            .getLogger(LoadFlowComputation.class);
}
