package io.github.stellorbit.client.model;

import java.util.Map;
import java.util.Objects;

public record RouteRuleQuery(
        String serviceName, String routeKey, Map<String, String> attributes, RequestContext context) {

    public RouteRuleQuery {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        context = context == null ? RequestContext.empty() : context;
    }

    /**
     * 返回用于匹配治理规则 conditions 的属性。
     */
    public Map<String, String> attributes() {
        Map<String, String> resolved = new java.util.LinkedHashMap<>(attributes);
        resolved.putAll(RuleQueryAttributes.common(serviceName, context));
        RuleQueryAttributes.putIfPresent(resolved, "routeKey", routeKey);
        return Map.copyOf(resolved);
    }
}
