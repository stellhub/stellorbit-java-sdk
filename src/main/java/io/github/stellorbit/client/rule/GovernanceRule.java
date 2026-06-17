package io.github.stellorbit.client.rule;

import java.util.Map;
import java.util.Objects;

public record GovernanceRule(
        String ruleId,
        String ruleName,
        String configKey,
        GovernanceRuleType ruleType,
        String targetService,
        GovernanceRuleStatus status,
        int priority,
        long revision,
        String checksum,
        String rawContent,
        Map<String, Object> content) {

    public GovernanceRule {
        ruleId = requireText(ruleId, "ruleId");
        ruleName = defaultText(ruleName, ruleId);
        configKey = defaultText(configKey, ruleId);
        ruleType = Objects.requireNonNull(ruleType, "ruleType must not be null");
        targetService = requireText(targetService, "targetService");
        status = status == null ? GovernanceRuleStatus.DRAFT : status;
        if (priority < 0) {
            throw new IllegalArgumentException("priority must be greater than or equal to 0");
        }
        checksum = checksum == null ? "" : checksum;
        rawContent = rawContent == null ? "" : rawContent;
        content = content == null ? Map.of() : Map.copyOf(content);
    }

    /**
     * 判断规则是否处于启用状态。
     */
    public boolean active() {
        return status == GovernanceRuleStatus.ACTIVE;
    }

    /**
     * 判断规则是否匹配目标服务。
     */
    public boolean matchesService(String serviceName) {
        return "*".equals(targetService) || targetService.equals(serviceName);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
