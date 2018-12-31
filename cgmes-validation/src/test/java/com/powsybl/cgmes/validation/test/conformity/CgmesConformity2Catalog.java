/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test.conformity;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.cgmes.validation.test.TestGridModelPath;
import com.powsybl.commons.datasource.CompressionFormat;

class CgmesConformity2Catalog {

    public TestGridModel entsoeExplicitLoadFlow() {
        return new TestGridModelPath(
                ENTSOE_CAS2.resolve("ENTSOE_ExplicitLoadFlowCalculation"),
                "fixed-ENTSOE_CGMES_v2.4_ExplicitLoadFlowCalculation",
                CompressionFormat.ZIP,
                null);
    }

    public TestGridModel transformerLineTest() {
        return new TestGridModelPath(
                ENTSOE_CAS2.resolve("TransformerLineTest"),
                "TransformerLineTest",
                CompressionFormat.ZIP,
                null);
    }

    public TestGridModel microBaseCaseAssembled() {
        return new TestGridModelPath(
                ENTSOE_CAS2.resolve("MicroGrid").resolve("BaseCase_BC"),
                "CGMES_v2.4.15_MicroGridTestConfiguration_BC_Assembled_v2",
                CompressionFormat.ZIP,
                null);
    }

    public TestGridModel microBaseCaseBE() {
        return new TestGridModelPath(
                ENTSOE_CAS2.resolve("MicroGrid").resolve("BaseCase_BC"),
                "CGMES_v2.4.15_MicroGridTestConfiguration_BC_BE_v2",
                CompressionFormat.ZIP,
                null);
    }

    public TestGridModel real() {
        return new TestGridModelPath(
                ENTSOE_CAS2.resolve("RealGrid"),
                "CGMES_v2.4.15_RealGridTestConfiguration_v2",
                CompressionFormat.ZIP,
                null);
    }

    private static final Path ENTSOE_CAS2 = Paths.get("../data/conformity/cas-2.0");
}
