package com.powsybl.cgmes.validation.test.flow;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

public class CgmesFlowValidationTest {

    @BeforeClass
    public static void setUp() throws IOException {
        cgmesFlowValidation = new CgmesFlowValidation("\\cgmes-csi\\IOP\\CGMES_IOP_20190116");
        cgmesFlowValidation.setReportPath("\\cgmes-csi\\IOP\\CGMES_IOP_20190116");
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

    static CgmesFlowValidation cgmesFlowValidation;
}
