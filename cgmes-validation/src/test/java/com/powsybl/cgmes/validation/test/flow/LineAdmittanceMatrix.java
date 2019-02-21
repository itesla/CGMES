package com.powsybl.cgmes.validation.test.flow;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;

public class LineAdmittanceMatrix extends AdmittanceMatrix {

    public LineAdmittanceMatrix(CgmesModel cgmes) {
        super(cgmes);
    }

    public void calculate(PropertyBag line, String config) {

        readLineParameters(line);
        double[] bsh = getLineBshunt(config);
        double bsh1 = bsh[0];
        double bsh2 = bsh[1];

        Complex z = new Complex(r, x);
        Complex ysh1 = new Complex(0.0, bsh1);
        Complex ysh2 = new Complex(0.0, bsh2);
        yff = z.reciprocal().add(ysh1);
        yft = z.reciprocal().negate();
        ytf = z.reciprocal().negate();
        ytt = z.reciprocal().add(ysh2);

        setModelCode(lineModelCode(bsh1, bsh2));
    }

    private double[] getLineBshunt(String config) {
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
        return new double[] {bsh1, bsh2 };
    }

    private void readLineParameters(PropertyBag line) {
        r = line.asDouble("r");
        x = line.asDouble("x");
        bch = line.asDouble("bch");
    }

    private double r;
    private double x;
    private double bch;
}
