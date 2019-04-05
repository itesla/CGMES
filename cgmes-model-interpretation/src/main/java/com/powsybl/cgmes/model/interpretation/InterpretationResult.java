/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class InterpretationResult {

    public static class ValidationData {

        ValidationData() {
            balance = 0.0;
            balanceData = new HashMap<>();
            detectedModelData = new HashMap<>();
        }

        public double getBalance() {
            return balance;
        }

        public Map<List<String>, PropertyBag> getBalanceData() {
            return balanceData;
        }

        public Map<String, DetectedEquipmentModel> getDetectedModelData() {
            return detectedModelData;
        }

        double                              balance;
        Map<List<String>, PropertyBag>      balanceData;
        Map<String, DetectedEquipmentModel> detectedModelData;
    }

    public double                                          error;
    public Map<CgmesEquipmentModelMapping, ValidationData> validationDataForAllModelMapping;
    public Exception                                       exception;
}
