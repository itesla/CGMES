package com.powsybl.cgmes.validation.test.flow;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

public class CgmesFlowValidationTest {

    private static final String CATALOG_PATH = "/Users/zamarrenolm/works/RTE/data";

    @BeforeClass
    public static void setUp() throws IOException {
        cgmesFlowValidation = new CgmesModelsInterpretation(CATALOG_PATH);
    }

    @Test
    public void reviewAst() throws IOException {
        new InterpretationsReport(Paths.get(CATALOG_PATH)).report(cgmesFlowValidation.reviewAll("glob:**ast*/*zip"));
    }

    @Test
    public void reviewAll1030() throws IOException {
        cgmesFlowValidation.reviewAll("glob:**BD**1030*zip");
    }

    // @Test
    public void reviewDebug1030() throws IOException {
        cgmesFlowValidation.reviewAll("glob:**BD*AST**1030*zip");
    }

    // @Test
    public void reviewNodeBreaker1130() throws IOException {
        cgmesFlowValidation.reviewAll("glob:**BD*NodeBreaker**1130*zip");
    }

    // @Test
    public void reviewBusBranch1130() throws IOException {
        cgmesFlowValidation.reviewAll("glob:**BD*BusBranch**1130*zip");
    }

    static CgmesModelsInterpretation cgmesFlowValidation;
}
