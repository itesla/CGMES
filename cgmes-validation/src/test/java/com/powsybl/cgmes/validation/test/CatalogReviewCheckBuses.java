/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test;

import static com.powsybl.cgmes.validation.test.TestHelpers.THR_P;
import static com.powsybl.cgmes.validation.test.TestHelpers.THR_Q;
import static com.powsybl.cgmes.validation.test.TestHelpers.checkBuses;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.complex.Complex;

import com.powsybl.iidm.network.Network;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class CatalogReviewCheckBuses extends CatalogReview {

    public CatalogReviewCheckBuses(CatalogLocation location) {
        super(location);
    }

    public void reviewAll(String pattern) throws IOException {
        reviewAll(pattern, p -> {
            Network network = convert(p);
            ResultsCheckBuses r = new ResultsCheckBuses(network);
            r.checkBuses = checkBuses(network, THR_P, THR_Q, r.errors);
            // intermediate results should be processed/compressed/summarized
            // so we keep use of memory "relatively" low
            // here, we are not doing any post-processing of the checkBuses results
            // we are simply storing all the details
            results.put(p, r);
        });
    }

    private static class ResultsCheckBuses {
        ResultsCheckBuses(Network network) {
            numBuses = (int) network.getBusView().getBusStream().count();
            errors = new HashMap<>(numBuses);
        }

        private boolean checkBuses;
        private final Map<String, Complex> errors;
        private final int numBuses;
    }

    public void report() {
        System.err.println("");
        System.err.println("Results");
        System.err.println("Results    size, pctok, errp, errq, check, path");
        boolean verbose = true;
        if (verbose) {
            System.err.println("Results        errp, errq, bus");
        }
        results.keySet().stream()
            .sorted()
            .forEach(p -> report(p, results.get(p), verbose));
    }

    private void report(Path p, ResultsCheckBuses r, boolean verbose) {
        // Total error and statistics about buses with error
        Complex error = r.errors.values().stream().reduce(Complex::add).orElse(Complex.ZERO);
        double pctok = 100.0 * (1 - r.errors.values().size() / r.numBuses);
        System.err.printf("    %5d %5.1f%% (%10.2f, %10.2f) %b %s%n",
            r.numBuses,
            pctok,
            error.getReal(), error.getImaginary(),
            r.checkBuses,
            p);
        if (verbose) {
            // Details about all errors by bus
            r.errors.entrySet().stream()
                .sorted((e1, e2) -> -Double.compare(e1.getValue().abs(), e2.getValue().abs()))
                .forEach(e -> reportBusError(e.getKey(), e.getValue()));
        }
    }

    private void reportBusError(String id, Complex error) {
        System.err.printf("        %10.2f %10.2f %s%n", error.getReal(), error.getImaginary(), id);
    }

    private final Map<Path, ResultsCheckBuses> results = new HashMap<>();
}
