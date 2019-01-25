package com.powsybl.cgmes.validation.test;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;

public class LimitsSummary {

    public LimitsSummary(CgmesModel cgmes) {
        this.cgmes = cgmes;
    }

    public void report() {
        PropertyBags limits = cgmes.operationalLimits();
        System.out.println("Summary limits");
        Supplier<Stream<PropertyBag>> forTerminals = () -> limits.stream().filter(l -> l.containsKey("Terminal"));
        Supplier<Stream<PropertyBag>> forEquipment = () -> limits.stream().filter(l -> l.containsKey("Equipment"));
        System.out.println("    for Terminals : ");
        limitsSummary(forTerminals, cgmes);
        System.out.println("    for Equipment : ");
        limitsSummary(forEquipment, cgmes);
    }

    private static void limitsSummary(Supplier<Stream<PropertyBag>> limits, CgmesModel cgmes) {
        Set<String> equipmentClasses = limits.get()
                .map(l -> eqclass(l, cgmes))
                .collect(Collectors.toSet());
        equipmentClasses.forEach(eqclass -> {
            Supplier<Stream<PropertyBag>> eqlimits = () -> limits.get()
                    .filter(l -> eqclass(l, cgmes).equals(eqclass));
            Set<LimitType> ltypes = eqlimits.get()
                    .map(l -> new LimitType(l))
                    .collect(Collectors.toSet());
            System.out.printf("        %-32s %5d %s%n", eqclass, eqlimits.get().count(), ltypes);
        });
    }

    private static String eqclass(PropertyBag limit, CgmesModel cgmes) {
        if (limit.containsKey("Terminal")) {
            return cgmes.terminal(limit.getId("Terminal")).conductingEquipmentType();
        } else {
            return limit.getLocal("EquipmentClass");
        }
    }

    private static class LimitType {
        LimitType(PropertyBag l) {
            type = l.getLocal("limitType");
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

        final String type;
        final String name;
        final String subclass;
    }

    private final CgmesModel cgmes;
}
