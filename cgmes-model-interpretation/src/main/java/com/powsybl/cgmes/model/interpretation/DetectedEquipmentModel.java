/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
class DetectedEquipmentModel {

    DetectedEquipmentModel(DetectedBranchModel branchModel) {
        detectedBranchModels = new ArrayList<>();
        this.detectedBranchModels.add(branchModel);
        total = 0;
        calculated = 0;
        ok = 0;
    }

    public DetectedEquipmentModel(DetectedBranchModel branchModel1, DetectedBranchModel branchModel2,
            DetectedBranchModel branchModel3) {
        detectedBranchModels = new ArrayList<>();
        this.detectedBranchModels.add(branchModel1);
        this.detectedBranchModels.add(branchModel2);
        this.detectedBranchModels.add(branchModel3);
        total = 0;
        calculated = 0;
        ok = 0;
    }

    public DetectedEquipmentModel(List<DetectedBranchModel> detectedBranchModels) {
        this.detectedBranchModels = detectedBranchModels;
        total = 0;
        calculated = 0;
        ok = 0;
    }

    public String code() {
        if (detectedBranchModels.size() == 1) {
            return detectedBranchModels.get(0).code();
        }
        StringBuilder code = new StringBuilder();
        code.append(detectedBranchModels.get(0).code());
        code.append(".");
        code.append(detectedBranchModels.get(1).code());
        code.append(".");
        code.append(detectedBranchModels.get(2).code());
        return code.toString();
    }

    public String conversionCode() {
        // Line model always ok
        // Xfmr2 model
        String model = code();
        if (model.length() == 6) {
            String model1 = model.substring(0, 3);
            String model2 = model.substring(3, 6);

            model2 = model2.replace("Y", "Y(ko)");
            model2 = model2.replace("R", "R(C)");
            model2 = model2.replace("P", "P(C)");
            String evalModel = model1 + model2;
            if (evalModel.contains("(C)") || evalModel.contains("(ko)")) {
                return evalModel;
            }
            // Xfmr3 model
        } else if (model.length() == 20) {
            String model1 = model.substring(0, 6);
            String model11 = model1.substring(0, 3);
            String model12 = model1.substring(3, 6);
            String model2 = model.substring(7, 13);
            String model21 = model2.substring(0, 3);
            String model22 = model2.substring(3, 6);
            String model3 = model.substring(14, 20);
            String model31 = model3.substring(0, 3);
            String model32 = model3.substring(3, 6);

            model11 = model11.replace("Y", "Y(ko)");
            model21 = model21.replace("Y", "Y(C)");
            model22 = model22.replace("Y", "Y(ko)");
            model31 = model31.replace("Y", "Y(C)");
            model32 = model32.replace("Y", "Y(ko)");
            model11 = model11.replace("P", "P(ko)");
            model12 = model12.replace("P", "P(ko)");
            model21 = model21.replace("P", "P(ko)");
            model22 = model22.replace("P", "P(ko)");
            model31 = model31.replace("P", "P(ko)");
            model32 = model32.replace("P", "P(ko)");
            model21 = model21.replace("R", "R(C)");
            model31 = model31.replace("R", "R(C)");
            if (model1.contains("R")) {
                if (model2.contains("R") && model3.contains("R")) {
                    model11 = model11.replace("R", "R(ko)");
                    model12 = model12.replace("R", "R(ko)");
                } else {
                    model11 = model11.replace("R", "R(C)");
                    model12 = model12.replace("R", "R(C)");
                }
            }

            String evalModel = model11 + model12 + "." + model21 + model22 + "." + model31 + model32;
            if (evalModel.contains("(C)") || evalModel.contains("(ko)")) {
                return evalModel;
            }
        }

        return "ok";
    }

    final List<DetectedBranchModel> detectedBranchModels;
    int                             total;
    int                             calculated;
    int                             ok;
}
