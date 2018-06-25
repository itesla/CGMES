package com.powsybl.cgmes.conversion.test.network.compare;

/*
 * #%L
 * CGMES conversion
 * %%
 * Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import com.powsybl.iidm.network.Network;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public final class ComparisonConfig {

    public ComparisonConfig() {
        checkNetworkId = true;
        differences = new DifferencesFail();
        networkMappingFactory = new NetworkMappingFactory() {
            @Override
            public NetworkMapping create(Network expected, Network actual) {
                return new NetworkMapping(expected, actual);
            }
        };
        checkVoltageLevelLimits = true;
        checkGeneratorReactiveCapabilityCurve = true;
        checkGeneratorRegulatingTerminal = true;
        compareNamesAllowSuffixes = false; // Must be identical
        tolerance = 1e-12;
    }

    public ComparisonConfig checkNetworkId(boolean checkNetworkId) {
        this.checkNetworkId = checkNetworkId;
        return this;
    }

    public ComparisonConfig onlyReportDifferences() {
        this.differences = new DifferencesReport();
        return this;
    }

    public ComparisonConfig checkVoltageLevelLimits(boolean checkVoltageLevelLimits) {
        this.checkVoltageLevelLimits = checkVoltageLevelLimits;
        return this;
    }

    public ComparisonConfig checkGeneratorReactiveCapabilityCurve(
            boolean checkGeneratorReactiveCapabilityCurve) {
        this.checkGeneratorReactiveCapabilityCurve = checkGeneratorReactiveCapabilityCurve;
        return this;
    }

    public ComparisonConfig checkGeneratorRegulatingTerminal(
            boolean checkGeneratorRegulatingTerminal) {
        this.checkGeneratorRegulatingTerminal = checkGeneratorRegulatingTerminal;
        return this;
    }

    public ComparisonConfig compareNamesAllowSuffixes(boolean allowSuffixes) {
        this.compareNamesAllowSuffixes = allowSuffixes;
        return this;
    }

    public ComparisonConfig tolerance(double tolerance) {
        this.tolerance = tolerance;
        return this;
    }

    public ComparisonConfig networkMappingFactory(NetworkMappingFactory networkMappingFactory) {
        this.networkMappingFactory = networkMappingFactory;
        return this;
    }

    boolean               checkNetworkId;
    Differences           differences;
    NetworkMappingFactory networkMappingFactory;
    boolean               checkVoltageLevelLimits;
    boolean               checkGeneratorReactiveCapabilityCurve;
    boolean               checkGeneratorRegulatingTerminal;
    boolean               compareNamesAllowSuffixes;
    double                tolerance;
}
