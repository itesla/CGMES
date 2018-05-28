package com.powsybl.cgmes.test.csi;

/*
 * #%L
 * CGMES data model
 * %%
 * Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import com.powsybl.cgmes.test.TestGridModel;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class RteCasesCatalog {
    public TestGridModel fr201303151230() {
        return new TestGridModel(
                RTE.resolve("20130315_1230_FO_FR0"),
                "20130315_1230_FO_FR0",
                null, false, false);
    }

    public TestGridModel fr201707041430Cim14() {
        return new TestGridModel(
                RTE.resolve("recollement-auto-20170704-1430-enrichi.cimv14"),
                "recollement-auto-20170704-1430-enrichi",
                null, false, false);
    }

    public TestGridModel fr201707041430Cgmes() {
        return new TestGridModel(
                RTE.resolve("recollement-auto-20170704-1430-enrichi.cgmes"),
                "recollement-auto-20170704-1430-enrichi",
                null, false, false);
    }

    private static final Path RTE = Paths.get("../data/csi/RTE");
}
