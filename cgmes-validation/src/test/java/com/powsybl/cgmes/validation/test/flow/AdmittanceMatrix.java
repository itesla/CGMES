package com.powsybl.cgmes.validation.test.flow;

import java.util.Map;

import org.apache.commons.math3.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;

public class AdmittanceMatrix {
    public AdmittanceMatrix(CgmesModel cgmes, Map<String, Integer> equipmentsReport) {
        this.cgmes = cgmes;
        this.equipmentsReport = equipmentsReport;
    }

    public Complex getYff() {
        return yff;
    }

    public Complex getYft() {
        return yft;
    }

    public Complex getYtf() {
        return ytf;
    }

    public Complex getYtt() {
        return ytt;
    }

    protected CgmesModel           cgmes;
    protected Map<String, Integer> equipmentsReport;
    protected Complex              yff;
    protected Complex              yft;
    protected Complex              ytf;
    protected Complex              ytt;

    protected static final Logger  LOG = LoggerFactory
            .getLogger(AdmittanceMatrix.class);
}
