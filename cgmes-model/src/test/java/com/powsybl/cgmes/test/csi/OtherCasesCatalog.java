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
public class OtherCasesCatalog {
    public TestGridModel eu201711140000Cgmes() {
        return new TestGridModel(
                OTHER.resolve("20171114_0000_SN2_UX0.cgmes/fixed"),
                "20171114_0000_SN2_UX0.cgmes",
                null, false, false);
    }

    public TestGridModel ba201709050930Cgmes() {
        return new TestGridModel(
                OTHER.resolve("20170905_0930_FO2_BA0"),
                "20170905_0930_FO2_BA0",
                null, false, false);
    }

    public TestGridModel bg201709051130Cgmes() {
        return new TestGridModel(
                OTHER.resolve("20170905_1130_FO2_BG2"),
                "20170905_1130_FO2_BG2",
                null, false, false);
    }

    public TestGridModel hu201709051130Cgmes() {
        return new TestGridModel(
                OTHER.resolve("20170905_1130_FO2_HU1"),
                "20170905_1130_FO2_HU1",
                null, false, false);
    }

    public TestGridModel rs201709050930Cgmes() {
        return new TestGridModel(
                OTHER.resolve("20170905_0930_FO2_RS0"),
                "20170905_0930_FO2_RS0",
                null, false, false);
    }

    private static final Path OTHER = Paths.get("../data/csi/other");
}
