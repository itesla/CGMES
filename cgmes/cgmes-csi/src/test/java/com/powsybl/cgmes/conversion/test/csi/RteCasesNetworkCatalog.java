package com.powsybl.cgmes.conversion.test.csi;

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

import java.nio.file.Files;

import com.powsybl.cgmes.csi.RteCasesCatalog;
import com.powsybl.cgmes.test.TestGridModel;
import com.powsybl.cim1.converter.CIM1Importer;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.iidm.network.Network;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class RteCasesNetworkCatalog {
    public Network fr201303151230() {
        return cimImport(catalog.fr201303151230());
    }

    public Network fr201707041430Cim14() {
        return cimImport(catalog.fr201707041430Cim14());
    }

    private Network cimImport(TestGridModel gm) {
        if (!Files.exists(gm.path())) {
            return null;
        }
        return new CIM1Importer().importData(
                new FileDataSource(gm.path(), gm.basename()),
                null);
    }

    private final RteCasesCatalog catalog = new RteCasesCatalog();
}
