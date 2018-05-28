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

import com.powsybl.cgmes.PowerFlow;
import com.powsybl.cgmes.conversion.Conversion;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.triplestore.PropertyBag;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class ShuntConversion extends AbstractConductingEquipmentConversion {

    public ShuntConversion(PropertyBag sh, Conversion.Context context) {
        super("ShuntCompensator", sh, context);
    }

    @Override
    public void convert() {
        int maximumSections = p.asInt("maximumSections", 0);
        int normalSections = p.asInt("normalSections", 0);
        float bPerSection = p.asFloat("bPerSection", 0.0f);
        int sections = fromContinuous(p.asFloat("SVsections", normalSections));

        // Adjustments
        sections = Math.abs(sections);
        maximumSections = Math.max(maximumSections, sections);
        if (bPerSection == 0) {
            float bPerSectionFixed = Float.MIN_VALUE;
            fixed("bPerSection", "Can not be zero", bPerSection, bPerSectionFixed);
            bPerSection = bPerSectionFixed;
        }

        ShuntCompensator shunt = voltageLevel().newShunt()
                .setId(iidmId())
                .setName(iidmName())
                .setEnsureIdUnicity(false)
                .setBus(terminalConnected() ? busId() : null)
                .setConnectableBus(busId())
                .setCurrentSectionCount(sections)
                .setbPerSection(bPerSection)
                .setMaximumSectionCount(maximumSections)
                .add();

        // At a shunt terminal, only Q can be set
        PowerFlow f = powerFlow();
        if (f.defined()) {
            float q = f.q();
            if (context.config().changeSignForShuntReactivePowerFlowInitialState()) {
                q = -q;
            }
            f = new PowerFlow(Float.NaN, q);
        }
        convertedTerminal(terminalId(), shunt.getTerminal(), 1, f);
    }
}
