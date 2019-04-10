package com.powsybl.cgmes.conversion.validation;

import java.util.Map.Entry;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping;
import com.powsybl.cgmes.model.interpretation.InterpretationResult;
import com.powsybl.cgmes.model.interpretation.InterpretationResult.ValidationData;
import com.powsybl.cgmes.model.interpretation.ModelInterpretation;

public class BestModelInterpretation extends ModelInterpretation {

    public BestModelInterpretation(CgmesModel model) {
        super(model);
    }

    public CgmesEquipmentModelMapping getModelMapping() {
        InterpretationResult result = getInterpretation();
        Entry<CgmesEquipmentModelMapping, ValidationData> e = result.validationDataForAllModelMapping.entrySet()
                .stream().limit(1).findFirst().get();
        return e.getKey();
    }

    public ValidationData getValidationData() {
        InterpretationResult result = getInterpretation();
        Entry<CgmesEquipmentModelMapping, ValidationData> e = result.validationDataForAllModelMapping.entrySet()
                .stream().limit(1).findFirst().get();
        return e.getValue();
    }
}
