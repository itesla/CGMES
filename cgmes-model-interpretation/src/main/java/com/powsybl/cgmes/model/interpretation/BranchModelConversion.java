/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import com.powsybl.cgmes.model.interpretation.DetectedBranchModel.ChangerType;

/**
 * @author José Antonio Marqués <marquesja at aia.es>, Marcos de Miguel <demiguelm at aia.es>
 */
public class BranchModelConversion {

    enum ConversionType {
        // XXX Luma Review Not clear the meaning of "OK", "KO", "CONVERSION"
        // NO_CONVERSION_REQUIRED, CONVERSION_NOT_POSSIBLE, CONVERSION_REQUIRED ???
        OK, KO, CONVERSION
    }

    public void conversion(DetectedBranchModel detectedBranchModel) {
        // Line
        if (detectedBranchModel.ratio1 == null) {
            return;
        }

        // T2x
        // XXX LUMA Review use standard abbreviation XFMR: XFMR2
        shunt2 = detectedBranchModel.shunt2 ? ConversionType.KO : ConversionType.OK;
        ratio2 = detectedBranchModel.ratio2.equals(ChangerType.CHANGEABLE_AT_NON_NEUTRAL) ? ConversionType.CONVERSION
                : ConversionType.OK;
        phase2 = detectedBranchModel.phase2.equals(ChangerType.CHANGEABLE_AT_NON_NEUTRAL) ? ConversionType.CONVERSION
                : ConversionType.OK;
    }

    public void conversion(DetectedBranchModel detectedBranchModel1, DetectedBranchModel detectedBranchModel2,
            DetectedBranchModel detectedBranchModel3) {

        phase1 = detectedBranchModel1.phase1.equals(ChangerType.CHANGEABLE_AT_NON_NEUTRAL) ? ConversionType.KO
                : ConversionType.OK;
        phase2 = detectedBranchModel1.phase2.equals(ChangerType.CHANGEABLE_AT_NON_NEUTRAL) ? ConversionType.KO
                : ConversionType.OK;

        String model = detectedBranchModel1.code() + "." + detectedBranchModel2.code() + "."
                + detectedBranchModel3.code();
        // XXX Luma Checking an string for an exact length ???
        if (model.length() == 20) {
            String model1 = model.substring(0, 6);
            String model11 = model1.substring(0, 3);
            String model12 = model1.substring(3, 6);
            String model2 = model.substring(7, 13);
            String model21 = model2.substring(0, 3);
            String model22 = model2.substring(3, 6);
            String model3 = model.substring(14, 20);
            String model31 = model3.substring(0, 3);
            String model32 = model3.substring(3, 6);

            model11 = model11.replace("P", "P(ko)");
            model11 = model11.replace("Y", "Y(ko)");
            model12 = model12.replace("P", "P(ko)");

            model21 = model21.replace("R", "R(C)");
            model21 = model21.replace("P", "P(ko)");
            model21 = model21.replace("Y", "Y(C)");
            model22 = model22.replace("Y", "Y(ko)");
            model22 = model22.replace("P", "P(ko)");

            model31 = model31.replace("R", "R(C)");
            model31 = model31.replace("P", "P(ko)");
            model31 = model31.replace("Y", "Y(C)");
            model32 = model32.replace("Y", "Y(ko)");
            model32 = model32.replace("P", "P(ko)");

            if (model1.contains("R")) {
                if (model2.contains("R") && model3.contains("R")) {
                    model11 = model11.replace("R", "R(ko)");
                    model12 = model12.replace("R", "R(ko)");
                } else {
                    model11 = model11.replace("R", "R(C)");
                    model12 = model12.replace("R", "R(C)");
                }
            }

            // XXX Luma Review What should we do with the evalModel ???
            // If we remove evalModel, we also can get rid of previous local variables
        }
    }

    ConversionType ratio1 = ConversionType.OK;
    ConversionType phase1 = ConversionType.OK;
    ConversionType shunt1 = ConversionType.OK;
    ConversionType shunt2 = ConversionType.OK;
    ConversionType ratio2 = ConversionType.OK;
    ConversionType phase2 = ConversionType.OK;
}
