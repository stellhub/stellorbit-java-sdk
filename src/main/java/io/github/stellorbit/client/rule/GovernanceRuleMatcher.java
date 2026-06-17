package io.github.stellorbit.client.rule;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class GovernanceRuleMatcher {

    /**
     * 判断规则是否匹配查询属性。
     */
    public boolean matches(GovernanceRule rule, Map<String, String> attributes) {
        if (rule == null) {
            return false;
        }
        return conditionsMatch(rule.content(), attributes == null ? Map.of() : attributes);
    }

    /**
     * 判断对象内的 conditions 是否匹配查询属性。
     */
    public boolean conditionsMatch(Map<String, Object> values, Map<String, String> attributes) {
        Map<String, Object> conditions = object(values, "conditions");
        if (conditions.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            if (!conditionMatches(attributes.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> object(Map<String, Object> values, String key) {
        if (values == null) {
            return Map.of();
        }
        Object value = values.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((entryKey, entryValue) -> result.put(String.valueOf(entryKey), entryValue));
            return Map.copyOf(result);
        }
        return Map.of();
    }

    private boolean conditionMatches(String actual, Object expected) {
        if (expected instanceof Map<?, ?> map) {
            return mapConditionMatches(actual, map);
        }
        if (expected instanceof Collection<?> collection) {
            Set<String> actualValues = actualValues(actual);
            return !actualValues.isEmpty()
                    && collection.stream().map(String::valueOf).anyMatch(actualValues::contains);
        }
        return actualValues(actual).contains(String.valueOf(expected));
    }

    private boolean mapConditionMatches(String actual, Map<?, ?> condition) {
        Set<String> actualValues = actualValues(actual);
        if (condition.containsKey("exists")) {
            boolean exists = Boolean.parseBoolean(String.valueOf(condition.get("exists")));
            return exists == !actualValues.isEmpty();
        }
        if (condition.containsKey("equals")) {
            return actualValues.contains(String.valueOf(condition.get("equals")));
        }
        if (condition.containsKey("notEquals")) {
            return !actualValues.contains(String.valueOf(condition.get("notEquals")));
        }
        if (condition.containsKey("in")) {
            Object expected = condition.get("in");
            if (expected instanceof Collection<?> collection) {
                return !actualValues.isEmpty()
                        && collection.stream().map(String::valueOf).anyMatch(actualValues::contains);
            }
        }
        return false;
    }

    private Set<String> actualValues(String actual) {
        if (actual == null || actual.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(actual.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
