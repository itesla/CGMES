package com.powsybl.cgmes.validation.test.flow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.model.CgmesOnDataSource;
import com.powsybl.cgmes.validation.test.CatalogReview;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.triplestore.api.TripleStoreFactory;

public class CgmesIOP20190116 extends CatalogReview {

    public CgmesIOP20190116() {
        super("\\cgmes-csi\\IOP\\CGMES_IOP_20190116");
    }

    @Test
    public void reviewAll1030() throws IOException {
        reviewAll("glob:**BD**1030*zip");
    }

    // @Test
    public void reviewDebug1030() throws IOException {
        reviewAll("glob:**BD*RTE**1030*FR0*zip");
    }

    // @Test
    public void reviewNodeBreaker1130() throws IOException {
        reviewAll("glob:**BD*NodeBreaker**1130*zip");
    }

    // @Test
    public void reviewBusBranch1130() throws IOException {
        reviewAll("glob:**BD*BusBranch**1130*zip");
    }

    void reviewAll(String pattern) throws IOException {
        Map<String, ModelReport> results = new HashMap<>();
        Map<String, Exception> exceptions = new HashMap<>();
        reviewAll(pattern, p -> {
            try {
                String modelName = p.getParent().getFileName().toString() + "_" + p.getFileName().toString().replace(".zip", "");
                LOG.info("case {}", modelName);
                DataSource ds = dataSource(p);
                CgmesOnDataSource cds = new CgmesOnDataSource(ds);
                cds.cimNamespace();

                String impl = TripleStoreFactory.defaultImplementation();

                CgmesModel cgmes = CgmesModelFactory.create(ds, impl);
                CgmesFlowValidation flowValidation = new CgmesFlowValidation(cgmes);
                flowValidation.test(modelName);

                ModelReport modelReport = new ModelReport();
                modelReport.error = flowValidation.getBestError();
                modelReport.report = flowValidation.getReport();
                results.put(modelName, modelReport);
            } catch (Exception x) {
                String modelName = p.getParent().getFileName().toString() + "_" + p.getFileName().toString().replace(".zip", "");
                exceptions.put(modelName, x);
            }
        });

        Comparator<Map.Entry<String, ModelReport>> byError = (
                Entry<String, ModelReport> o1,
                Entry<String, ModelReport> o2) -> {
            return Double.compare(o1.getValue().error, o2.getValue().error);
        };

        Map<String, ModelReport> sortedResults = results.entrySet().stream()
                .sorted(byError.reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));
        dumpReport(sortedResults, exceptions);
    }

    private void dumpReport(Map<String, ModelReport> results, Map<String, Exception> exceptions) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        LocalDateTime dateTime = LocalDateTime.now();
        String formattedDateTime = dateTime.format(formatter);
        results.values().forEach(r -> {
            try {
                Files.write(data.resolve(formattedDateTime + "_report.txt"), r.report.getBytes(),
                        StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                Files.write(data.resolve(formattedDateTime + "_report.txt"),
                        System.getProperty("line.separator").getBytes(), StandardOpenOption.APPEND,
                        StandardOpenOption.CREATE);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                LOG.error("Error message {}", e.getMessage());
            }
        });
        exceptions.keySet().forEach(p -> {
            try {
                Files.write(data.resolve(formattedDateTime + "_error.txt"),
                        String.format("%s %s", p, exceptions.get(p).toString()).getBytes(),
                        StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                Files.write(data.resolve(formattedDateTime + "_error.txt"),
                        System.getProperty("line.separator").getBytes(), StandardOpenOption.APPEND,
                        StandardOpenOption.CREATE);
            } catch (IOException e) {
                LOG.error("Error message {}", e.getMessage());
            }
        });
    }

    class ModelReport {
        double error;
        String report;
    }

    private static final Logger LOG = LoggerFactory
            .getLogger(CgmesIOP20190116.class);
}
