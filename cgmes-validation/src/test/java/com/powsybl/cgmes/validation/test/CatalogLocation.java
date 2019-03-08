package com.powsybl.cgmes.validation.test;

import java.nio.file.Path;

public interface CatalogLocation {

    Path dataRoot();

    Path boundary();
}
