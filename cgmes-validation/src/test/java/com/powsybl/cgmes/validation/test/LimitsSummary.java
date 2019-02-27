package com.powsybl.cgmes.validation.test;

import java.io.PrintStream;
import java.util.Collections;
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

public class LimitsSummary {

    public LimitsSummary() {
        // For dry runs
        forTerminals = Collections.emptyList();
        forEquipment = Collections.emptyList();
    }

    public LimitsSummary(CgmesModel cgmes) {
        PropertyBags limits = cgmes.operationalLimits();
        forTerminals = summary(() -> limits.stream().filter(l -> l.containsKey("Terminal")), cgmes);
        forEquipment = summary(() -> limits.stream().filter(l -> l.containsKey("Equipment")), cgmes);
    }

    public List<Limits> forTerminals() {
        return forTerminals;
    }

    public List<Limits> forEquipment() {
        return forEquipment;
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
                .collect(Collectors.toSet());
        return equipmentClasses.stream()
                .map(equipmentClass -> {
                    Supplier<Stream<PropertyBag>> classLimits = () -> limits.get()
                            .filter(l -> equipmentClass(l, cgmes).equals(equipmentClass));
                    Map<LimitType, Long> types = classLimits.get()
                            .map(l -> new LimitType(l))
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                    return new Limits(equipmentClass, classLimits.get().count(), types);
                }).collect(Collectors.toList());
    }

    private static String equipmentClass(PropertyBag limit, CgmesModel cgmes) {
        if (limit.containsKey("Terminal")) {
            return cgmes.terminal(limit.getId("Terminal")).conductingEquipmentType();
        } else {
            return limit.getLocal("EquipmentClass");
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
}
