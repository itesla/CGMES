package com.powsybl.cgmes.validation.test.csi.dacf.d20180221.bus_branch;

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

import java.nio.file.Path;
import java.nio.file.Paths;

import com.powsybl.cgmes.test.TestGridModel;

public class EsoCasesCatalog {
    public TestGridModel eso20180221T0930Z() {
        return new TestGridModel(ESO.resolve("20180221T0930Z"), null, null, false, false);
    }
    public TestGridModel eso20180221T0030Z() {
        return new TestGridModel(ESO.resolve("20180221T0030Z"), null, null, false, false);
    }
    public TestGridModel eso20180220T2330Z() {
        return new TestGridModel(ESO.resolve("20180220T2330Z"), null, null, false, false);
    }
    private static final Path ESO = Paths.get("../data/csi/DACF/20180221/unzipped/ESO");
}
