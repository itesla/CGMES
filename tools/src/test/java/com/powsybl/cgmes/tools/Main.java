package com.powsybl.cgmes.tools;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.powsybl.cgmes.CgmesModel;
import com.powsybl.cgmes.CgmesModelFactory;
import com.powsybl.commons.datasource.CompressionFormat;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.triplestore.PropertyBags;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        output(String.format("Working directory = [%s]", System.getProperty("user.dir")));

        Path path = null;
        String basename = null;
        CompressionFormat compressionExtension = null;
        String tripleStoreImplementation = null;
        String action = null;
        for (int k = 0; k < args.length; k++) {
            if (args[k].equals("--path")) {
                path = Paths.get(args[k + 1]);
            } else if (args[k].equals("--basename")) {
                basename = args[k + 1];
            } else if (args[k].equals("--compressionExtension")) {
                compressionExtension = CompressionFormat.valueOf(args[k + 1]);
            } else if (args[k].equals("--tripleStoreImplementation")) {
                tripleStoreImplementation = args[k + 1];
            } else if (args[k].equals("--action")) {
                action = args[k + 1];
            }
        }
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(tripleStoreImplementation, "tripleStoreImplementation");
        Objects.requireNonNull(action, "action");
        basename = basename == null ? "" : basename;

        ReadOnlyDataSource ds = DataSourceUtil.createDataSource(path, basename, compressionExtension, null);
        CgmesModel cgmes = CgmesModelFactory.create(ds, tripleStoreImplementation);

        doSomething(cgmes, action);
    }

    private static void doSomething(CgmesModel cgmes, String action) {
        if (action.equals("numObjectsByType")) {
            PropertyBags ot = cgmes.numObjectsByType();
            output(ot.tabulateLocals());
        }
    }

    private static void output(String s) {
        System.out.println(s);
    }
}
