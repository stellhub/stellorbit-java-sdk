package io.github.stellorbit.client.rule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.stellnula.config.StellnulaConfigEntry;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public final class GovernanceRuleParser {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public GovernanceRuleParser() {
        this(new ObjectMapper());
    }

    public GovernanceRuleParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * 解析 StellNula 配置项为治理规则。
     */
    public GovernanceRule parse(StellnulaConfigEntry entry, String checksum) {
        Objects.requireNonNull(entry, "entry must not be null");
        try {
            JsonNode root = objectMapper.readTree(entry.configValue());
            GovernanceRuleType ruleType = GovernanceRuleType.parse(requiredText(root, "ruleType"));
            GovernanceRuleStatus status = GovernanceRuleStatus.parse(text(root, "status", "DRAFT"));
            String targetService = requiredText(root, "targetService");
            int priority = root.path("priority").asInt(-1);
            if (priority < 0) {
                throw new IllegalArgumentException("governance rule priority must be greater than or equal to 0");
            }
            requireRulePayload(ruleType, root);
            Map<String, Object> content = objectMapper.convertValue(root, MAP_TYPE);
            return new GovernanceRule(
                    entry.configId(),
                    text(root, "ruleName", entry.configKey()),
                    entry.configKey(),
                    ruleType,
                    targetService,
                    status,
                    priority,
                    entry.revision(),
                    checksum,
                    entry.configValue(),
                    content);
        } catch (IOException ex) {
            throw new IllegalArgumentException("governance rule content must be valid JSON: " + entry.configId(), ex);
        }
    }

    private void requireRulePayload(GovernanceRuleType ruleType, JsonNode root) {
        switch (ruleType) {
            case ROUTE -> requireNode(root, "routes");
            case RATE_LIMIT -> requireNode(root, "limit");
            case CIRCUIT_BREAKER -> requireNode(root, "breaker");
            case AUTH -> requireNode(root, "auth");
            case DEGRADE -> requireNode(root, "degrade");
        }
    }

    private String requiredText(JsonNode root, String fieldName) {
        String value = text(root, fieldName, "");
        if (value.isBlank()) {
            throw new IllegalArgumentException("governance rule " + fieldName + " must not be blank");
        }
        return value;
    }

    private String text(JsonNode root, String fieldName, String defaultValue) {
        JsonNode node = root.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        String value = node.asText(defaultValue);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private void requireNode(JsonNode root, String fieldName) {
        if (root.path(fieldName).isMissingNode() || root.path(fieldName).isNull()) {
            throw new IllegalArgumentException("governance rule " + fieldName + " must be provided");
        }
    }
}
