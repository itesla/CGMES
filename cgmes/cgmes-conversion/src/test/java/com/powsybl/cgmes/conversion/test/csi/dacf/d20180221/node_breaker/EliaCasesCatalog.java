package com.powsybl.cgmes.conversion.test.csi.dacf.d20180221.node_breaker;

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

public class EliaCasesCatalog {
    public TestGridModel elia20180220T2330Z() {
        return new TestGridModel(ELIA.resolve("20180220T2330Z"), null, null, false, false);
    }
    private static final Path ELIA = Paths.get("../data/csi/DACF/20180221-nb/ELIA");
}
