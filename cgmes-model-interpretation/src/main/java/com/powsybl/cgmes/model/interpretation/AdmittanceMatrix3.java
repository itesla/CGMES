package com.powsybl.cgmes.model.interpretation;

class AdmittanceMatrix3 {

    public AdmittanceMatrix3() {
        super();
        end1 = new AdmittanceMatrix();
        end2 = new AdmittanceMatrix();
        end3 = new AdmittanceMatrix();
    }

    AdmittanceMatrix end1;
    AdmittanceMatrix end2;
    AdmittanceMatrix end3;
}
