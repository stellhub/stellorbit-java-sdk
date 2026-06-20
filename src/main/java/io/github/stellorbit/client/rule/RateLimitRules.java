package io.github.stellorbit.client.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RateLimitRules {

    public static final String LIMIT_MODE_QPS = "QPS";
    public static final String LIMIT_MODE_HEADER = "HEADER";
    public static final String COORDINATION_MODE_LOCAL_ONLY = "LOCAL_ONLY";
    public static final String COORDINATION_MODE_GLOBAL_SYNC = "GLOBAL_SYNC";
    public static final String COORDINATION_MODE_GLOBAL_QUOTA = "GLOBAL_QUOTA";
    public static final String KEY_EXTRACTOR_SOURCE_HEADER = "HEADER";
    public static final String KEY_EXTRACTOR_SOURCE_GRPC_METADATA = "GRPC_METADATA";

    private static final Set<String> SUPPORTED_LIMIT_MODES = Set.of(
            LIMIT_MODE_QPS,
            "CONCURRENCY",
            LIMIT_MODE_HEADER,
            "HOT_KEY",
            "CUSTOM",
            "QUOTA",
            "BANDWIDTH",
            "CONNECTION",
            "MODEL");
    private static final Set<String> SUPPORTED_LIMIT_TYPES = Set.of(
            "REQUEST",
            "CONNECTION",
            "BYTE",
            "TENANT",
            "USER",
            "CALLER",
            "API_KEY",
            "RESOURCE",
            "HEADER",
            "GRPC_METADATA",
            "IP",
            "ENDPOINT",
            "METHOD",
            "TOPIC",
            "MODEL_REQUEST",
            "MODEL_TOKEN",
            "MODEL_COST",
            "MODEL_CONCURRENCY",
            "CUSTOM_KEY");
    private static final Set<String> SUPPORTED_LIMIT_ALGORITHMS = Set.of(
            "TOKEN_BUCKET",
            "LEAKY_BUCKET",
            "FIXED_WINDOW",
            "SLIDING_WINDOW",
            "QUOTA_LEASE",
            "CONCURRENCY_LIMIT",
            "HOT_KEY",
            "CUSTOM",
            "ADAPTIVE");
    private static final Set<String> SUPPORTED_TRAFFIC_PROTOCOLS =
            Set.of("HTTP", "GRPC", "TCP", "MESSAGE", "MODEL", "ANY");
    private static final Set<String> SUPPORTED_EXECUTION_LOCATIONS =
            Set.of("APPLICATION", "SIDECAR", "GATEWAY", "EDGE");
    private static final Set<String> SUPPORTED_COORDINATION_MODES =
            Set.of(COORDINATION_MODE_LOCAL_ONLY, COORDINATION_MODE_GLOBAL_SYNC, COORDINATION_MODE_GLOBAL_QUOTA);
    private static final Set<String> SUPPORTED_ENFORCEMENT_MODES =
            Set.of("LOCAL", COORDINATION_MODE_GLOBAL_SYNC, COORDINATION_MODE_GLOBAL_QUOTA, "EDGE");

    private RateLimitRules() {
    }

    /**
     * 读取限流模式，旧规则缺失该字段时兼容为 QPS。
     */
    public static String limitMode(GovernanceRule rule) {
        return defaultText(ruleField(rule, "limitMode"), LIMIT_MODE_QPS);
    }

    /**
     * 读取限流对象类型。
     */
    public static String limitType(GovernanceRule rule) {
        return ruleField(rule, "limitType");
    }

    /**
     * 读取限流算法。
     */
    public static String limitAlgorithm(GovernanceRule rule) {
        return normalizeEnum(firstText(
                ruleField(rule, "limitAlgorithm"),
                nestedText(rule, "limit", "algorithm"),
                nestedText(rule, "rateLimit", "algorithm")));
    }

    /**
     * 读取流量协议。
     */
    public static String trafficProtocol(GovernanceRule rule) {
        return ruleField(rule, "trafficProtocol");
    }

    /**
     * 读取执行位置。
     */
    public static String executionLocation(GovernanceRule rule) {
        return ruleField(rule, "executionLocation");
    }

    /**
     * 读取协调模式，并兼容旧 enforcementMode 字段。
     */
    public static String coordinationMode(GovernanceRule rule) {
        String coordinationMode = ruleField(rule, "coordinationMode");
        if (!coordinationMode.isBlank()) {
            return coordinationMode;
        }
        String enforcementMode = ruleField(rule, "enforcementMode");
        if (isDistributedCoordinationMode(enforcementMode)) {
            return normalizeEnum(enforcementMode);
        }
        if ("LOCAL".equals(normalizeEnum(enforcementMode))) {
            return COORDINATION_MODE_LOCAL_ONLY;
        }
        return COORDINATION_MODE_LOCAL_ONLY;
    }

    /**
     * 判断规则是否需要分布式限流链路执行。
     */
    public static boolean isDistributedRule(GovernanceRule rule) {
        return isDistributedCoordinationMode(coordinationMode(rule));
    }

    /**
     * 判断规则是否应由本地运行时执行。
     */
    public static boolean isLocalRuntimeRule(GovernanceRule rule) {
        return enumEquals(coordinationMode(rule), COORDINATION_MODE_LOCAL_ONLY);
    }

    /**
     * 判断规则是否使用 HTTP Header 提取 key。
     */
    public static boolean usesHttpHeaderExtractor(GovernanceRule rule) {
        return usesKeyExtractorSource(rule, KEY_EXTRACTOR_SOURCE_HEADER);
    }

    /**
     * 判断规则是否使用 gRPC Metadata 提取 key。
     */
    public static boolean usesGrpcMetadataExtractor(GovernanceRule rule) {
        return usesKeyExtractorSource(rule, KEY_EXTRACTOR_SOURCE_GRPC_METADATA);
    }

    /**
     * 判断规则是否使用指定来源提取 key。
     */
    public static boolean usesKeyExtractorSource(GovernanceRule rule, String source) {
        String expected = normalizeEnum(source);
        if (expected.isBlank()) {
            return true;
        }
        return keyExtractorSources(rule).stream().anyMatch(value -> enumEquals(value, expected));
    }

    /**
     * 读取 keyExtractor.keys 中声明的来源。
     */
    public static List<String> keyExtractorSources(GovernanceRule rule) {
        Map<String, Object> keyExtractor = object(rule, "keyExtractor");
        if (keyExtractor.isEmpty()) {
            return List.of();
        }
        List<String> sources = new ArrayList<>();
        collectSource(keyExtractor, sources);
        Object keys = keyExtractor.get("keys");
        if (keys instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof Map<?, ?> map) {
                    collectSource(map, sources);
                }
            }
        } else if (keys instanceof Map<?, ?> map) {
            collectSource(map, sources);
        }
        return sources.stream()
                .map(RateLimitRules::normalizeEnum)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    /**
     * 判断两个枚举文本是否一致。
     */
    public static boolean enumEquals(String first, String second) {
        String normalizedFirst = normalizeEnum(first);
        return !normalizedFirst.isBlank() && normalizedFirst.equals(normalizeEnum(second));
    }

    /**
     * 判断限流模式是否受支持。
     */
    public static boolean supportsLimitMode(String value) {
        return supported(SUPPORTED_LIMIT_MODES, value);
    }

    /**
     * 判断限流对象类型是否受支持。
     */
    public static boolean supportsLimitType(String value) {
        return supported(SUPPORTED_LIMIT_TYPES, value);
    }

    /**
     * 判断限流算法是否受支持。
     */
    public static boolean supportsLimitAlgorithm(String value) {
        return supported(SUPPORTED_LIMIT_ALGORITHMS, value);
    }

    /**
     * 判断流量协议是否受支持。
     */
    public static boolean supportsTrafficProtocol(String value) {
        return supported(SUPPORTED_TRAFFIC_PROTOCOLS, value);
    }

    /**
     * 判断执行位置是否受支持。
     */
    public static boolean supportsExecutionLocation(String value) {
        return supported(SUPPORTED_EXECUTION_LOCATIONS, value);
    }

    /**
     * 判断协调模式是否受支持。
     */
    public static boolean supportsCoordinationMode(String value) {
        return supported(SUPPORTED_COORDINATION_MODES, value);
    }

    /**
     * 判断旧执行模式是否受支持。
     */
    public static boolean supportsEnforcementMode(String value) {
        return supported(SUPPORTED_ENFORCEMENT_MODES, value);
    }

    static String normalizeEnum(String value) {
        return value == null ? "" : value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static boolean supported(Set<String> supportedValues, String value) {
        String normalized = normalizeEnum(value);
        return normalized.isBlank() || supportedValues.contains(normalized);
    }

    private static boolean isDistributedCoordinationMode(String value) {
        return enumEquals(value, COORDINATION_MODE_GLOBAL_SYNC)
                || enumEquals(value, COORDINATION_MODE_GLOBAL_QUOTA);
    }

    private static String ruleField(GovernanceRule rule, String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        if (rule == null) {
            return "";
        }
        return normalizeEnum(firstText(
                stringValue(rule.content().get(fieldName)),
                nestedText(rule, "limit", fieldName),
                nestedText(rule, "rateLimit", fieldName)));
    }

    private static String nestedText(GovernanceRule rule, String first, String second) {
        if (rule == null) {
            return "";
        }
        return stringValue(nested(rule.content(), first, second));
    }

    private static Map<String, Object> object(GovernanceRule rule, String fieldName) {
        if (rule == null) {
            return Map.of();
        }
        Object value = rule.content().get(fieldName);
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        return Map.of();
    }

    private static Map<String, Object> copyMap(Map<?, ?> map) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        map.forEach((key, value) -> values.put(String.valueOf(key), value));
        return Map.copyOf(values);
    }

    private static Object nested(Map<String, Object> content, String first, String second) {
        Object value = content.get(first);
        if (value instanceof Map<?, ?> map) {
            return map.get(second);
        }
        return null;
    }

    private static void collectSource(Map<?, ?> value, List<String> sources) {
        String source = stringValue(value.get("source"));
        if (!source.isBlank()) {
            sources.add(source);
        }
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
