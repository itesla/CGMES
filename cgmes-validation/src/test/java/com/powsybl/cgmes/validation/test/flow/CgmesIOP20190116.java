package com.powsybl.cgmes.validation.test.flow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

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

    // @Test
    public void reviewAll1030() throws IOException {
        reviewAll("glob:**BD**1030*zip");
    }

    @Test
    public void reviewDebug1030() throws IOException {
        reviewAll("glob:**BD*MAVIR**1030*zip");
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
        Map<String, String> results = new HashMap<>();
        Map<String, Exception> exceptions = new HashMap<>();
        reviewAll(pattern, p -> {
            try {
                LOG.info("case {}", p.getParent().getFileName().toString());
                DataSource ds = dataSource(p);
                CgmesOnDataSource cds = new CgmesOnDataSource(ds);
                cds.cimNamespace();

                String impl = TripleStoreFactory.defaultImplementation();

                CgmesModel cgmes = CgmesModelFactory.create(ds, impl);
                CgmesFlowValidation flowValidation = new CgmesFlowValidation(cgmes);
                flowValidation.test(p.getParent().getFileName().toString());
                String report = flowValidation.getReport();
                results.put(p.getParent().getFileName().toString(), report);
            } catch (Exception x) {
                exceptions.put(p.getParent().getFileName().toString(), x);
            }
        });
        dumpReport(results, exceptions);
    }

    private void dumpReport(Map<String, String> results, Map<String, Exception> exceptions) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        LocalDateTime dateTime = LocalDateTime.now();
        String formattedDateTime = dateTime.format(formatter);
        results.values().forEach(r -> {
            try {
                Files.write(data.resolve(formattedDateTime + "_report.txt"), r.getBytes(),
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
                // TODO Auto-generated catch block
                LOG.error("Error message {}", e.getMessage());
            }
        });
    }

    private static final Logger LOG = LoggerFactory
            .getLogger(CgmesIOP20190116.class);
}
