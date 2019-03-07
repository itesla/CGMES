package com.powsybl.cgmes.validation.test.flow;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.validation.test.CatalogReview;
import com.powsybl.triplestore.api.TripleStoreFactory;

public class CgmesModelsInterpretation extends CatalogReview {

    public CgmesModelsInterpretation(String sdata) {
        super(sdata);
    }

    public Map<String, InterpretationResult> reviewAll(String pattern) throws IOException {
        Map<String, InterpretationResult> interpretations = new HashMap<>();
        Map<String, Exception> exceptions = new HashMap<>();
        reviewAll(pattern, p -> {
            try {
                LOG.info("case {}", modelName(p));
                ModelInterpretation modelInterpretation = new ModelInterpretation(load(p), modelName(p));
                modelInterpretation.interpret();
                interpretations.put(modelName(p), modelInterpretation.getInterpretation());
            } catch (Exception x) {
                exceptions.put(modelName(p), x);
            }
        });
        dumpExceptions(exceptions);
        return interpretations;
    }

    private CgmesModel load(Path p) {
        String impl = TripleStoreFactory.defaultImplementation();
        return CgmesModelFactory.create(dataSource(p), impl);
    }

    private void dumpExceptions(Map<String, Exception> exceptions) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        LocalDateTime dateTime = LocalDateTime.now();
        String formattedDateTime = dateTime.format(formatter);
        if (!exceptions.isEmpty()) {
            try (BufferedWriter w = Files.newBufferedWriter(
                Paths.get("/TODO").resolve("AutomatedCGMESValidationError." + formattedDateTime), StandardCharsets.UTF_8)) {
                exceptions.keySet().forEach(p -> {
                    try {
                        w.write(String.format("%s %s", p, exceptions.get(p)));
                        w.newLine();
                    } catch (IOException e) {
                        LOG.error("Error writing exception found in model {} {} {}", p, exceptions.get(p),
                            e.getMessage());
                    }
                });
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(CgmesModelsInterpretation.class);
}
