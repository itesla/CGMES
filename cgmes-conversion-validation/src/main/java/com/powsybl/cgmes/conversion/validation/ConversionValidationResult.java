package com.powsybl.cgmes.conversion.validation;

import java.util.HashMap;
import java.util.Map;

import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class ConversionValidationResult {

    private static final double FLOW_THRESHOLD = 0.0001;

    public static class VerificationData {

        public VerificationData() {
            flowData = new HashMap<>();
        }

        public Map<String, FlowData> getFlowData() {
            return flowData;
        }

        public int nonCalculated() {
            return (int) flowData.values().stream()
                    .filter(fd -> !fd.calculated).count();
        }

        public int failedCount() {
            return (int) flowData.values().stream()
                    .filter(fd -> fd.flowError() > FLOW_THRESHOLD).count();
        }

        public boolean failed() {
            return flowData.values().stream()
                    .filter(fd -> fd.flowError() > FLOW_THRESHOLD).limit(1).count() > 0;
        }

        Map<String, FlowData> flowData;
    }

    public int                                               failedCount;
    public Map<CgmesEquipmentModelMapping, VerificationData> verificationDataForAllModelMapping;
    public Exception                                         exception;
}
