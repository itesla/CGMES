/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.validation.test;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class LimitsSummary {

    public LimitsSummary() {
        // For dry runs
        forTerminals = Collections.emptyList();
        forEquipment = Collections.emptyList();
        limitTypesDifferentSubclass = Collections.emptyMap();
        limitTypesDifferentSubclassCount = Collections.emptyMap();
        eqClassNumObjects = Collections.emptyMap();
        countsByEqClassAndLimitType = Collections.emptyMap();
        countsByEqClassAndLimitTypeAndTerminal = Collections.emptyMap();
    }

    // We want to know:
    // Which models use limits defined using Apparent Power values.
    // How many equipment receive these limits
    // (as a % of the total # of equipment in the class).
    // How many equipment have both Apparent Power and Current limit values for the
    // same type of limit (say PATL given as both an Apparent Power and a Current)
    public LimitsSummary(CgmesModel cgmes) {
        PropertyBags limits = cgmes.operationalLimits();
        forTerminals = summary(() -> limits.stream().filter(l -> l.containsKey("Terminal")), cgmes);
        forEquipment = summary(() -> limits.stream().filter(l -> l.containsKey("Equipment")), cgmes);

        limitTypesDifferentSubclass = new HashMap<>();
        limitTypesDifferentSubclassCount = new HashMap<>();
        // Build a map with all found limit types for each equipment id
        Map<String, Set<LimitType>> els = limits.stream()
            .collect(groupingBy(l -> equipmentId(l, cgmes),
                mapping(LimitType::new, toSet())));
        // Select the equipment that has multiple subclasses for the same limit type
        els.forEach((id, ls) -> {
            if (ls.size() > 1) {
                Map<String, Set<String>> lts = ls.stream()
                    .collect(groupingBy(
                        lt -> lt.type,
                        mapping(lt -> lt.subclass, toSet())));
                long numSameTypeDiffSubclass = lts.values().stream().filter(lt -> lt.size() > 1).count();
                if (numSameTypeDiffSubclass > 0) {
                    // Save an example for each type with different subclass found
                    limitTypesDifferentSubclass.put(lts, id);
                    limitTypesDifferentSubclassCount.merge(lts, 1, Integer::sum);
                }
            }
        });

        // Group by Equipment Class, then by LimitType,
        // count unique EquipmentId
        countsByEqClassAndLimitType = limits.stream()
            .collect(
                groupingBy(
                    l -> equipmentClass(l, cgmes),
                    groupingBy(LimitType::new,
                        Collectors.collectingAndThen(
                            Collectors.mapping(l -> equipmentId(l, cgmes), Collectors.toSet()),
                            Set::size))));

        countsByEqClassAndLimitTypeAndTerminal = limits.stream()
            .collect(
                groupingBy(
                    l -> equipmentClass(l, cgmes),
                    groupingBy(LimitType::new,
                        groupingBy(l -> l.containsKey("Terminal") ? "Terminal" : "Equipment",
                            Collectors.collectingAndThen(
                                Collectors.mapping(l -> equipmentId(l, cgmes), Collectors.toSet()),
                                Set::size)))));

        eqClassNumObjects = cgmes.numObjectsByType().stream()
            .collect(toMap(
                p -> p.getLocal("Type"),
                p -> p.asInt("numObjects")));
    }

    public List<Limits> forTerminals() {
        return forTerminals;
    }

    public List<Limits> forEquipment() {
        return forEquipment;
    }

    public Map<String, Integer> eqClassNumObjects() {
        return eqClassNumObjects;
    }

    public boolean hasEquipmentWithSameLimitTypeAndDifferentSubclasses() {
        return !limitTypesDifferentSubclass.isEmpty();
    }

    public Set<Map<String, Set<String>>> sameLimitTypeAndDifferentSubclasses() {
        return limitTypesDifferentSubclass.keySet();
    }

    public String sameLimitTypeAndDifferentSubclassesSample(Map<String, Set<String>> ls) {
        return limitTypesDifferentSubclass.get(ls);
    }

    public int sameLimitTypeAndDifferentSubclassesCount(Map<String, Set<String>> ls) {
        return limitTypesDifferentSubclassCount.get(ls);
    }

    public Map<String, Map<LimitType, Integer>> countsByEqClassAndLimitType() {
        return countsByEqClassAndLimitType;
    }

    public Map<String, Map<LimitType, Map<String, Integer>>> countsByEqClassAndLimitTypeAndTerminal() {
        return countsByEqClassAndLimitTypeAndTerminal;
    }

    public void report(PrintStream p) {
        p.println("Summary limits");
        p.println("    for Terminals : ");
        report(forTerminals, p);
        p.println("    for Equipment : ");
        report(forEquipment, p);
    }

    private void report(List<Limits> ls, PrintStream p) {
        ls.forEach(l -> {
            p.printf("        %5d %s%n", l.num, l.equipmentClass);
            l.types.forEach((t, num) -> {
                p.printf("            %5d %s%n", num, t);
            });
        });
    }

    private List<Limits> summary(Supplier<Stream<PropertyBag>> limits, CgmesModel cgmes) {
        Set<String> equipmentClasses = limits.get()
            .map(l -> equipmentClass(l, cgmes))
            .collect(toSet());
        return equipmentClasses.stream()
            .map(equipmentClass -> {
                Supplier<Stream<PropertyBag>> classLimits = () -> limits.get()
                    .filter(l -> equipmentClass(l, cgmes).equals(equipmentClass));
                Map<LimitType, Long> types = classLimits.get()
                    .map(l -> new LimitType(l))
                    .collect(groupingBy(Function.identity(), counting()));
                return new Limits(equipmentClass, classLimits.get().count(), types);
            }).collect(toList());
    }

    private static String equipmentClass(PropertyBag limit, CgmesModel cgmes) {
        if (limit.containsKey("Terminal")) {
            return cgmes.terminal(limit.getId("Terminal")).conductingEquipmentType();
        } else {
            return limit.getLocal("EquipmentClass");
        }
    }

    private String equipmentId(PropertyBag limit, CgmesModel cgmes) {
        if (limit.containsKey("Terminal")) {
            return cgmes.terminal(limit.getId("Terminal")).conductingEquipment();
        } else {
            return limit.getLocal("Equipment");
        }
    }

    public static class Limits {
        Limits(String equipmentClass, long num, Map<LimitType, Long> types) {
            this.equipmentClass = equipmentClass;
            this.num = num;
            this.types = types;
        }

        public final String equipmentClass;
        public final long num;
        public final Map<LimitType, Long> types;
    }

    public static class LimitType {
        LimitType(PropertyBag l) {
            type = l.getLocal("limitType").replace("LimitTypeKind.", "");
            name = l.getLocal("operationalLimitTypeName");
            subclass = l.getLocal("OperationalLimitSubclass");
        }

        @Override
        public String toString() {
            return String.format("(%s, %s, %s)", type, name, subclass);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof LimitType)) {
                return false;
            }
            LimitType other = (LimitType) o;
            return type.equals(other.type)
                && name.equals(other.name)
                && subclass.equals(other.subclass);
        }

        @Override
        public final int hashCode() {
            return Objects.hash(type, name, subclass);
        }

        public final String type;
        public final String name;
        public final String subclass;
    }

    private final List<Limits> forTerminals;
    private final List<Limits> forEquipment;
    private final Map<Map<String, Set<String>>, String> limitTypesDifferentSubclass;
    private final Map<Map<String, Set<String>>, Integer> limitTypesDifferentSubclassCount;
    private final Map<String, Integer> eqClassNumObjects;
    private final Map<String, Map<LimitType, Integer>> countsByEqClassAndLimitType;
    private final Map<String, Map<LimitType, Map<String, Integer>>> countsByEqClassAndLimitTypeAndTerminal;
}
