package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

abstract class AbstractAdmittanceMatrix {

    Complex     yff;
    Complex     yft;
    Complex     ytf;
    Complex     ytt;
    DetectedBranchModel branchModel;
}
