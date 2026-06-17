package io.github.stellorbit.client.rule;

import java.util.Locale;

public enum GovernanceRuleType {
    ROUTE,
    RATE_LIMIT,
    CIRCUIT_BREAKER,
    AUTH,
    DEGRADE;

    /**
     * 解析治理规则类型。
     */
    public static GovernanceRuleType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ruleType must not be blank");
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "AUTHORIZATION", "AUTHENTICATION", "ACCESS_CONTROL" -> AUTH;
            default -> GovernanceRuleType.valueOf(normalized);
        };
    }
}
