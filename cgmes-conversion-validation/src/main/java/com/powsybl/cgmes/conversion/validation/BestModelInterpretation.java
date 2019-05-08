package com.powsybl.cgmes.conversion.validation;

import java.util.Map.Entry;

import com.powsybl.cgmes.interpretation.Interpretation;
import com.powsybl.cgmes.interpretation.InterpretationResults;
import com.powsybl.cgmes.interpretation.InterpretationResults.InterpretationAlternativeResults;
import com.powsybl.cgmes.interpretation.model.cgmes.CgmesModelForInterpretation;
import com.powsybl.cgmes.interpretation.model.interpreted.InterpretationAlternative;

public class BestModelInterpretation extends Interpretation {

    public BestModelInterpretation(CgmesModelForInterpretation model) {
        super(model);
    }

    public InterpretationAlternative getModelMapping(InterpretationResults result) {
        Entry<InterpretationAlternative, InterpretationAlternativeResults> e = result.interpretationAlternativeResults().entrySet()
                .stream().limit(1).findFirst().get();
        return e.getKey();
    }

    public InterpretationAlternativeResults getValidationData(InterpretationResults result) {
        Entry<InterpretationAlternative, InterpretationAlternativeResults> e = result.interpretationAlternativeResults().entrySet()
                .stream().limit(1).findFirst().get();
        return e.getValue();
    }
}
