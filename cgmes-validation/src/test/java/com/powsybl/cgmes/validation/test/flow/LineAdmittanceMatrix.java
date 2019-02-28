package com.powsybl.cgmes.validation.test.flow;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;

public class LineAdmittanceMatrix extends AdmittanceMatrix {

    public LineAdmittanceMatrix(CgmesModel cgmes) {
        super(cgmes);
    }

    public void calculate(PropertyBag line, double nominalV1, double nominalV2,
            String config) {

        readLineParameters(line);
        BShuntData bShuntData = getLineBshunt(config);
        double bsh1 = bShuntData.bsh1;
        double bsh2 = bShuntData.bsh2;

        Complex z = new Complex(r, x);
        Complex ysh1 = new Complex(0.0, bsh1);
        Complex ysh2 = new Complex(0.0, bsh2);

        // Lines with nominalV1 != nominalV2 (380, 400)
        double a0 = getLineRatio0(nominalV1, nominalV2, config);
        yff = z.reciprocal().add(ysh1);
        yft = z.reciprocal().negate().divide(a0);
        ytf = z.reciprocal().negate().divide(a0);
        ytt = z.reciprocal().add(ysh2).divide(a0 * a0);

        setModelCode(lineModelCode(bsh1, bsh2));
    }

    private double getLineRatio0(double nominalV1, double nominalV2, String config) {
        String configurationRario0 = "ratio0_off";
        if (config.contains("Line_ratio0_on")) {
            configurationRario0 = "ratio0_on";
        }
        if (config.contains("Line_ratio0_off")) {
            configurationRario0 = "ratio0_off";
        }

        double a0 = 1.0;
        if (configurationRario0.equals("ratio0_on")) {
            if (Math.abs(nominalV1 - nominalV2) > 0 && nominalV1 != 0.0 && !Double.isNaN(nominalV1)
                    && !Double.isNaN(nominalV2)) {
                a0 = nominalV2 / nominalV1;
            }
        }

        return a0;
    }

    private BShuntData getLineBshunt(String config) {
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

        BShuntData bShuntData = new BShuntData();
        if (configurationBsh.equals("end1")) {
            bShuntData.bsh1 = bch;
        } else if (configurationBsh.equals("end2")) {
            bShuntData.bsh2 = bch;
        } else if (configurationBsh.equals("split")) {
            bShuntData.bsh1 = bch * 0.5;
            bShuntData.bsh2 = bch * 0.5;
        }
        return bShuntData;
    }

    private void readLineParameters(PropertyBag line) {
        r = line.asDouble("r");
        x = line.asDouble("x");
        bch = line.asDouble("bch");
    }

    protected class BShuntData {
        double bsh1 = 0.0;
        double bsh2 = 0.0;
    }

    private double r;
    private double x;
    private double bch;
}
