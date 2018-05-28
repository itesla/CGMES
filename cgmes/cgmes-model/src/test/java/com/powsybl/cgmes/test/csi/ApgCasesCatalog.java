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

import java.nio.file.Paths;

import com.powsybl.cgmes.test.TestGridModel;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class ApgCasesCatalog {
    // Nodal CGMES from APG (a DACF produced in November 2017)
    public TestGridModel apgBD291120171D() {
        return new TestGridModel(
                Paths.get(
                        "../data/csi/APG/CE03_BD29112017_1D_APG_BusBranch/20171129T1230Z/fixed"),
                null,
                null, false, false);
    }
}
