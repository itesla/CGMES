/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.validation.test.conformity;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.powsybl.cgmes.conformity.test.CgmesConformity1Catalog;
import com.powsybl.cgmes.validation.test.TestHelpers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CgmesConformity1Limits {

    @BeforeClass
    public static void setUp() throws IOException {
        catalog = new CgmesConformity1Catalog();
    }

    @Test
    public void microGridBaseCaseBELimits() throws IOException {
        Network network = Importers.importData("CGMES",
            catalog.microGridBaseCaseBE().dataSource(),
            null,
            LocalComputationManager.getDefault());
        TestHelpers.limitsSummary(network).report(System.err);
    }

    @Test
    public void miniNodeBreakerLimits() throws IOException {
        Network network = Importers.importData("CGMES",
            catalog.miniNodeBreaker().dataSource(),
            null,
            LocalComputationManager.getDefault());
        TestHelpers.limitsSummary(network).report(System.err);
    }

    private static CgmesConformity1Catalog catalog;
}
