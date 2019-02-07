package com.powsybl.cgmes.validation.test.flow;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.complex.Complex;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.cgmes.conformity.test.CgmesConformity1Catalog;
import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.cgmes.model.CgmesModel.CgmesTerminal;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.cgmes.model.CgmesNames;
import com.powsybl.cgmes.model.CgmesOnDataSource;
import com.powsybl.cgmes.model.test.TestGridModel;
import com.powsybl.cgmes.model.test.TestGridModelPath;
import com.powsybl.cgmes.model.triplestore.CgmesModelTripleStore;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.TripleStoreFactory;

public class CgmesFlowValidation {

    @BeforeClass
    public static void setUp() throws IOException {
        catalog = new CgmesConformity1Catalog();

        Path p = Paths.get("F:\\cgmes-csi\\DACF\\20180221\\unzipped\\APG\\20180220T2330Z");
        TestGridModelPath gm = new TestGridModelPath(p, "20180220T2330Z_", null);
        loadModel(gm);
    }

    @Test
    public void test() throws IOException {

        calcBalances();
    }

    private void calcBalances() {

        List<String> configs = new ArrayList<>(Arrays.asList("default"));
        configs.forEach(config -> {
            Map<List<String>, PropertyBag> sortedReport = calcBalance(config);

            long totalNodes = sortedReport.values().size();
            long tolTotal = sortedReport.values().stream().filter(pb -> {
                return Math.abs(pb.asDouble("balanceP")) > 1.0
                        || Math.abs(pb.asDouble("balanceQ")) > 1.0;
            }).count();

            double total = sortedReport.values().stream().map(pb -> {
                return Math.abs(pb.asDouble("balanceP"));
            }).mapToDouble(Double::doubleValue).sum();
            total += sortedReport.values().stream().map(pb -> {
                return Math.abs(pb.asDouble("balanceQ"));
            }).mapToDouble(Double::doubleValue).sum();
            LOG.info("config {} total error {} nodes {} error {} pct {}", config, total, totalNodes,
                    tolTotal, Long.valueOf(tolTotal).doubleValue()
                            / Long.valueOf(totalNodes).doubleValue() * 100.0);

            show = 5;
            sortedReport.keySet().forEach(k -> {
                if (show > 0) {
                    PropertyBag pb = sortedReport.get(k);
                    double balanceP = pb.asDouble("balanceP");
                    double balanceQ = pb.asDouble("balanceQ");
                    boolean partial = pb.asBoolean("partial", false);
                    if (partial) {
                        LOG.info("nodes {} {} {} partial connected transformer", k, balanceP, balanceQ);
                    } else {
                        LOG.info("nodes {} {} {}", k, balanceP, balanceQ);
                    }
                }
                show--;
            });
        });
    }

    private Map<List<String>, PropertyBag> calcBalance(String config) {

        Map<List<String>, PropertyBag> report = new HashMap<>();
        List<String> pn = new ArrayList<>(Arrays.asList("balanceP", "balanceQ"));

        joinedNodes.forEach(nodes -> {
            nodes.forEach(n -> {
                PropertyBag node = nodeParameters.get(n);
                LOG.debug("------  equipment node ----------> {}", n);
                if (n.startsWith("_e53a3164") || n.startsWith("_f8c7523f")) {
                    LOG.debug("node {}", node);
                }
                if (!equipmentsInNode.containsKey(n)) {
                    double p = node.asDouble("p");
                    double q = node.asDouble("q");
                    double balanceP = node.asDouble("balanceP", 0.0);
                    double balanceQ = node.asDouble("balanceQ", 0.0);
                    node.put("balanceP", Double.toString(balanceP + p));
                    node.put("balanceQ", Double.toString(balanceQ + q));
                    return;
                }
                equipmentsInNode.get(n).forEach(id -> {
                    PropertyBag line = lineParameters.get(id);
                    if (line != null) {
                        Boolean connected = line.asBoolean("connected", false);
                        if (connected) {
                            PropertyBag node1 = nodeParameters.get(line.get("terminal1"));
                            PropertyBag node2 = nodeParameters.get(line.get("terminal2"));
                            calcFlowLine(n, node1, node2, line, config);
                            double balanceP = node.asDouble("balanceP", 0.0);
                            double balanceQ = node.asDouble("balanceQ", 0.0);
                            node.put("balanceP", Double.toString(balanceP + line.asDouble("p")));
                            node.put("balanceQ", Double.toString(balanceQ + line.asDouble("q")));
                            LOG.debug("Line {}  Line P {} Q {} balanceP {} balanceQ {}", line,
                                    line.asDouble("p"), line.asDouble("q"),
                                    node.asDouble("balanceP", 0.0),
                                    node.asDouble("balanceQ", 0.0));
                        }
                    }
                    PropertyBag transformer = transformerParameters.get(id);
                    if (transformer != null) {
                        Boolean connected1 = transformer.asBoolean("connected1", false);
                        Boolean connected2 = transformer.asBoolean("connected2", false);
                        Boolean connected3 = transformer.asBoolean("connected3", connected1);
                        PropertyBag node3 = nodeParameters.get(transformer.get("terminal3"));
                        if (connected1 && connected2 && connected3) {
                            PropertyBag node1 = nodeParameters.get(transformer.get("terminal1"));
                            PropertyBag node2 = nodeParameters.get(transformer.get("terminal2"));
                            if (node3 == null) {
                                calcFlowT2x(n, node1, node2, transformer, config);
                            } else {
                                calcFlowT3x(n, node1, node2, node3, transformer, config);
                            }
                            double balanceP = node.asDouble("balanceP", 0.0);
                            double balanceQ = node.asDouble("balanceQ", 0.0);
                            node.put("balanceP",
                                    Double.toString(balanceP + transformer.asDouble("p")));
                            node.put("balanceQ",
                                    Double.toString(balanceQ + transformer.asDouble("q")));
                            LOG.debug("Transformer {} P {} Q {} ", transformer,
                                    transformer.asDouble("p"),
                                    transformer.asDouble("q"));
                        } else {
                            if (node3 == null) {
                                if (connected1 != connected2) {
                                    node.put("partial", "true");
                                }
                            } else {
                                if (connected1 != connected2 || connected1 != connected3) {
                                    node.put("partial", "true");
                                }
                            }
                        }
                    }
                });
                double p = node.asDouble("p");
                double q = node.asDouble("q");
                double balanceP = node.asDouble("balanceP", 0.0);
                double balanceQ = node.asDouble("balanceQ", 0.0);
                node.put("balanceP", Double.toString(balanceP + p));
                node.put("balanceQ", Double.toString(balanceQ + q));

                LOG.debug("equipment {} ,  {}", n, equipmentsInNode.get(n));
                LOG.debug("node {} P {} Q {} balanceP {} balanceQ {}", n, p, q,
                        nodeParameters.get(n).asDouble("balanceP"),
                        nodeParameters.get(n).asDouble("balanceQ"));
            });

            double balanceP = nodes.stream().map(n -> {
                PropertyBag node = nodeParameters.get(n);
                return node.asDouble("balanceP", 0.0);
            }).mapToDouble(Double::doubleValue).sum();
            double balanceQ = nodes.stream().map(n -> {
                PropertyBag node = nodeParameters.get(n);
                return node.asDouble("balanceQ", 0.0);
            }).mapToDouble(Double::doubleValue).sum();

            Stream<String> partial = nodes.stream().filter(n -> {
                PropertyBag node = nodeParameters.get(n);
                return node.containsKey("partial");
            });
            LOG.debug("nodes {} {} {}", nodes, balanceP, balanceQ);

            PropertyBag pb = new PropertyBag(pn);
            pb.put("balanceP", "" + balanceP);
            pb.put("balanceQ", "" + balanceQ);
            pb.put("partial", Boolean.toString(partial.count() > 0));
            report.put(nodes, pb);
        });

        Comparator<Map.Entry<List<String>, PropertyBag>> byBalance = (
                Entry<List<String>, PropertyBag> o1,
                Entry<List<String>, PropertyBag> o2) -> {
            if (Double.compare(Math.abs(o1.getValue().asDouble("balanceP", 0.0)),
                    Math.abs(o2.getValue().asDouble("balanceP", 0.0))) == 0) {
                return Double.compare(Math.abs(o1.getValue().asDouble("balanceQ", 0.0)),
                        Math.abs(o2.getValue().asDouble("balanceQ", 0.0)));
            } else {
                return Double.compare(Math.abs(o1.getValue().asDouble("balanceP", 0.0)),
                        Math.abs(o2.getValue().asDouble("balanceP", 0.0)));
            }
        };

        Map<List<String>, PropertyBag> sortedReport = report.entrySet().stream()
                .sorted(byBalance.reversed()).collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (a, b) -> {
                        throw new AssertionError();
                    },
                    LinkedHashMap::new));

        return sortedReport;
    }

    class LineAdmittanceMatrix extends AdmittanceMatrix {

        public void calculate(PropertyBag line, String config) {
            double r = line.asDouble("r");
            double x = line.asDouble("x");
            double bch = line.asDouble("bch");

            String configurationBsh = "end1";
            if (config.equals("default")) {
                configurationBsh = "end1_end2";
            } else if (config.equals("configuration1")) {
                configurationBsh = "end1_end2";
            }

            double bsh1 = 0.0;
            double bsh2 = 0.0;
            if (configurationBsh.equals("end1")) {
                bsh1 = bch;
            } else if (configurationBsh.equals("end2")) {
                bsh2 = bch;
            } else if (configurationBsh.equals("end1_end2")) {
                bsh1 = bch * 0.5;
                bsh2 = bch * 0.5;
            }
            Complex z = new Complex(r, x);
            Complex ysh1 = new Complex(0.0, bsh1);
            Complex ysh2 = new Complex(0.0, bsh2);
            yff = z.reciprocal().add(ysh1);
            yft = z.reciprocal().negate();
            ytf = z.reciprocal().negate();
            ytt = z.reciprocal().add(ysh2);
        }
    }

    class T2xAdmittanceMatrix extends AdmittanceMatrix {

        public void calculate(PropertyBag transformer, String config) {
            double r1 = transformer.asDouble("r1");
            double x1 = transformer.asDouble("x1");
            double b1 = transformer.asDouble("b1");
            double g1 = transformer.asDouble("g1");
            int pac1 = transformer.asInt("pac1", 0);
            double ratedU1 = transformer.asDouble("ratedU1");
            double rns1 = transformer.asDouble("rns1", 0.0);
            double rsvi1 = transformer.asDouble("rsvi1", 0.0);
            double rstep1 = transformer.asDouble("rstep1", 0.0);
            double pns1 = transformer.asDouble("pns1", 0.0);
            double psvi1 = transformer.asDouble("psvi1", 0.0);
            double pstep1 = transformer.asDouble("pstep1", 0.0);
            double r2 = transformer.asDouble("r2");
            double x2 = transformer.asDouble("x2");
            double b2 = transformer.asDouble("b2");
            double g2 = transformer.asDouble("g2");
            int pac2 = transformer.asInt("pac2", 0);
            double ratedU2 = transformer.asDouble("ratedU2");
            double rns2 = transformer.asDouble("rns2", 0.0);
            double rsvi2 = transformer.asDouble("rsvi2", 0.0);
            double rstep2 = transformer.asDouble("rstep2", 0.0);
            double pns2 = transformer.asDouble("pns2", 0.0);
            double psvi2 = transformer.asDouble("psvi2", 0.0);
            double pstep2 = transformer.asDouble("pstep2", 0.0);
            double pwca1 = transformer.asDouble("pwca1", 0.0);
            double pwca2 = transformer.asDouble("pwca2", 0.0);
            String ptype1 = transformer.get("ptype1");
            String ptype2 = transformer.get("ptype2");

            String configurationRatio = "ratio_end1";
            String configurationYshunt = "yshunt_end1";
            String configurationPhaseAngleClock = "clock_off";
            LOG.debug(" transformer {}", transformer);

            if (config.equals("default")) {
                configurationRatio = "ratio_end1_end2_end2";
                configurationYshunt = "yshunt_end1";
                configurationPhaseAngleClock = "clock_off";
            } else if (config.equals("configuration1")) {
                configurationRatio = "ratio_end1";
                configurationYshunt = "yshunt_end1";
                configurationPhaseAngleClock = "clock_on";
            }

            // ratio configuration

            double rtc1a = 1.0 + (rstep1 - rns1) * (rsvi1 / 100.0);
            double rtc1A = 0.0;
            double ptc1a = 1.0;
            double ptc1A = (pstep1 - pns1) * (psvi1 / 100.0);
            if (ptype1 != null && ptype1.endsWith("asymmetrical")) {
                double dx = 1.0
                        + (pstep1 - pns1) * (psvi1 / 100.0) * Math.cos(Math.toRadians(pwca1));
                double dy = (pstep1 - pns1) * (psvi1 / 100.0) * Math.sin(Math.toRadians(pwca1));
                ptc1a = Math.hypot(dx, dy);
                ptc1A = Math.toDegrees(Math.atan2(dy, dx));
            }

            double rtc2a = 1.0 + (rstep2 - rns2) * (rsvi2 / 100.0);
            double rtc2A = 0.0;
            double ptc2a = 1.0;
            double ptc2A = (pstep2 - pns2) * (psvi2 / 100.0);
            if (ptype2 != null && ptype2.endsWith("asymmetrical")) {
                double dx = 1.0
                        + (pstep2 - pns2) * (psvi2 / 100.0) * Math.cos(Math.toRadians(pwca2));
                double dy = (pstep2 - pns2) * (psvi2 / 100.0) * Math.sin(Math.toRadians(pwca2));
                ptc2a = Math.hypot(dx, dy);
                ptc2A = Math.toDegrees(Math.atan2(dy, dx));
            }
            LOG.debug(" rtc1 {} {} ptc1 {} {} rtc2 {} {} ptc2 {} {}", rtc1a, rtc1A, ptc1a, ptc1A,
                    rtc2a, rtc2A, ptc2a, ptc2A);

            double a1 = 1.0;
            double angle1 = 0.0;
            double a2 = 1.0;
            double angle2 = 0.0;

            if (configurationRatio.equals("ratio_end1")) {
                a1 = (ratedU1 / ratedU2) * rtc1a * ptc1a * rtc2a * ptc2a;
                angle1 = rtc1A + ptc1A + rtc2A + ptc2A;
            } else if (configurationRatio.equals("ratio_end2")) {
                a2 = (ratedU2 / ratedU1) * rtc1a * ptc1a * rtc2a * ptc2a;
                angle2 = rtc1A + ptc1A + rtc2A + ptc2A;
            } else if (configurationRatio.equals("ratio_end1_end2_end1")) {
                a1 = (ratedU1 / ratedU2) * rtc1a * ptc1a;
                angle1 = rtc1A + ptc1A;
                a2 = (ratedU2 / ratedU2) * rtc2a * ptc2a;
                angle2 = rtc2A + ptc2A;
            } else if (configurationRatio.equals("ratio_end1_end2_end2")) {
                a1 = (ratedU1 / ratedU1) * rtc1a * ptc1a;
                angle1 = rtc1A + ptc1A;
                a2 = (ratedU2 / ratedU1) * rtc2a * ptc2a;
                angle2 = rtc2A + ptc2A;
            } else if (configurationRatio.equals("ratio_end1_end2")) {
                a1 = (ratedU1 / ratedU1) * rtc1a * ptc1a * rtc2a * ptc2a;
                angle1 = rtc1A + ptc1A + rtc2A + ptc2A;
                a2 = ratedU2 / ratedU1;
                angle2 = 0.0;
            }

            // yshunt configuration

            Complex ysh1 = Complex.ZERO;
            Complex ysh2 = Complex.ZERO;
            if (configurationYshunt.equals("yshunt_end1")) {
                ysh1 = ysh1.add(new Complex(g1 + g2, b1 + b2));
            } else if (configurationYshunt.equals("yshunt_end2")) {
                ysh2 = ysh2.add(new Complex(g1 + g2, b1 + b2));
            } else if (configurationYshunt.equals("yshunt_end1_end2")) {
                ysh1 = ysh1.add(new Complex(g1, b1));
                ysh2 = ysh2.add(new Complex(g2, b2));
            }

            // phaseAngleClock configuration

            if (configurationPhaseAngleClock.equals("clock_on")) {
                if (pac1 != 0) {
                    angle1 = addPhaseAngleClock(angle1, pac1);
                }
                if (pac2 != 0) {
                    angle2 = addPhaseAngleClock(angle2, pac2);
                }
            }

            angle1 = Math.toRadians(angle1);
            angle2 = Math.toRadians(angle2);
            Complex aA1 = new Complex(a1 * Math.cos(angle1), a1 * Math.sin(angle1));
            Complex aA2 = new Complex(a2 * Math.cos(angle2), a2 * Math.sin(angle2));

            Complex z = new Complex(r1, x1);
            yff = z.reciprocal().add(ysh1).divide(aA1.conjugate().multiply(aA1));
            yft = z.reciprocal().negate().divide(aA1.conjugate().multiply(aA2));
            ytf = z.reciprocal().negate().divide(aA2.conjugate().multiply(aA1));
            ytt = z.reciprocal().add(ysh2).divide(aA2.conjugate().multiply(aA2));
        }
    }

    class AdmittanceMatrix {
        public Complex getYff() {
            return yff;
        }

        public Complex getYft() {
            return yft;
        }

        public Complex getYtf() {
            return ytf;
        }

        public Complex getYtt() {
            return ytt;
        }

        protected Complex yff;
        protected Complex yft;
        protected Complex ytf;
        protected Complex ytt;
    }

    class T3xAdmittanceMatrix {

        public void calculate(PropertyBag transformer, String config) {
            double r1 = transformer.asDouble("r1");
            double x1 = transformer.asDouble("x1");
            double b1 = transformer.asDouble("b1");
            double g1 = transformer.asDouble("g1");
            int pac1 = transformer.asInt("pac1", 0);
            double ratedU1 = transformer.asDouble("ratedU1");
            double rns1 = transformer.asDouble("rns1", 0.0);
            double rsvi1 = transformer.asDouble("rsvi1", 0.0);
            double rstep1 = transformer.asDouble("rstep1", 0.0);
            double pns1 = transformer.asDouble("pns1", 0.0);
            double psvi1 = transformer.asDouble("psvi1", 0.0);
            double pstep1 = transformer.asDouble("pstep1", 0.0);
            double r2 = transformer.asDouble("r2");
            double x2 = transformer.asDouble("x2");
            double b2 = transformer.asDouble("b2");
            double g2 = transformer.asDouble("g2");
            int pac2 = transformer.asInt("pac2", 0);
            double ratedU2 = transformer.asDouble("ratedU2");
            double rns2 = transformer.asDouble("rns2", 0.0);
            double rsvi2 = transformer.asDouble("rsvi2", 0.0);
            double rstep2 = transformer.asDouble("rstep2", 0.0);
            double pns2 = transformer.asDouble("pns2", 0.0);
            double psvi2 = transformer.asDouble("psvi2", 0.0);
            double pstep2 = transformer.asDouble("pstep2", 0.0);
            double r3 = transformer.asDouble("r3");
            double x3 = transformer.asDouble("x3");
            double b3 = transformer.asDouble("b3");
            double g3 = transformer.asDouble("g3");
            int pac3 = transformer.asInt("pac3", 0);
            double ratedU3 = transformer.asDouble("ratedU3");
            double rns3 = transformer.asDouble("rns3", 0.0);
            double rsvi3 = transformer.asDouble("rsvi3", 0.0);
            double rstep3 = transformer.asDouble("rstep3", 0.0);
            double pns3 = transformer.asDouble("pns3", 0.0);
            double psvi3 = transformer.asDouble("psvi3", 0.0);
            double pstep3 = transformer.asDouble("pstep3", 0.0);
            double pwca1 = transformer.asDouble("pwca1", 0.0);
            double pwca2 = transformer.asDouble("pwca2", 0.0);
            double pwca3 = transformer.asDouble("pwca3", 0.0);
            String ptype1 = transformer.get("ptype1");
            String ptype2 = transformer.get("ptype2");
            String ptype3 = transformer.get("ptype3");

            String configurationRatio = "ratio_Tx123_end2";
            String configurationYshunt = "yshunt_Tx123_outside_end";
            String configurationPhaseAngleClock = "clock_off";

            if (config.equals("default")) {
                configurationRatio = "ratio_Tx123_end2";
                configurationYshunt = "yshunt_Tx123_outside_end";
                configurationPhaseAngleClock = "clock_off";
            } else if (config.equals("configuration1")) {
                configurationRatio = "ratio_Tx123_end2";
                configurationYshunt = "yshunt_Tx123_outside_end";
                configurationPhaseAngleClock = "clock_on_inside_end";
            }
            LOG.debug(" transformer {}", transformer);

            // ratio configuration

            double rtc1a = 1.0 + (rstep1 - rns1) * (rsvi1 / 100.0);
            double rtc1A = 0.0;
            double ptc1a = 1.0;
            double ptc1A = (pstep1 - pns1) * (psvi1 / 100.0);
            if (ptype1 != null && ptype1.endsWith("asymmetrical")) {
                double dx = 1.0
                        + (pstep1 - pns1) * (psvi1 / 100.0) * Math.cos(Math.toRadians(pwca1));
                double dy = (pstep1 - pns1) * (psvi1 / 100.0) * Math.sin(Math.toRadians(pwca1));
                ptc1a = Math.hypot(dx, dy);
                ptc1A = Math.toDegrees(Math.atan2(dy, dx));
            }
            double rtc2a = 1.0 + (rstep2 - rns2) * (rsvi2 / 100.0);
            double rtc2A = 0.0;
            double ptc2a = 1.0;
            double ptc2A = (pstep2 - pns2) * (psvi2 / 100.0);
            if (ptype2 != null && ptype2.endsWith("asymmetrical")) {
                double dx = 1.0
                        + (pstep2 - pns2) * (psvi2 / 100.0) * Math.cos(Math.toRadians(pwca2));
                double dy = (pstep2 - pns2) * (psvi2 / 100.0) * Math.sin(Math.toRadians(pwca2));
                ptc2a = Math.hypot(dx, dy);
                ptc2A = Math.toDegrees(Math.atan2(dy, dx));
            }
            double rtc3a = 1.0 + (rstep3 - rns3) * (rsvi3 / 100.0);
            double rtc3A = 0.0;
            double ptc3a = 1.0;
            double ptc3A = (pstep3 - pns3) * (psvi3 / 100.0);
            if (ptype3 != null && ptype3.endsWith("asymmetrical")) {
                double dx = 1.0
                        + (pstep3 - pns3) * (psvi3 / 100.0) * Math.cos(Math.toRadians(pwca3));
                double dy = (pstep3 - pns3) * (psvi3 / 100.0) * Math.sin(Math.toRadians(pwca3));
                ptc3a = Math.hypot(dx, dy);
                ptc3A = Math.toDegrees(Math.atan2(dy, dx));
            }
            LOG.debug(" rtc1 {} {} ptc1 {} {} rtc2 {} {} ptc2 {} {} rtc3 {} {} ptc3 {} {}", rtc1a,
                    rtc1A, ptc1a, ptc1A, rtc2a, rtc2A, ptc2a, ptc2A, rtc3a, rtc3A, ptc3a, ptc3A);

            double a11 = 1.0;
            double angle11 = 0.0;
            double a12 = 1.0;
            double angle12 = 0.0;

            double a21 = 1.0;
            double angle21 = 0.0;
            double a22 = 1.0;
            double angle22 = 0.0;

            double a31 = 1.0;
            double angle31 = 0.0;
            double a32 = 1.0;
            double angle32 = 0.0;

            double ratedU0 = 1.0;
            if (configurationRatio.equals("ratio_Tx123_end2")) {
                a11 = (ratedU1 / ratedU1) * rtc1a * ptc1a;
                angle11 = rtc1A + ptc1A;
                a12 = ratedU0 / ratedU1;
                angle12 = 0.0;
                a21 = (ratedU2 / ratedU2) * rtc2a * ptc2a;
                angle21 = rtc2A + ptc2A;
                a22 = ratedU0 / ratedU2;
                angle22 = 0.0;
                a31 = (ratedU3 / ratedU3) * rtc3a * ptc3a;
                angle31 = rtc3A + ptc3A;
                a32 = ratedU0 / ratedU3;
                angle32 = 0.0;
            } else if (configurationRatio.equals("ratio_Tx123_end1")) {
                a11 = (ratedU1 / ratedU0) * rtc1a * ptc1a;
                angle11 = rtc1A + ptc1A;
                a12 = ratedU0 / ratedU0;
                angle12 = 0.0;
                a21 = (ratedU2 / ratedU0) * rtc2a * ptc2a;
                angle21 = rtc2A + ptc2A;
                a22 = ratedU0 / ratedU0;
                angle22 = 0.0;
                a31 = (ratedU3 / ratedU0) * rtc3a * ptc3a;
                angle31 = rtc3A + ptc3A;
                a32 = ratedU0 / ratedU0;
                angle32 = 0.0;
            }

            // yshunt configuration

            Complex ysh11 = Complex.ZERO;
            Complex ysh12 = Complex.ZERO;
            Complex ysh21 = Complex.ZERO;
            Complex ysh22 = Complex.ZERO;
            Complex ysh31 = Complex.ZERO;
            Complex ysh32 = Complex.ZERO;
            if (configurationYshunt.equals("yshunt_Tx123_outside_end")) {
                ysh11 = ysh11.add(new Complex(g1, b1));
                ysh21 = ysh21.add(new Complex(g2, b2));
                ysh31 = ysh31.add(new Complex(g3, b3));
            } else if (configurationYshunt.equals("yshunt_Tx123_inside_end")) {
                ysh12 = ysh12.add(new Complex(g1, b1));
                ysh22 = ysh22.add(new Complex(g2, b2));
                ysh32 = ysh32.add(new Complex(g3, b3));
            }

            // phaseAngleClock configuration

            if (configurationPhaseAngleClock.equals("clock_on_inside_end")) {
                if (pac1 != 0) {
                    angle12 = addPhaseAngleClock(angle12, pac1);
                }
                if (pac2 != 0) {
                    angle22 = addPhaseAngleClock(angle22, pac2);
                }
                if (pac3 != 0) {
                    angle32 = addPhaseAngleClock(angle32, pac3);
                }
            } else if (configurationPhaseAngleClock.equals("clock_on_outside_end")) {
                if (pac1 != 0) {
                    angle11 = addPhaseAngleClock(angle11, pac1);
                }
                if (pac2 != 0) {
                    angle21 = addPhaseAngleClock(angle21, pac2);
                }
                if (pac3 != 0) {
                    angle31 = addPhaseAngleClock(angle31, pac3);
                }
            }

            angle11 = Math.toRadians(angle11);
            angle12 = Math.toRadians(angle12);
            angle21 = Math.toRadians(angle21);
            angle22 = Math.toRadians(angle22);
            angle31 = Math.toRadians(angle31);
            angle32 = Math.toRadians(angle32);
            Complex aA11 = new Complex(a11 * Math.cos(angle11), a11 * Math.sin(angle11));
            Complex aA12 = new Complex(a12 * Math.cos(angle12), a12 * Math.sin(angle12));
            Complex aA21 = new Complex(a21 * Math.cos(angle21), a21 * Math.sin(angle21));
            Complex aA22 = new Complex(a22 * Math.cos(angle22), a22 * Math.sin(angle22));
            Complex aA31 = new Complex(a31 * Math.cos(angle31), a31 * Math.sin(angle31));
            Complex aA32 = new Complex(a32 * Math.cos(angle32), a32 * Math.sin(angle32));
            LOG.debug(" aA11 {} aA12 {} aA21 {} aA22 {} aA31 {} aA32 {}", aA11, aA12, aA21, aA22,
                    aA31, aA32);

            Complex z1 = new Complex(r1, x1);
            yff1 = z1.reciprocal().add(ysh11).divide(aA11.conjugate().multiply(aA11));
            yft1 = z1.reciprocal().negate().divide(aA11.conjugate().multiply(aA12));
            ytf1 = z1.reciprocal().negate().divide(aA12.conjugate().multiply(aA11));
            ytt1 = z1.reciprocal().add(ysh12).divide(aA12.conjugate().multiply(aA12));

            Complex z2 = new Complex(r2, x2);
            yff2 = z2.reciprocal().add(ysh21).divide(aA21.conjugate().multiply(aA21));
            yft2 = z2.reciprocal().negate().divide(aA21.conjugate().multiply(aA22));
            ytf2 = z2.reciprocal().negate().divide(aA22.conjugate().multiply(aA21));
            ytt2 = z2.reciprocal().add(ysh22).divide(aA22.conjugate().multiply(aA22));

            Complex z3 = new Complex(r3, x3);
            yff3 = z3.reciprocal().add(ysh31).divide(aA31.conjugate().multiply(aA31));
            yft3 = z3.reciprocal().negate().divide(aA31.conjugate().multiply(aA32));
            ytf3 = z3.reciprocal().negate().divide(aA32.conjugate().multiply(aA31));
            ytt3 = z3.reciprocal().add(ysh32).divide(aA32.conjugate().multiply(aA32));
        }

        public Complex getYff1() {
            return yff1;
        }

        public Complex getYft1() {
            return yft1;
        }

        public Complex getYtf1() {
            return ytf1;
        }

        public Complex getYtt1() {
            return ytt1;
        }

        public Complex getYff2() {
            return yff2;
        }

        public Complex getYft2() {
            return yft2;
        }

        public Complex getYtf2() {
            return ytf2;
        }

        public Complex getYtt2() {
            return ytt2;
        }

        public Complex getYff3() {
            return yff3;
        }

        public Complex getYft3() {
            return yft3;
        }

        public Complex getYtf3() {
            return ytf3;
        }

        public Complex getYtt3() {
            return ytt3;
        }

        protected Complex yff1;
        protected Complex yft1;
        protected Complex ytf1;
        protected Complex ytt1;
        protected Complex yff2;
        protected Complex yft2;
        protected Complex ytf2;
        protected Complex ytt2;
        protected Complex yff3;
        protected Complex yft3;
        protected Complex ytf3;
        protected Complex ytt3;
    }

    private double addPhaseAngleClock(double angle, int phaseAngleClock) {
        double phaseAngleClockDegree = 0.0;
        phaseAngleClockDegree += phaseAngleClock * 30.0;
        phaseAngleClockDegree = Math.IEEEremainder(phaseAngleClockDegree, 360.0);
        if (phaseAngleClockDegree > 180.0) {
            phaseAngleClockDegree -= 360.0;
        }
        return angle + phaseAngleClockDegree;
    }

    class CalcFlow {

        public void calculate(Complex yff, Complex yft, Complex ytf, Complex ytt, Complex vf,
                Complex vt) {
            Complex ift = yft.multiply(vt).add(yff.multiply(vf));
            sft = ift.conjugate().multiply(vf);

            Complex itf = ytf.multiply(vf).add(ytt.multiply(vt));
            stf = itf.conjugate().multiply(vt);
        }

        public Complex getSft() {
            return sft;
        }

        public Complex getStf() {
            return stf;
        }

        private Complex sft;
        private Complex stf;
    }

    private void calcFlowT2x(String n, PropertyBag node1, PropertyBag node2,
            PropertyBag transformer,
            String config) {
        if (n.startsWith("_e53a3164") || n.startsWith("_f8c7523f")) {
            LOG.debug("From {}  {} To {}  {}", node1.asDouble("v"), node1.asDouble("angle"),
                    node2.asDouble("v"), node2.asDouble("angle"));
        }
        double v1 = node1.asDouble("v");
        double angle1 = Math.toRadians(node1.asDouble("angle"));
        double v2 = node2.asDouble("v");
        double angle2 = Math.toRadians(node2.asDouble("angle"));
        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));

        T2xAdmittanceMatrix admittanceMatrix = new T2xAdmittanceMatrix();
        admittanceMatrix.calculate(transformer, config);

        CalcFlow calcFlow = new CalcFlow();
        calcFlow.calculate(admittanceMatrix.getYff(), admittanceMatrix.getYft(),
                admittanceMatrix.getYtf(), admittanceMatrix.getYtt(), vf, vt);

        Complex res = Complex.ZERO;
        if (transformer.get("terminal1").equals(n)) {
            res = calcFlow.getSft();
        } else if (transformer.get("terminal2").equals(n)) {
            res = calcFlow.getStf();
        }
        if (n.startsWith("_e53a3164") || n.startsWith("_f8c7523f")) {
            LOG.debug("t2x {} {} node {}", res.getReal(), res.getImaginary(), n);
        }
        transformer.put("p", Double.toString(res.getReal()));
        transformer.put("q", Double.toString(res.getImaginary()));
    }

    private void calcFlowT3x(String n, PropertyBag node1, PropertyBag node2, PropertyBag node3,
            PropertyBag transformer, String config) {
        if (n.startsWith("_e53a3164") || n.startsWith("_f8c7523f")) {
            LOG.debug("End1 {}  {} End2 {}  {} End3 {} {}", node1.asDouble("v"),
                    node1.asDouble("angle"),
                    node2.asDouble("v"), node2.asDouble("angle"), node3.asDouble("v"),
                    node3.asDouble("angle"));
        }
        double v1 = node1.asDouble("v");
        double angle1 = Math.toRadians(node1.asDouble("angle"));
        double v2 = node2.asDouble("v");
        double angle2 = Math.toRadians(node2.asDouble("angle"));
        double v3 = node3.asDouble("v");
        double angle3 = Math.toRadians(node3.asDouble("angle"));
        Complex vf1 = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vf2 = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));
        Complex vf3 = new Complex(v3 * Math.cos(angle3), v3 * Math.sin(angle3));

        T3xAdmittanceMatrix admittanceMatrix = new T3xAdmittanceMatrix();
        admittanceMatrix.calculate(transformer, config);
        Complex v0 = admittanceMatrix.getYtf1().multiply(vf1)
                .add(admittanceMatrix.getYtf2().multiply(vf2))
                .add(admittanceMatrix.getYtf3().multiply(vf3))
                .negate().divide(admittanceMatrix.getYtt1().add(admittanceMatrix.getYtt2())
                        .add(admittanceMatrix.getYtt3()));
        LOG.debug("V0 ------> {} {}", v0.abs(), v0.getArgument());
        CalcFlow calcFlow = new CalcFlow();

        Complex res = Complex.ZERO;
        if (transformer.get("terminal1").equals(n)) {
            calcFlow.calculate(admittanceMatrix.getYff1(), admittanceMatrix.getYft1(),
                    admittanceMatrix.getYtf1(), admittanceMatrix.getYtt1(), vf1, v0);
            res = calcFlow.getSft();
        } else if (transformer.get("terminal2").equals(n)) {
            calcFlow.calculate(admittanceMatrix.getYff2(), admittanceMatrix.getYft2(),
                    admittanceMatrix.getYtf2(), admittanceMatrix.getYtt2(), vf2, v0);
            res = calcFlow.getSft();
        } else if (transformer.get("terminal3").equals(n)) {
            calcFlow.calculate(admittanceMatrix.getYff3(), admittanceMatrix.getYft3(),
                    admittanceMatrix.getYtf3(), admittanceMatrix.getYtt3(), vf3, v0);
            res = calcFlow.getSft();
        }
        if (n.startsWith("_e53a3164") || n.startsWith("_f8c7523f")) {
            LOG.debug("trafo3D {} {} node {}", res.getReal(), res.getImaginary(), n);
        }
        transformer.put("p", Double.toString(res.getReal()));
        transformer.put("q", Double.toString(res.getImaginary()));
    }

    private void calcFlowLine(String n, PropertyBag node1, PropertyBag node2, PropertyBag line,
            String config) {
        if (n.startsWith("_e53a3164") || n.startsWith("_f8c7523f")) {
            LOG.debug("From {}  {} To {}  {}", node1.asDouble("v"), node1.asDouble("angle"),
                    node2.asDouble("v"), node2.asDouble("angle"));
        }
        double v1 = node1.asDouble("v");
        double angle1 = Math.toRadians(node1.asDouble("angle"));
        double v2 = node2.asDouble("v");
        double angle2 = Math.toRadians(node2.asDouble("angle"));
        Complex vf = new Complex(v1 * Math.cos(angle1), v1 * Math.sin(angle1));
        Complex vt = new Complex(v2 * Math.cos(angle2), v2 * Math.sin(angle2));
        LineAdmittanceMatrix admittanceMatrix = new LineAdmittanceMatrix();
        admittanceMatrix.calculate(line, config);

        CalcFlow calcFlow = new CalcFlow();
        calcFlow.calculate(admittanceMatrix.getYff(), admittanceMatrix.getYft(),
                admittanceMatrix.getYtf(), admittanceMatrix.getYtt(), vf, vt);

        Complex res = Complex.ZERO;
        if (line.get("terminal1").equals(n)) {
            res = calcFlow.getSft();
        } else if (line.get("terminal2").equals(n)) {
            res = calcFlow.getStf();
        }
        if (n.startsWith("_e53a3164") || n.startsWith("_f8c7523f")) {
            LOG.debug("line {} {} node {}", res.getReal(), res.getImaginary(), n);
        }
        line.put("p", Double.toString(res.getReal()));
        line.put("q", Double.toString(res.getImaginary()));
    }

    private static void loadModel(TestGridModel gridModel) throws IOException {
        ReadOnlyDataSource ds = gridModel.dataSource();

        // Check that the case exists
        // even if we do not have any available triple store implementation
        // cimNamespace() will throw an exception if no CGMES data is found
        CgmesOnDataSource cds = new CgmesOnDataSource(ds);
        cds.cimNamespace();

        String impl = TripleStoreFactory.defaultImplementation();

        CgmesModel actual = CgmesModelFactory.create(ds, impl);
        nodeParameters = nodeParameters(actual);
        nodeParameters.keySet().forEach(key -> {
            LOG.debug("node {} ,  {}", key, nodeParameters.get(key));
        });

        equipmentsInNode = new HashMap<>();
        lineParameters = lineParameters(actual, equipmentsInNode);
        transformerParameters = transformerParameters(actual,
                equipmentsInNode);
        joinedNodes = nodeSwitchesParameters(actual, nodeParameters);
        nodeFlow(actual, nodeParameters);

        lineParameters.keySet().forEach(key -> {
            LOG.debug("line {} , {}", key, lineParameters.get(key));
        });
        transformerParameters.keySet().forEach(key -> {
            LOG.debug("transformer {} , {}", key, transformerParameters.get(key));
        });
    }

    private static List<List<String>> nodeSwitchesParameters(CgmesModel cgmes,
            Map<String, PropertyBag> nodeParameters) {

        List<List<String>> joinedNodes = new ArrayList<>();
        Map<String, List<String>> nodes = new HashMap<>();

        String retainedSwitches = "SELECT * "
                + "WHERE { "
                + "{ GRAPH ?graph {"
                + "    ?Switch"
                + "        a ?type ;"
                + "        cim:IdentifiedObject.name ?name ;"
                + "        cim:Switch.retained ?retained ;"
                + "        cim:Equipment.EquipmentContainer ?EquipmentContainer ."
                + "    VALUES ?type { cim:Switch cim:Breaker cim:Disconnector } ."
                + "    ?Terminal1"
                + "        a cim:Terminal ;"
                + "        cim:Terminal.ConductingEquipment ?Switch ."
                + "    ?Terminal2"
                + "        a cim:Terminal ;"
                + "        cim:Terminal.ConductingEquipment ?Switch ."
                + "    FILTER ( STR(?Terminal1) < STR(?Terminal2) )"
                + "}} "
                + "OPTIONAL { GRAPH ?graphSSH {"
                + "    ?Switch cim:Switch.open ?open"
                + "}}"
                + "}";
        ((CgmesModelTripleStore) cgmes).query(retainedSwitches).forEach(rs -> {
            Boolean retained = rs.asBoolean("retained", false);
            Boolean open = rs.asBoolean("open", false);
            if (retained && !open) {
                CgmesTerminal t = cgmes.terminal(rs.getId(CgmesNames.TERMINAL + "1"));
                String id1 = t.topologicalNode();
                t = cgmes.terminal(rs.getId(CgmesNames.TERMINAL + "2"));
                String id2 = t.topologicalNode();
                List<String> node = nodes.computeIfAbsent(id1, x -> new ArrayList<>());
                node.add(id2);
                node = nodes.computeIfAbsent(id2, x -> new ArrayList<>());
                node.add(id1);
            }
        });

        List<String> joined = new ArrayList<>();
        nodes.keySet().forEach(k -> {
            if (joined.contains(k)) {
                return;
            }
            List<String> joinNodes = new ArrayList<>();
            joinNodes.add(k);
            joined.add(k);
            List<String> ns = nodes.get(k);
            ns.forEach(node -> {
                if (joined.contains(node)) {
                    return;
                }
                joinNodes.add(node);
                joined.add(node);
            });
            joinedNodes.add(joinNodes);
        });

        nodeParameters.keySet().forEach(node -> {
            if (joined.contains(node)) {
                return;
            }
            List<String> joinNodes = new ArrayList<>();
            joinNodes.add(node);
            joined.add(node);
            joinedNodes.add(joinNodes);
        });

        return joinedNodes;
    }

    private static Map<String, PropertyBag> nodeParameters(CgmesModel cgmes) {

        propertyNames = new ArrayList<>(Arrays.asList("v", "angle", "p", "q"));
        Map<String, PropertyBag> nodes = new HashMap<>();
        voltages = new HashMap<>();

        String svVoltage = "SELECT * "
                + "WHERE { "
                + "{ GRAPH ?graphSV {"
                + "    ?SvVoltage"
                + "        a cim:SvVoltage ;"
                + "        cim:SvVoltage.TopologicalNode ?TopologicalNode ;"
                + "        cim:SvVoltage.angle ?angle ;"
                + "        cim:SvVoltage.v ?v"
                + "}}"
                + "}";
        ((CgmesModelTripleStore) cgmes).query(svVoltage).forEach(v -> {
            String id = v.getId("TopologicalNode");
            voltages.put(id, v);
        });

        cgmes.topologicalNodes().forEach(n -> {
            String id = n.getId("TopologicalNode");
            String v = n.get("v");
            String angle = n.get("angle");

            PropertyBag node = nodes.computeIfAbsent(id, x -> new PropertyBag(propertyNames));
            node.put("v", v);
            node.put("angle", angle);
            node.put("p", "0.0");
            node.put("q", "0.0");
        });

        return nodes;
    }

    private static void nodeFlow(CgmesModel cgmes, Map<String, PropertyBag> nodes) {
        cgmes.energyConsumers().forEach(e -> terminalFlow(cgmes, nodes, e));
        cgmes.energySources().forEach(e -> terminalFlow(cgmes, nodes, e));
        cgmes.equivalentInjections().forEach(e -> terminalFlow(cgmes, nodes, e));
        cgmes.shuntCompensators().forEach(e -> equipmentFlow(cgmes, nodes, e));
        cgmes.staticVarCompensators().forEach(e -> terminalFlow(cgmes, nodes, e));
        cgmes.asynchronousMachines().forEach(e -> terminalFlow(cgmes, nodes, e));
        cgmes.synchronousMachines().forEach(e -> terminalFlow(cgmes, nodes, e));
    }

    private static void terminalFlow(CgmesModel cgmes, Map<String, PropertyBag> nodes,
            PropertyBag equipment) {
        CgmesTerminal t = cgmes.terminal(equipment.getId(CgmesNames.TERMINAL));
        if (!t.connected()) {
            return;
        }
        String nodeId = t.topologicalNode();
        PropertyBag node = nodes.get(nodeId);
        if (node == null) {
            PropertyBag n = voltages.get(nodeId);
            String v = n.get("v");
            String angle = n.get("angle");

            node = nodes.computeIfAbsent(nodeId, x -> new PropertyBag(propertyNames));
            node.put("v", v);
            node.put("angle", angle);
            node.put("p", "0.0");
            node.put("q", "0.0");
        }
        double pNode = node.asDouble("p");
        double qNode = node.asDouble("q");

        double pEquipment = t.flow().p();
        double qEquipment = t.flow().q();

        node.put("p", String.valueOf(pNode + pEquipment));
        node.put("q", String.valueOf(qNode + qEquipment));
    }

    private static void equipmentFlow(CgmesModel cgmes, Map<String, PropertyBag> nodes,
            PropertyBag equipment) {
        CgmesTerminal t = cgmes.terminal(equipment.getId(CgmesNames.TERMINAL));
        if (!t.connected()) {
            return;
        }
        String nodeId = t.topologicalNode();
        PropertyBag node = nodes.get(nodeId);
        if (node == null) {
            PropertyBag n = voltages.get(nodeId);
            String v = n.get("v");
            String angle = n.get("angle");

            node = nodes.computeIfAbsent(nodeId, x -> new PropertyBag(propertyNames));
            node.put("v", v);
            node.put("angle", angle);
            node.put("p", "0.0");
            node.put("q", "0.0");
        }
        double v = node.asDouble("v");
        double bPerSection = equipment.asDouble(CgmesNames.B_PER_SECTION, 0.0);
        int normalSections = equipment.asInt("normalSections", 0);
        double sections = equipment.asDouble("SVsections", normalSections);

        double qNode = node.asDouble("q");
        double qEquipment = bPerSection * sections * v * v;
        node.put("q", String.valueOf(qNode + qEquipment));
    }

    private static Map<String, PropertyBag> lineParameters(CgmesModel cgmes,
            Map<String, List<String>> equipmentsInNode) {

        propertyNames = new ArrayList<>(Arrays.asList("r", "x", "bch"));
        Map<String, PropertyBag> lines = new HashMap<>();
        cgmes.acLineSegments().forEach(l -> {
            String id = l.getId(CgmesNames.AC_LINE_SEGMENT);
            String r = l.get("r");
            String x = l.get("x");
            String bch = l.get("bch");

            PropertyBag line = lines.computeIfAbsent(id, z -> new PropertyBag(propertyNames));
            line.put("r", r);
            line.put("x", x);
            line.put("bch", bch);

            CgmesTerminal t = cgmes.terminal(l.getId(CgmesNames.TERMINAL + 1));
            String nodeId = t.topologicalNode();
            boolean t1connected = t.connected();
            line.put("terminal1", nodeId);
            List<String> idLines = equipmentsInNode.computeIfAbsent(nodeId, z -> new ArrayList<>());
            idLines.add(id);

            t = cgmes.terminal(l.getId(CgmesNames.TERMINAL + 2));
            nodeId = t.topologicalNode();
            boolean t2connected = t.connected();
            line.put("terminal2", nodeId);
            line.put("connected", Boolean.toString(t1connected && t2connected));
            idLines = equipmentsInNode.computeIfAbsent(nodeId, z -> new ArrayList<>());
            idLines.add(id);
        });
        return lines;
    }

    private static Map<String, PropertyBag> transformerParameters(CgmesModel cgmes,
            Map<String, List<String>> equipmentsInNode) {

        propertyNames = new ArrayList<>(Arrays.asList(
                "r1", "x1", "b1", "g1", "pac1", "ratedU1", "rns1", "rsvi1", "rstep1", "pns1",
                "psvi1", "pstep1",
                "r2", "x2", "b2", "g2", "pac2", "ratedU2", "rns2", "rsvi2", "rstep2", "pns2",
                "psvi2", "pstep2",
                "r3", "x3", "b3", "g3", "pac3", "ratedU3", "rns3", "rsvi3", "rstep3", "pns3",
                "psvi3", "pstep3"));

        Map<String, PropertyBag> powerTransformerRatioTapChanger = new HashMap<>();
        Map<String, PropertyBag> powerTransformerPhaseTapChanger = new HashMap<>();
        cgmes.ratioTapChangers().forEach(ratio -> {
            String id = ratio.getId("RatioTapChanger");
            powerTransformerRatioTapChanger.put(id, ratio);
        });
        cgmes.phaseTapChangers().forEach(phase -> {
            String id = phase.getId("PhaseTapChanger");
            powerTransformerPhaseTapChanger.put(id, phase);
        });

        Map<String, PropertyBag> transformers = new HashMap<>();
        cgmes.groupedTransformerEnds().entrySet().forEach(tends -> {
            String id = tends.getKey();
            PropertyBags ends = tends.getValue();
            ends.forEach(end -> transformerEndParameters(cgmes, equipmentsInNode,
                    powerTransformerRatioTapChanger, powerTransformerPhaseTapChanger, transformers,
                    id, end));
        });
        return transformers;
    }

    private static void transformerEndParameters(CgmesModel cgmes,
            Map<String, List<String>> equipmentsInNode,
            Map<String, PropertyBag> powerTransformerRatioTapChanger,
            Map<String, PropertyBag> powerTransformerPhaseTapChanger,
            Map<String, PropertyBag> transformers, String id, PropertyBag end) {
        String endNumber = end.get("endNumber");
        String r = end.get("r");
        String x = end.get("x");
        String b = end.get("b");
        String g = end.get("g");
        String phaseAngleClock = end.get("phaseAngleClock");
        String ratedU = end.get("ratedU");
        String rtc = end.getId("RatioTapChanger");
        PropertyBag rt = powerTransformerRatioTapChanger.get(rtc);
        String ptc = end.getId("PhaseTapChanger");
        PropertyBag pt = powerTransformerPhaseTapChanger.get(ptc);

        PropertyBag transformer = transformers.computeIfAbsent(id,
            z -> new PropertyBag(propertyNames));
        transformer.put("r" + endNumber, r);
        transformer.put("x" + endNumber, x);
        transformer.put("b" + endNumber, b);
        transformer.put("g" + endNumber, g);
        transformer.put("pac" + endNumber, phaseAngleClock);
        transformer.put("ratedU" + endNumber, ratedU);
        transformer.put("g" + endNumber, g);
        if (rt != null) {
            String ratioNeutralStep = rt.get("neutralStep");
            String ratioStepVoltageIncrement = rt.get("stepVoltageIncrement");
            String ratioStep = rt.get("SVtapStep");
            if (ratioNeutralStep != null) {
                transformer.put("rns" + endNumber, ratioNeutralStep);
            }
            if (ratioStepVoltageIncrement != null) {
                transformer.put("rsvi" + endNumber, ratioStepVoltageIncrement);
            }
            if (ratioStep != null) {
                transformer.put("rstep" + endNumber, ratioStep);
            }
        }
        if (pt != null) {
            String phaseNeutralStep = pt.get("neutralStep");
            String phaseStepVoltageIncrement = pt.get("voltageStepIncrement");
            String phaseStep = pt.get("SVtapStep");
            String phaseWindingConnectionAngle = pt.get("windingConnectionAngle");
            String ptcType = pt.getLocal("phaseTapChangerType").toLowerCase();
            if (phaseNeutralStep != null) {
                transformer.put("pns" + endNumber, phaseNeutralStep);
            }
            if (phaseStepVoltageIncrement != null) {
                transformer.put("psvi" + endNumber, phaseStepVoltageIncrement);
            }
            if (phaseStep != null) {
                transformer.put("pstep" + endNumber, phaseStep);
            }
            if (phaseWindingConnectionAngle != null) {
                transformer.put("pwca" + endNumber, phaseWindingConnectionAngle);
            }
            if (ptcType != null) {
                transformer.put("ptype" + endNumber, ptcType);
            }
        }

        CgmesTerminal t = cgmes.terminal(end.getId(CgmesNames.TERMINAL));
        String nodeId = t.topologicalNode();
        transformer.put("terminal" + endNumber, nodeId);
        transformer.put("connected"  + endNumber, Boolean.toString(t.connected()));
        List<String> idTransformers = equipmentsInNode.computeIfAbsent(nodeId,
            z -> new ArrayList<>());
        idTransformers.add(id);
    }

    private static int                       show = 5;
    private static List<String>              propertyNames;
    private static Map<String, PropertyBag>  voltages;
    private static List<List<String>>        joinedNodes;
    private static Map<String, PropertyBag>  nodeParameters;
    private static Map<String, PropertyBag>  lineParameters;
    private static Map<String, PropertyBag>  transformerParameters;
    private static Map<String, List<String>> equipmentsInNode;

    private static CgmesConformity1Catalog   catalog;
    private static final Logger              LOG  = LoggerFactory
            .getLogger(CgmesFlowValidation.class);
}
