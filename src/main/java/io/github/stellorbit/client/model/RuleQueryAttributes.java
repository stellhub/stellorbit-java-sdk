package io.github.stellorbit.client.model;

import java.util.LinkedHashMap;
import java.util.Map;

final class RuleQueryAttributes {

    private RuleQueryAttributes() {
    }

    static Map<String, String> common(String serviceName, RequestContext context) {
        RequestContext resolved = context == null ? RequestContext.empty() : context;
        Map<String, String> attributes = new LinkedHashMap<>(resolved.asAttributes());
        putIfPresent(attributes, "serviceName", serviceName);
        return attributes;
    }

    static void putIfPresent(Map<String, String> values, String key, String value) {
        if (value != null && !value.isBlank()) {
            values.put(key, value);
        }
    }
}
