package com.powsybl.cgmes.validation.test;

import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.commons.datasource.ZipFileDataSource;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBags;

public class Catalog {

    public Catalog(CatalogLocation location) {
        Objects.requireNonNull(location);
        this.location = location;
    }

    public CgmesModel cgmes(Path path) {
        return CgmesModelFactory.create(dataSource(path), "rdf4j");
    }

    public Network convert(String rpath) {
        return convert(location.dataRoot().resolve(rpath), null, location.boundary());
    }

    public Network convert(String rpath, Properties params) {
        return convert(location.dataRoot().resolve(rpath), params);
    }

    public Network convert(Path path) {
        return convert(path, null, location.boundary());
    }

    public Network convert(Path path, Properties params) {
        return convert(path, params, location.boundary());
    }

    public Network convert(Path path, Properties params0, Path boundary) {
        Properties params = new Properties();
        if (params0 != null) {
            params.putAll(params0);
        }
        if (boundary != null) {
            params.put("iidm.import.cgmes.boundary-location", boundary.toString());
        }
        Network network = Importers.importData("CGMES",
            dataSource(path),
            params,
            LocalComputationManager.getDefault());
        return network;
    }

    public DataSource dataSource(Path path) {
        if (!Files.exists(path)) {
            fail();
        }
        String spath = path.toString();
        if (Files.isDirectory(path)) {
            String basename = spath.substring(spath.lastIndexOf('/') + 1);
            return new FileDataSource(path, basename);
        } else if (Files.isRegularFile(path) && spath.endsWith(".zip")) {
            return new ZipFileDataSource(path);
        } else {
            fail();
        }
        return null;
    }

    public String modelName(Path p) {
        // Identify the model using the portion of path relative to data root
        return p.subpath(location.dataRoot().getNameCount(), p.getNameCount()).toString();
    }

    public String tsoName(Path p) {
        String sp = p.toString();
        int i = sp.indexOf("_1D_") + 4;
        int j = sp.indexOf("_", i);
        if (j > i) {
            return sp.substring(i, j);
        } else {
            return sp.substring(i);
        }
    }

    public String country(Path p) {
        String sp = p.toString();
        int i = sp.lastIndexOf("_");
        return sp.substring(i + 1, i + 3);
    }

    protected String mas(CgmesModel cgmes) {
        PropertyBags models = ((CgmesModelTripleStore) cgmes).namedQuery("modelIds");
        return models.stream()
            .filter(m -> m.containsKey("modelingAuthoritySet"))
            .map(m -> m.get("modelingAuthoritySet"))
            .filter(mas -> !mas.contains("tscnet.eu"))
            .findFirst()
            .orElse("-");
    }

    protected final CatalogLocation location;
}
