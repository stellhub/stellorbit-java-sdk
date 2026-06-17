package io.github.stellorbit.client.model;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record AuthorizationRuleQuery(
        String serviceName,
        String principal,
        String tenantId,
        Set<String> roles,
        String token,
        RequestContext context) {

    public AuthorizationRuleQuery {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        context = context == null ? RequestContext.empty() : context;
    }

    /**
     * 返回用于匹配治理规则 conditions 的属性。
     */
    public Map<String, String> attributes() {
        Map<String, String> attributes = new java.util.LinkedHashMap<>(RuleQueryAttributes.common(serviceName, context));
        RuleQueryAttributes.putIfPresent(attributes, "principal", principal);
        RuleQueryAttributes.putIfPresent(attributes, "tenantId", resolvedTenantId());
        RuleQueryAttributes.putIfPresent(attributes, "token", token);
        if (!roles.isEmpty()) {
            attributes.put("roles", String.join(",", roles));
        }
        return Map.copyOf(attributes);
    }

    private String resolvedTenantId() {
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        return context.tenantId();
    }
}
