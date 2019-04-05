/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.tools.Catalog;
import com.powsybl.triplestore.api.TripleStoreFactory;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
public class CgmesModelsInterpretation extends Catalog {

    public CgmesModelsInterpretation(String sdata) {
        this(sdata, null);
    }

    public CgmesModelsInterpretation(String sdata, String sboundary) {
        super(sdata);
        boundary = sboundary == null ? null : Paths.get(sboundary);
        exceptions = new HashMap<>();
    }

    public Map<String, InterpretationResult> reviewAll(String pattern) throws IOException {
        Map<String, InterpretationResult> interpretations = new HashMap<>();
        reviewAll(pattern, p -> {
            try {
                LOG.info("case {}", modelName(p));
                ModelInterpretation modelInterpretation = new ModelInterpretation(load(p));
                modelInterpretation.interpret();
                interpretations.put(modelName(p), modelInterpretation.getInterpretation());
            } catch (Exception x) {
                exceptions.put(modelName(p), x);
                LOG.warn(x.getMessage());
            }
        });
        return interpretations;
    }

    public Map<String, Exception> getExceptions() {
        return exceptions;
    }

    private CgmesModel load(Path p) {
        String impl = TripleStoreFactory.defaultImplementation();
        if (boundary == null) {
            return CgmesModelFactory.create(dataSource(p), impl);
        } else {
            return CgmesModelFactory.create(dataSource(p), dataSource(boundary), impl);
        }
    }

    private final Path             boundary;
    private Map<String, Exception> exceptions;
    private static final Logger    LOG = LoggerFactory.getLogger(CgmesModelsInterpretation.class);
}
