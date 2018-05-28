package com.powsybl.cgmes.conversion.test.csi;

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

import org.junit.BeforeClass;
import org.junit.Test;

import com.powsybl.cgmes.conversion.test.ConversionTester;
import com.powsybl.cgmes.conversion.test.network.compare.Comparison;
import com.powsybl.cgmes.conversion.test.network.compare.ComparisonConfig;
import com.powsybl.cgmes.conversion.test.network.compare.NetworkMapping;
import com.powsybl.cgmes.conversion.test.network.compare.NetworkMappingFactory;
import com.powsybl.cgmes.test.csi.RteCasesCatalog;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.TripleStoreFactory;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class RteCasesConversionTest {
    @BeforeClass
    public static void setUp() {
        expecteds = new RteCasesNetworkCatalog();
        actuals = new RteCasesCatalog();
        tester = new ConversionTester(
                TripleStoreFactory.onlyDefaultImplementation(),
                new ComparisonConfig()
                        .checkNetworkId(false)
                        .checkVoltageLevelLimits(false)
                        .checkGeneratorReactiveCapabilityCurve(false)
                        .checkGeneratorRegulatingTerminal(false));
    }

    @Test
    public void fr201303151230Cim14()  {
        tester.testConversion(expecteds.fr201303151230(), actuals.fr201303151230());
    }

    @Test
    public void fr201707041430Cim14()  {
        tester.testConversion(expecteds.fr201707041430Cim14(), actuals.fr201707041430Cim14());
    }

    @Test
    public void fr201707041430Cgmes()  {
        Network expected = expecteds.fr201707041430Cim14();
        ComparisonConfig limitedNetworkComparison = new ComparisonConfig()
                .onlyReportDifferences()
                .checkNetworkId(false)
                .checkVoltageLevelLimits(false)
                .checkGeneratorReactiveCapabilityCurve(false)
                .checkGeneratorRegulatingTerminal(false)
                .compareNamesAllowSuffixes(true)
                .networkMappingFactory(new NetworkMappingFactory() {
                    @Override
                    public NetworkMapping create(Network expected, Network actual) {
                        return new NetworkMappingFr201707041430Cgmes(expected, actual);
                    }
                });
        tester.testConversion(expected, actuals.fr201707041430Cgmes(), limitedNetworkComparison);
    }

    static final class NetworkMappingFr201707041430Cgmes extends NetworkMapping {
        public NetworkMappingFr201707041430Cgmes(Network expected, Network actual) {
            super(expected, actual);
            expectedPrefixed = false;
            actualPrefixed = true;
            addMapping("_0TREGL32TREGU_ACLS", "_0TREGL32TREGU_ACLS");
            addMapping("_0TREGL31TREGU_ACLS", "_0TREGL31TREGU_ACLS");
            addMapping("_0FORTL31FORTA_ACLS", "_0FORTL31FORTA_ACLS");
            addMapping("_0FORTL32FORTA_ACLS", "_0FORTL32FORTA_ACLS");
            addMapping("_1GUILL11MOUCH_ACLS", "_1GUILL11MOUCH_ACLS");
            addMapping("_1ALBEP7_VL", "_1ALBEP7_VL");
            addMapping("_2ALBEP7_VL", "_2ALBEP7_VL");
            addMapping("_3ALBEP7_VL", "_3ALBEP7_VL");
            addMapping("_2P_GAP7_VL", "_2P_GAP7_VL");
            addMapping("_2P_GAP7_VL", "_2P_GAP7_VL");
        }

        @Override
        public Identifiable findExpected(Identifiable a) {
            Identifiable e = super.findExpected(a);
            if (e != null) {
                return e;
            }
            String c = Comparison.className(a);
            String aid = applyPrefixToActual(a.getId());
            if (c.equals("BusBreakerVoltageLevel")) {
                e = expected.getIdentifiable(aid);
                if (e != null) {
                    return e;
                }
                aid = a.getId();
            } else if (c.equals("ConfiguredBus")) {
                aid = aid.replaceFirst("_TN", "_VL_TN");
                e = expected.getIdentifiable(aid);
                if (e != null) {
                    return e;
                }
                // If not found, try without applying prefixes
                aid = a.getId().replaceFirst("_TN", "_VL_TN");
            } else if (c.equals("Load")) {
                aid = aid.replaceFirst("_CL", "_EC");
                aid = aid.replaceFirst("_NCL", "_EC");
            } else if (c.equals("Switch")) {
                aid = aid.replaceFirst("_BRSW_", "_SW_");
            } else if (c.equals("Generator")) {
                String ngu = aid.replaceFirst("_SM", "_NGU_SM");
                e = expected.getIdentifiable(ngu);
                if (e != null) {
                    return e;
                }
                String tgu = aid.replaceFirst("_SM", "_TGU_SM");
                e = expected.getIdentifiable(tgu);
                if (e != null) {
                    return e;
                }
                String hgu = aid.replaceFirst("_SM", "_HGU_SM");
                e = expected.getIdentifiable(hgu);
                if (e != null) {
                    return e;
                }
                String wgu = aid.replaceFirst("_SM", "_WGU_SM");
                e = expected.getIdentifiable(wgu);
                if (e != null) {
                    return e;
                }
                String gu = aid.replaceFirst("_SM", "_GU_SM");
                e = expected.getIdentifiable(gu);
                if (e != null) {
                    return e;
                }
                String gu0 = aid.replaceFirst("_0_SM", "_GU0_SM");
                e = expected.getIdentifiable(gu0);
                if (e != null) {
                    return e;
                }
                String gu1 = aid.replaceFirst("_1_SM", "_GU1_SM");
                aid = gu1;
            } else if (c.equals("Substation")) {
                String aid6 = aid.replaceFirst("_S", "_6_S");
                e = expected.getIdentifiable(aid6);
                if (e != null) {
                    return e;
                }
                String aid7 = aid.replaceFirst("_S", "_7_S");
                aid = aid7;
            }
            return expected.getIdentifiable(aid);
        }

        @Override
        public Identifiable findActual(Identifiable e) {
            Identifiable a = super.findActual(e);
            if (a != null) {
                return a;
            }
            String c = Comparison.className(e);
            String eid = applyPrefixToExpected(e.getId());
            if (c.equals("BusBreakerVoltageLevel")) {
                a = actual.getIdentifiable(eid);
                if (a != null) {
                    return a;
                }
                eid = e.getId();
            } else if (c.equals("ConfiguredBus")) {
                eid = eid.replaceFirst("_VL_TN", "_TN");
                a = actual.getIdentifiable(eid);
                if (a != null) {
                    return a;
                }
                // If not found, try without applying prefixes
                eid = e.getId().replaceFirst("_VL_TN", "_TN");
            } else if (c.equals("Load")) {
                // We do not know if _CL or _NCL (conform load or non-conform load)
                String ecl = eid.replaceFirst("_EC", "_CL");
                a = actual.getIdentifiable(ecl);
                if (a != null) {
                    return a;
                }
                String encl = eid.replaceFirst("_EC", "_NCL");
                eid = encl;
            } else if (c.equals("Switch")) {
                eid = eid.replaceFirst("_SW_", "_BRSW_");
            } else if (c.equals("Generator")) {
                eid = eid.replaceFirst("_NGU_SM", "_SM");
                eid = eid.replaceFirst("_TGU_SM", "_SM");
                eid = eid.replaceFirst("_HGU_SM", "_SM");
                eid = eid.replaceFirst("_WGU_SM", "_SM");
                eid = eid.replaceFirst("_GU_SM", "_SM");
                eid = eid.replaceFirst("_GU0_SM", "_0_SM");
                eid = eid.replaceFirst("_GU1_SM", "_1_SM");
            } else if (c.equals("Substation")) {
                eid = eid.replaceFirst("_6_S", "_S");
                eid = eid.replaceFirst("_7_S", "_S");
            }
            return actual.getIdentifiable(eid);
        }

    }

    private static RteCasesNetworkCatalog expecteds;
    private static RteCasesCatalog        actuals;
    private static ConversionTester       tester;
}
