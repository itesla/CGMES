package com.powsybl.cgmes.validation.test.conformity;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.powsybl.cgmes.conformity.test.CgmesConformity1Catalog;
import com.powsybl.cgmes.conversion.CgmesModelExtension;
import com.powsybl.cgmes.validation.test.LimitsSummary;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;

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
        new LimitsSummary(network.getExtension(CgmesModelExtension.class).getCgmesModel()).report();
    }

    @Test
    public void miniNodeBreakerLimits() throws IOException {
        Network network = Importers.importData("CGMES",
                catalog.miniNodeBreaker().dataSource(),
                null,
                LocalComputationManager.getDefault());
        new LimitsSummary(network.getExtension(CgmesModelExtension.class).getCgmesModel()).report();
    }

    private static CgmesConformity1Catalog catalog;
}
