/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.EndDistribution;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author José Antonio Marqués <marquesja at aia.es>, Marcos de Miguel <demiguelm at aia.es>
 */
public class LineAdmittanceMatrix extends AdmittanceMatrix {

    public LineAdmittanceMatrix(PropertyBag line, CgmesEquipmentModelMapping config) {
        super();
        this.config = config;

        r = line.asDouble("r");
        x = line.asDouble("x");
        bch = line.asDouble("bch");
    }

    public void calculate(double nominalV1, double nominalV2) {

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

        branchModel = new DetectedBranchModel(bsh1, bsh2);
    }

    private double getLineRatio0(double nominalV1, double nominalV2, CgmesEquipmentModelMapping config) {
        double a0 = 1.0;
        if (config.isLineRatio0()) {
            if (Math.abs(nominalV1 - nominalV2) > 0 && nominalV1 != 0.0 && !Double.isNaN(nominalV1)
                    && !Double.isNaN(nominalV2)) {
                a0 = nominalV2 / nominalV1;
            }
        }

        return a0;
    }

    private BShuntData getLineBshunt(CgmesEquipmentModelMapping config) {
        EndDistribution lineBshunt = config.getLineBshunt();
        BShuntData bShuntData = new BShuntData();
        switch (lineBshunt) {
            case END1:
                bShuntData.bsh1 = bch;
                break;
            case END2:
                bShuntData.bsh2 = bch;
                break;
            case SPLIT:
                bShuntData.bsh1 = bch * 0.5;
                bShuntData.bsh2 = bch * 0.5;
                break;
        }
        return bShuntData;
    }

    static class BShuntData {
        double bsh1 = 0.0;
        double bsh2 = 0.0;
    }

    private final CgmesEquipmentModelMapping config;

    private final double               r;
    private final double               x;
    private final double               bch;
}
