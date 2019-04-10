package com.powsybl.cgmes.conversion.validation;

import com.powsybl.cgmes.conversion.Conversion;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr2RatioPhaseMappingAlternative;
import com.powsybl.cgmes.model.interpretation.CgmesEquipmentModelMapping.Xfmr3RatioPhaseMappingAlternative;
import com.powsybl.iidm.network.Network;

public class CgmesConversion {

    public CgmesConversion(CgmesModel model, CgmesEquipmentModelMapping modelMapping) {
        Conversion.Config config = configureConversion(modelMapping);
        conversion = new Conversion(model, config);
    }

    public Network convert() {
        return conversion.convert();
    }

    private Conversion.Config configureConversion(CgmesEquipmentModelMapping modelMapping) {
        Conversion.Config config = new Conversion.Config();
        config.allowUnsupportedTapChangers();
        if (modelMapping.getXfmr2Ratio0() == Xfmr2RatioPhaseMappingAlternative.END1) {
            config.setXfmr2Ratio0AtEnd1(true);
        } else {
            config.setXfmr2Ratio0AtEnd1(false);
        }
        if (modelMapping.isXfmr2Pac2Negate() || modelMapping.isXfmr2Ptc2Negate()) {
            config.setInvertXfmr2IidmAngle(true);
        } else {
            config.setInvertXfmr2IidmAngle(false);
        }
        if (modelMapping.getXfmr3Ratio0StarBusSide() == Xfmr3RatioPhaseMappingAlternative.NETWORK_SIDE) {
            config.setXfmr3Ratio0Outside(true);
        } else {
            config.setXfmr3Ratio0Outside(false);
        }
        if (modelMapping.getXfmr3RatioPhaseStarBusSide() == Xfmr3RatioPhaseMappingAlternative.NETWORK_SIDE) {
            config.setXfmr3RatioPhaseOutside(true);
        } else {
            config.setXfmr3RatioPhaseOutside(false);
        }
        return config;
    }

    private final Conversion conversion;
}
