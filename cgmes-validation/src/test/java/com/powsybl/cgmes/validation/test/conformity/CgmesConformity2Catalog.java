package com.powsybl.cgmes.validation.test.conformity;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.cgmes.validation.test.TestGridModelPath;
import com.powsybl.commons.datasource.CompressionFormat;

public class CgmesConformity2Catalog {

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

    private static final Path ENTSOE_CAS2 = Paths.get("../data/conformity/cas-2.0");
}
