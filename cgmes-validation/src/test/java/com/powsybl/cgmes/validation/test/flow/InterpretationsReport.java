package com.powsybl.cgmes.validation.test.flow;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterpretationsReport {

    public InterpretationsReport(Path output) {
        this.output = output;
    }

    public void report(Map<String, InterpretationResult> interpretations) throws IOException {
        Comparator<Map.Entry<String, InterpretationResult>> byError = (
            Entry<String, InterpretationResult> o1,
            Entry<String, InterpretationResult> o2) -> {
            return Double.compare(o1.getValue().error, o2.getValue().error);
        };
        Map<String, InterpretationResult> sortedResults = interpretations.entrySet().stream()
            .sorted(byError.reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                throw new AssertionError();
            }, LinkedHashMap::new));
        dump(sortedResults);
    }

    private void dump(Map<String, InterpretationResult> interpretations) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        LocalDateTime dateTime = LocalDateTime.now();
        String formattedDateTime = dateTime.format(formatter);
        try (BufferedWriter w = Files.newBufferedWriter(
            output.resolve("AutomatedCGMESValidationReport." + formattedDateTime), StandardCharsets.UTF_8)) {
            w.write(formattedDateTime);
            w.newLine();
            interpretations.entrySet().forEach(e -> {
                try {
                    w.write(e.getValue().report);
                    w.newLine();
                    w.flush();
                } catch (IOException x) {
                    LOG.warn("Error writing report for model {} {}", e.getKey(), x.getMessage());
                }
            });
        }
    }

    private final Path output;

    static final Logger LOG = LoggerFactory.getLogger(InterpretationsReport.class);
}
