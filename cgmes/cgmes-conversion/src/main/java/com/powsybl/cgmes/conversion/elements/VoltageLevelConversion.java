package com.powsybl.cgmes.conversion.elements;

/*
 * #%L
 * CGMES conversion
 * %%
 * Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import com.powsybl.cgmes.conversion.Conversion;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.triplestore.PropertyBag;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class VoltageLevelConversion extends AbstractIdentifiedObjectConversion {
    public VoltageLevelConversion(PropertyBag vl, Conversion.Context context) {
        super("VoltageLevel", vl, context);
        cgmesSubstationId = p.getId("Substation");
        iidmSubstationId = context.substationIdMapping().iidm(cgmesSubstationId);
        substation = context.network().getSubstation(iidmSubstationId);
    }

    @Override
    public boolean valid() {
        if (substation == null) {
            missing(String.format("Substation %s (IIDM id: %s)",
                    cgmesSubstationId,
                    iidmSubstationId));
            return false;
        }
        return true;
    }

    @Override
    public void convert() {
        String baseVoltage = p.getId("BaseVoltage");
        double nominalVoltage = p.asDouble("nominalVoltage");
        double lowVoltageLimit = p.asDouble("lowVoltageLimit");
        double highVoltageLimit = p.asDouble("highVoltageLimit");
        System.err.println("low, high   = " + lowVoltageLimit + ", " + highVoltageLimit + " : " + id);
        System.err.println("slow, shigh = " + p.get("lowVoltageLimit") + ", " + p.get("highVoltageLimit") + " : " + id);

        // Missing elements in the boundary file
        if (Double.isNaN(nominalVoltage)) {
            nominalVoltage = (lowVoltageLimit + highVoltageLimit) / 2.0;
            if (Double.isNaN(nominalVoltage)) {
                nominalVoltage = Math.PI;
            }
            missing(String.format("BaseVoltage %s", baseVoltage), nominalVoltage);
        }

        String voltageLevelId = context.namingStrategy().getId("VoltageLevel", id);
        VoltageLevel voltageLevel = context.network().getVoltageLevel(voltageLevelId);
        if (voltageLevel == null) {
            substation.newVoltageLevel()
                    .setId(voltageLevelId)
                    .setName(name)
                    .setEnsureIdUnicity(false)
                    .setNominalV(nominalVoltage)
                    .setTopologyKind(TopologyKind.BUS_BREAKER)
                    .setLowVoltageLimit(lowVoltageLimit)
                    .setHighVoltageLimit(highVoltageLimit)
                    .add();
        }
    }

    private final String     cgmesSubstationId;
    private final String     iidmSubstationId;
    private final Substation substation;
}
