package com.powsybl.cgmes.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.datasource.ZipFileDataSource;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;

public class Model {

    public Model(String sdata) {
        this.data = Paths.get(sdata);
    }

    public Network convert(String rpath) {
        return convert(this.data.resolve(rpath), null);
    }

    public Network convert(String rpath, Properties params) {
        return convert(this.data.resolve(rpath), params);
    }

    public Network convert(String rpath, Properties params, String rboundaries) {
        return convert(this.data.resolve(rpath), params, this.data.resolve(rboundaries));
    }

    public Network convert(Path path) {
        return convert(path, null, null);
    }

    public Network convert(Path path, Properties params) {
        return convert(path, params, null);
    }

    public Network convert(Path path, Properties params0, Path boundaries) {
        Properties params = new Properties();
        if (params0 != null) {
            params.putAll(params0);
        }
        if (boundaries != null) {
            params.put("useTheseBoundaries", dataSource(boundaries));
            throw new PowsyblException("Explicit boundaries not available");
        }
        Network network = Importers.importData("CGMES",
                dataSource(path),
                params,
                LocalComputationManager.getDefault());
        return network;
    }

    public DataSource dataSource(Path path) {
        if (!Files.exists(path)) {
            return null;
        }

        String spath = path.toString();
        if (Files.isDirectory(path)) {
            String basename = spath.substring(spath.lastIndexOf('/') + 1);
            return new FileDataSource(path, basename);
        } else if (Files.isRegularFile(path) && spath.endsWith(".zip")) {
            return new ZipFileDataSource(path);
        }
        return null;
    }

    protected final Path data;
}
