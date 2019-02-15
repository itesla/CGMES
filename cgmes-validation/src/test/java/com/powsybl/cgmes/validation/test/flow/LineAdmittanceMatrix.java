package com.powsybl.cgmes.validation.test.flow;

import java.util.Map;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;

public class LineAdmittanceMatrix extends AdmittanceMatrix {

    public LineAdmittanceMatrix(CgmesModel cgmes, Map<String, Integer> equipmentsReport) {
        super(cgmes, equipmentsReport);
    }

    public void calculate(PropertyBag line, String config) {
        double r = line.asDouble("r");
        double x = line.asDouble("x");
        double bch = line.asDouble("bch");

        String configurationBsh = "split";
        if (config.contains("Line_end1")) {
            configurationBsh = "end1";
        }
        if (config.contains("Line_end2")) {
            configurationBsh = "end2";
        }
        if (config.contains("Line_split")) {
            configurationBsh = "split";
        }

        double bsh1 = 0.0;
        double bsh2 = 0.0;
        if (configurationBsh.equals("end1")) {
            bsh1 = bch;
        } else if (configurationBsh.equals("end2")) {
            bsh2 = bch;
        } else if (configurationBsh.equals("split")) {
            bsh1 = bch * 0.5;
            bsh2 = bch * 0.5;
        }

        StringBuilder code = new StringBuilder();
        if (bsh1 == 0.0) {
            code.append("N");
        } else {
            code.append("Y");
        }
        if (bsh2 == 0.0) {
            code.append("N");
        } else {
            code.append("Y");
        }
        line.put("code", code.toString());
        Integer total = equipmentsReport.get(code.toString());
        if (total == null) {
            total = new Integer(0);
        }
        equipmentsReport.put(code.toString(), total + 1);

        Complex z = new Complex(r, x);
        Complex ysh1 = new Complex(0.0, bsh1);
        Complex ysh2 = new Complex(0.0, bsh2);
        yff = z.reciprocal().add(ysh1);
        yft = z.reciprocal().negate();
        ytf = z.reciprocal().negate();
        ytt = z.reciprocal().add(ysh2);
    }

}
