/**
 * Copyright (c) 2017-2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test;

import java.nio.file.Path;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.test.AbstractTestGridModel;
import com.powsybl.commons.datasource.CompressionFormat;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.commons.datasource.ReadOnlyDataSource;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class TestGridModelPath extends AbstractTestGridModel {

    public TestGridModelPath(
            Path path,
            String basename,
            CgmesModel expected) {
        this(path, basename, null, expected);
    }

    public TestGridModelPath(
            Path path,
            String basename,
            CompressionFormat compressionExtension,
            CgmesModel expected) {
        super(path.getName(path.getNameCount() - 1).toString(), expected);
        this.path = path;
        this.basename = basename;
        this.compressionExtension = compressionExtension;
    }

    public String basename() {
        return basename;
    }

    public CompressionFormat getCompressionExtension() {
        return compressionExtension;
    }

    @Override
    public ReadOnlyDataSource dataSource() {
        return DataSourceUtil.createDataSource(path, basename(), getCompressionExtension(), null);
    }

    private final Path path;
    private final String basename;
    private final CompressionFormat compressionExtension;
}
