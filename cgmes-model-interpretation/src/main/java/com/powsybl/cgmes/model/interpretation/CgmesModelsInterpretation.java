package com.powsybl.cgmes.model.interpretation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.tools.Catalog;
import com.powsybl.triplestore.api.TripleStoreFactory;

public class CgmesModelsInterpretation extends Catalog {

    public CgmesModelsInterpretation(String sdata) {
        super(sdata);
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
            }
        });
        return interpretations;
    }

    public Map<String, Exception> getExceptions() {
        return exceptions;
    }

    private CgmesModel load(Path p) {
        String impl = TripleStoreFactory.defaultImplementation();
        return CgmesModelFactory.create(dataSource(p), impl);
    }

    private static Map<String, Exception> exceptions;
    private static final Logger LOG = LoggerFactory.getLogger(CgmesModelsInterpretation.class);
}
