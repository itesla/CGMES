/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.tools;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.commons.datasource.CompressionFormat;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.triplestore.api.PropertyBags;

final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        output(String.format("Working directory = [%s]", System.getProperty("user.dir")));

        Path path = null;
        String basename = null;
        CompressionFormat compressionExtension = null;
        String tripleStoreImplementation = null;
        String action = null;
        String actionParam = null;
        for (int k = 0; k < args.length; k++) {
            switch (args[k]) {
                case "--path":
                    path = Paths.get(args[k + 1]);
                    break;
                case "--basename":
                    basename = args[k + 1];
                    break;
                case "--compression":
                    compressionExtension = CompressionFormat.valueOf(args[k + 1].toUpperCase());
                    break;
                case "--tripleStore":
                    tripleStoreImplementation = args[k + 1];
                    break;
                case "--action":
                    action = args[k + 1];
                    if (k + 2 < args.length) {
                        actionParam = args[k + 2];
                    }
                    break;
            }
        }
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(tripleStoreImplementation, "tripleStore");
        Objects.requireNonNull(action, "action");
        basename = basename == null ? "" : basename;
        output("Parameters");
        outputParameter("path", path.toAbsolutePath());
        outputParameter("basename", basename);
        outputParameter("compression", compressionExtension);
        outputParameter("tripleStore", tripleStoreImplementation);
        outputParameter("action", action);
        outputParameter("actionParam", actionParam);

        ReadOnlyDataSource ds = DataSourceUtil.createDataSource(path, basename, compressionExtension, null);
        CgmesModel cgmes = CgmesModelFactory.create(ds, tripleStoreImplementation);

        doSomething(cgmes, action, actionParam);
    }

    private static void doSomething(CgmesModel cgmes, String action, String actionParam) {
        if (action.equals("numObjectsByType")) {
            PropertyBags ot = cgmes.numObjectsByType();
            output(ot.tabulateLocals());
        } else if (action.equals("allObjectsOfType")) {
            String type = actionParam;
            Objects.requireNonNull(type);
            PropertyBags ot = cgmes.allObjectsOfType(type);
            if (ot.size() > 0) {
                output(ot.tabulate());

                String idProperty = "object";
                String keyProperty = "attribute";
                String valueProperty = "value";
                // Pivot all properties except ...
                List<String> notPivotable = Arrays.asList("graph", "type", idProperty, keyProperty, valueProperty);
                List<String> pivotPropertyNames = ot.pluckLocals("attribute");
                pivotPropertyNames.removeIf(p -> notPivotable.contains(p));
                PropertyBags ot1 = ot.pivotLocalNames(idProperty, keyProperty, pivotPropertyNames, valueProperty);
                output(ot1.tabulateLocals());
            }
        } else {
            output(String.format("Unknown action %s", action));
        }
    }

    private static void output(String s) {
        System.out.println(s);
    }

    private static void outputParameter(String param, Object value) {
        System.out.printf("    %-15s = %s%n", param, value);
    }
}
