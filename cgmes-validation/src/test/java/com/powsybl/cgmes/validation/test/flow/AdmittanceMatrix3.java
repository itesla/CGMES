package com.powsybl.cgmes.validation.test.flow;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;

public class AdmittanceMatrix3 extends AdmittanceMatrix {

    public AdmittanceMatrix3(CgmesModel cgmes) {
        super(cgmes);
    }

    public Complex getYff1() {
        return getYff();
    }

    public Complex getYft1() {
        return getYft();
    }

    public Complex getYtf1() {
        return getYtf();
    }

    public Complex getYtt1() {
        return getYtt();
    }

    public Complex getYff2() {
        return yff2;
    }

    public Complex getYft2() {
        return yft2;
    }

    public Complex getYtf2() {
        return ytf2;
    }

    public Complex getYtt2() {
        return ytt2;
    }

    public Complex getYff3() {
        return yff3;
    }

    public Complex getYft3() {
        return yft3;
    }

    public Complex getYtf3() {
        return ytf3;
    }

    public Complex getYtt3() {
        return ytt3;
    }

    protected Complex              yff2;
    protected Complex              yft2;
    protected Complex              ytf2;
    protected Complex              ytt2;
    protected Complex              yff3;
    protected Complex              yft3;
    protected Complex              ytf3;
    protected Complex              ytt3;
}
