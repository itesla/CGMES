package com.powsybl.cgmes.model.interpretation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.powsybl.triplestore.api.PropertyBag;

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
