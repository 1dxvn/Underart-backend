package com.underart.infrastructure.web;

import com.underart.domain.exception.BusinessRuleException;
import java.util.Locale;
import java.util.Map;

public record SortParam<E extends Enum<E>>(E field, boolean ascending) {

    public static <E extends Enum<E>> SortParam<E> parse(String raw, Map<String, E> allowed, E defaultField,
                                                         boolean defaultAscending) {
        if (raw == null || raw.isBlank()) {
            return new SortParam<>(defaultField, defaultAscending);
        }
        String[] parts = raw.split(",", 2);
        E field = allowed.get(parts[0].trim());
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
        if (field == null || !direction.equals("asc") && !direction.equals("desc")) {
            throw new BusinessRuleException("Invalid sort: '" + raw + "'. Use one of " + allowed.keySet()
                    + " followed by ,asc or ,desc");
        }
        return new SortParam<>(field, direction.equals("asc"));
    }
}
