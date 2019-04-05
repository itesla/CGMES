/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.LineShuntMappingAlternative;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class LineModel {

    public LineModel(PropertyBag line, CgmesEquipmentModelMapping config) {
        super();
        this.config = config;
        this.line = line;
        admittanceMatrix = new BranchAdmittanceMatrix();

        r = line.asDouble("r");
        x = line.asDouble("x");
    }

    public void interpret(double nominalV1, double nominalV2) {

        // yshunt
        BShuntData bShuntData = getLineBshunt(config);
        double bsh1 = bShuntData.bsh1;
        double bsh2 = bShuntData.bsh2;
        Complex ysh1 = new Complex(0.0, bsh1);
        Complex ysh2 = new Complex(0.0, bsh2);

        detectBranchModel(bsh1, bsh2);

        // add structural ratio after detected branch model
        // Lines with nominalV1 != nominalV2 (380, 400)
        double a0 = getLineRatio0(nominalV1, nominalV2, config);

        // admittance
        admittanceMatrix.calculateAdmittance(r, x, a0, 0.0, ysh1, 1.0, 0.0, ysh2);
    }

    public DetectedBranchModel getBranchModel() {
        return branchModel;
    }

    public BranchAdmittanceMatrix getAdmittanceMatrix() {
        return admittanceMatrix;
    }

    private void detectBranchModel(double bsh1, double bsh2) {
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
        double bch = line.asDouble("bch");
        LineShuntMappingAlternative lineBshunt = config.getLineBshunt();
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
    private final PropertyBag                line;

    private final double                     r;
    private final double                     x;

    private BranchAdmittanceMatrix           admittanceMatrix;
    private DetectedBranchModel              branchModel;
}
