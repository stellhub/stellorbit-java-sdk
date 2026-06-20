package io.github.stellorbit.client.rule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.stellnula.config.StellnulaConfigEntry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public final class GovernanceRuleParser {

    private static final String AGGREGATE_SCHEMA_VERSION = "stellorbit.governance.aggregate.v1";
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
        List<GovernanceRule> rules = parseAll(entry, checksum);
        if (rules.size() != 1) {
            throw new IllegalArgumentException(
                    "governance config " + entry.configId() + " must contain exactly one rule");
        }
        return rules.getFirst();
    }

    /**
     * 解析 StellNula 配置项为一个或多个治理规则。
     */
    public List<GovernanceRule> parseAll(StellnulaConfigEntry entry, String checksum) {
        Objects.requireNonNull(entry, "entry must not be null");
        try {
            JsonNode root = objectMapper.readTree(entry.configValue());
            validateAggregatePayload(entry, root);
            return parseAggregate(entry, checksum, root);
        } catch (IOException ex) {
            throw new IllegalArgumentException("governance rule content must be valid JSON: " + entry.configId(), ex);
        }
    }

    private List<GovernanceRule> parseAggregate(StellnulaConfigEntry entry, String checksum, JsonNode root)
            throws IOException {
        JsonNode rulesNode = root.path("rules");
        if (!rulesNode.isArray()) {
            throw new IllegalArgumentException("aggregated governance config rules must be an array: "
                    + entry.configId());
        }
        List<GovernanceRule> rules = new ArrayList<>();
        int index = 0;
        for (JsonNode ruleNode : rulesNode) {
            rules.add(parseAggregatedRule(entry, checksum, root, ruleNode, index));
            index++;
        }
        return List.copyOf(rules);
    }

    private void validateAggregatePayload(StellnulaConfigEntry entry, JsonNode root) {
        String schemaVersion = requiredText(root, "schemaVersion");
        if (!AGGREGATE_SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("governance config " + entry.configId()
                    + " must use aggregate schema version " + AGGREGATE_SCHEMA_VERSION);
        }
        String payloadConfigId = requiredText(root, "configId");
        if (!entry.configId().equals(payloadConfigId)) {
            throw new IllegalArgumentException("governance config id mismatch: entry="
                    + entry.configId() + ", payload=" + payloadConfigId);
        }
        GovernanceRuleType ruleType = GovernanceRuleType.parse(requiredText(root, "ruleType"));
        requireRulePayload(ruleType, root);
    }

    private GovernanceRule parseAggregatedRule(
            StellnulaConfigEntry entry,
            String checksum,
            JsonNode root,
            JsonNode ruleNode,
            int index)
            throws IOException {
        JsonNode contentNode = ruleNode.path("content");
        if (!contentNode.isObject()) {
            throw new IllegalArgumentException("aggregated governance rule content must be an object: "
                    + entry.configId() + "#" + index);
        }
        String ruleConfigId = text(ruleNode, "configId", entry.configId());
        if (!entry.configId().equals(ruleConfigId)) {
            throw new IllegalArgumentException("aggregated governance rule config id mismatch: entry="
                    + entry.configId() + ", rule=" + ruleConfigId);
        }
        String ruleId = requiredText(ruleNode, "ruleId");
        String stellnulaRuleType = text(ruleNode, "stellnulaRuleType", text(root, "ruleType", ""));
        String targetService = text(ruleNode, "targetService", text(contentNode, "targetService", text(root, "targetService", "")));
        String status = text(ruleNode, "status", text(contentNode, "status", text(root, "status", "DRAFT")));
        int priority = intValue(ruleNode, "priority", intValue(contentNode, "priority", intValue(root, "priority", -1)));

        Map<String, Object> content = new LinkedHashMap<>(objectMapper.convertValue(contentNode, MAP_TYPE));
        putIfPresent(content, "ruleType", stellnulaRuleType);
        putIfPresent(content, "sourceRuleType", text(ruleNode, "ruleType", text(root, "sourceRuleType", "")));
        putIfPresent(content, "targetService", targetService);
        putIfPresent(content, "status", status);
        if (priority >= 0) {
            content.put("priority", priority);
        }
        putIfPresent(content, "ruleCode", text(ruleNode, "ruleCode", ""));
        putIfPresent(content, "schemaVersion", text(ruleNode, "schemaVersion", ""));
        putIfPresent(content, "aggregateConfigId", entry.configId());
        putIfPresent(content, "aggregateChecksum", text(root, "checksum", checksum));

        JsonNode merged = objectMapper.valueToTree(content);
        return parseRuleContent(
                ruleId,
                text(ruleNode, "ruleName", text(ruleNode, "ruleCode", ruleId)),
                entry.configId(),
                merged,
                entry.revision(),
                text(ruleNode, "checksum", text(root, "checksum", checksum)),
                objectMapper.writeValueAsString(content));
    }

    private GovernanceRule parseRuleContent(
            String ruleId,
            String ruleName,
            String configKey,
            JsonNode root,
            long revision,
            String checksum,
            String rawContent) {
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
                ruleId,
                ruleName,
                configKey,
                ruleType,
                targetService,
                status,
                priority,
                revision,
                checksum,
                rawContent,
                content);
    }

    private void requireRulePayload(GovernanceRuleType ruleType, JsonNode root) {
        switch (ruleType) {
            case ROUTE -> requireNode(root, "routes");
            case RATE_LIMIT -> {
                requireNode(root, "limit");
                validateRateLimitPayload(root);
            }
            case CIRCUIT_BREAKER -> requireNode(root, "breaker");
            case AUTH -> requireNode(root, "auth");
            case DEGRADE -> requireNode(root, "degrade");
        }
    }

    private void validateRateLimitPayload(JsonNode root) {
        validateSupported(root, "limitMode", RateLimitRules::supportsLimitMode);
        validateSupported(root, "limitType", RateLimitRules::supportsLimitType);
        validateSupported(root, "limitAlgorithm", RateLimitRules::supportsLimitAlgorithm);
        validateSupported(root, "trafficProtocol", RateLimitRules::supportsTrafficProtocol);
        validateSupported(root, "executionLocation", RateLimitRules::supportsExecutionLocation);
        validateSupported(root, "coordinationMode", RateLimitRules::supportsCoordinationMode);
        validateSupported(root, "enforcementMode", RateLimitRules::supportsEnforcementMode);
        validateNestedSupported(root, "limit", "algorithm", "limit.algorithm", RateLimitRules::supportsLimitAlgorithm);
        validateNestedSupported(
                root, "rateLimit", "algorithm", "rateLimit.algorithm", RateLimitRules::supportsLimitAlgorithm);
    }

    private void validateSupported(JsonNode root, String fieldName, Predicate<String> supported) {
        JsonNode node = root.path(fieldName);
        if (!node.isMissingNode() && !node.isNull()) {
            if (!node.isValueNode()) {
                throw new IllegalArgumentException("unsupported rate limit " + fieldName + ": " + node);
            }
            String value = node.asText("");
            if (!supported.test(value)) {
                throw new IllegalArgumentException("unsupported rate limit " + fieldName + ": " + value);
            }
        }
    }

    private void validateNestedSupported(
            JsonNode root, String parentName, String fieldName, String displayName, Predicate<String> supported) {
        JsonNode parent = root.path(parentName);
        if (!parent.isObject()) {
            return;
        }
        JsonNode node = parent.path(fieldName);
        if (!node.isMissingNode() && !node.isNull()) {
            if (!node.isValueNode()) {
                throw new IllegalArgumentException("unsupported rate limit " + displayName + ": " + node);
            }
            String value = node.asText("");
            if (!supported.test(value)) {
                throw new IllegalArgumentException("unsupported rate limit " + displayName + ": " + value);
            }
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

    private int intValue(JsonNode root, String fieldName, int defaultValue) {
        JsonNode node = root.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        return node.asInt(defaultValue);
    }

    private void putIfPresent(Map<String, Object> values, String key, String value) {
        if (value != null && !value.isBlank()) {
            values.put(key, value);
        }
    }
}
