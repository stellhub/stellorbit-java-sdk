package io.github.stellorbit.client.rule;

import java.util.Locale;

public enum GovernanceRuleStatus {
    DRAFT,
    ACTIVE,
    DISABLED;

    /**
     * 解析治理规则状态。
     */
    public static GovernanceRuleStatus parse(String value) {
        if (value == null || value.isBlank()) {
            return DRAFT;
        }
        return GovernanceRuleStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
