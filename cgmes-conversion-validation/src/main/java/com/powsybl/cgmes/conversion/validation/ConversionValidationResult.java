package com.powsybl.cgmes.conversion.validation;

import java.util.HashMap;
import java.util.Map;

import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class ConversionValidationResult {

    public static class VerificationData {

        public VerificationData() {
            flowData = new HashMap<>();
        }

        public Map<String, FlowData> getFlowData() {
            return flowData;
        }

        public boolean failed() {
            return flowData.values().stream()
                    .filter(fd -> Math.abs(fd.pCgmes - fd.pIidm) + Math.abs(fd.qCgmes - fd.qIidm) > 0.001).limit(1)
                    .count() > 0;
        }

        Map<String, FlowData> flowData;
    }

    public boolean                                           failed;
    public Map<CgmesEquipmentModelMapping, VerificationData> verificationDataForAllModelMapping;
    public Exception                                         exception;
}
