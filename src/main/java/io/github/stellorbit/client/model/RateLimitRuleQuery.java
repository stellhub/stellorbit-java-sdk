package io.github.stellorbit.client.model;

import java.util.Map;
import java.util.Objects;

public record RateLimitRuleQuery(String serviceName, String quotaKey, RequestContext context) {

    public RateLimitRuleQuery {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        context = context == null ? RequestContext.empty() : context;
    }

    /**
     * 返回用于匹配治理规则 conditions 的属性。
     */
    public Map<String, String> attributes() {
        Map<String, String> attributes = new java.util.LinkedHashMap<>(RuleQueryAttributes.common(serviceName, context));
        RuleQueryAttributes.putIfPresent(attributes, "quotaKey", resolvedQuotaKey());
        return Map.copyOf(attributes);
    }

    private String resolvedQuotaKey() {
        if (quotaKey != null && !quotaKey.isBlank()) {
            return quotaKey;
        }
        if (context.quotaKey() != null && !context.quotaKey().isBlank()) {
            return context.quotaKey();
        }
        return context.tenantId();
    }
}
