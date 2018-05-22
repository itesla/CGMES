package com.powsybl.cgmes.conversion.test;

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

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.commons.PowsyblException;
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

    public LoadFlowComputation(String loadFlowFactoryClassName) {
        this.loadFlowFactory = loadFlowFactory(loadFlowFactoryClassName);
    }

    public static String defaultFactoryClassName() {
        return MOCK_FACTORY_CLASS_NAME;
    }

    public boolean available() {
        return loadFlowFactory != null;
    }

    public void compute(
            Network network,
            LoadFlowParameters loadFlowParameters,
            String targetStateId,
            Path workingDirectory) {
        if (loadFlowFactory == null) {
            throw new PowsyblException("Can't compute LoadFlow. LoadFlowFactory not available");
        }
        network.getStateManager().cloneState(
                network.getStateManager().getWorkingStateId(),
                targetStateId);
        network.getStateManager().setWorkingState(targetStateId);
        try {
            Path working = Files.createDirectories(workingDirectory.resolve("temp-lf-computation"));
            ComputationManager computationManager = new LocalComputationManager(working);
            int priority = 1;
            LoadFlow loadFlow = loadFlowFactory.create(network, computationManager, priority);
            LoadFlowResult loadFlowResult = loadFlow.run(loadFlowParameters);
            LOG.info("Loadflow isOk = {}", loadFlowResult.isOk());
        } catch (Exception x) {
            LOG.error("Can't compute LoadFlow {}", x);
            throw new PowsyblException("Can't compute LoadFlow", x);
        }
    }

    private LoadFlowFactory loadFlowFactory(String loadFlowFactoryClassName) {
        LOG.info("Known LoadFlow engines   : {}, {}, {}",
                HELM_FLOW_FACTORY_CLASS_NAME,
                HADES_FACTORY_CLASS_NAME,
                MOCK_FACTORY_CLASS_NAME);
        LOG.info("Selected LoadFlow engine : {}", loadFlowFactoryClassName);
        try {
            Class<? extends LoadFlowFactory> loadFlowFactoryClass;
            loadFlowFactoryClass = Class.forName(loadFlowFactoryClassName)
                    .asSubclass(LoadFlowFactory.class);
            LoadFlowFactory loadFlowFactory = loadFlowFactoryClass.newInstance();
            return loadFlowFactory;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException x) {
            LOG.error("Can not setup LoadFlowFactory {} {}", loadFlowFactoryClassName, x);
            return null;
        }
    }

    private final LoadFlowFactory loadFlowFactory;

    private static final String   HADES_FACTORY_CLASS_NAME     = "com.rte_france.itesla.hades2.Hades2Factory";
    private static final String   HELM_FLOW_FACTORY_CLASS_NAME = "com.elequant.helmflow.powsybl.HelmFlowFactory";
    private static final String   MOCK_FACTORY_CLASS_NAME      = "com.powsybl.loadflow.mock.LoadFlowFactoryMock";

    private static final Logger   LOG                          = LoggerFactory
            .getLogger(LoadFlowComputation.class);
}
