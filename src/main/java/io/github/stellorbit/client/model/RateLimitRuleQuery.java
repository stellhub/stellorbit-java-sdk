package io.github.stellorbit.client.model;

import java.util.Map;
import java.util.Objects;

public record RateLimitRuleQuery(
        String serviceName,
        String quotaKey,
        RequestContext context,
        String limitMode,
        String limitType,
        String trafficProtocol,
        String executionLocation,
        String coordinationMode,
        String keyExtractorSource) {

    public RateLimitRuleQuery(String serviceName, String quotaKey, RequestContext context) {
        this(serviceName, quotaKey, context, null, null, null, null, null, null);
    }

    public RateLimitRuleQuery {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        context = context == null ? RequestContext.empty() : context;
        limitMode = blankToNull(limitMode);
        limitType = blankToNull(limitType);
        trafficProtocol = blankToNull(trafficProtocol);
        executionLocation = blankToNull(executionLocation);
        coordinationMode = blankToNull(coordinationMode);
        keyExtractorSource = blankToNull(keyExtractorSource);
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

    /**
     * 返回携带限流模式过滤条件的新查询。
     */
    public RateLimitRuleQuery withLimitMode(String nextLimitMode) {
        return new RateLimitRuleQuery(
                serviceName,
                quotaKey,
                context,
                nextLimitMode,
                limitType,
                trafficProtocol,
                executionLocation,
                coordinationMode,
                keyExtractorSource);
    }

    /**
     * 返回携带限流对象类型过滤条件的新查询。
     */
    public RateLimitRuleQuery withLimitType(String nextLimitType) {
        return new RateLimitRuleQuery(
                serviceName,
                quotaKey,
                context,
                limitMode,
                nextLimitType,
                trafficProtocol,
                executionLocation,
                coordinationMode,
                keyExtractorSource);
    }

    /**
     * 返回携带流量协议过滤条件的新查询。
     */
    public RateLimitRuleQuery withTrafficProtocol(String nextTrafficProtocol) {
        return new RateLimitRuleQuery(
                serviceName,
                quotaKey,
                context,
                limitMode,
                limitType,
                nextTrafficProtocol,
                executionLocation,
                coordinationMode,
                keyExtractorSource);
    }

    /**
     * 返回携带执行位置过滤条件的新查询。
     */
    public RateLimitRuleQuery withExecutionLocation(String nextExecutionLocation) {
        return new RateLimitRuleQuery(
                serviceName,
                quotaKey,
                context,
                limitMode,
                limitType,
                trafficProtocol,
                nextExecutionLocation,
                coordinationMode,
                keyExtractorSource);
    }

    /**
     * 返回携带协调模式过滤条件的新查询。
     */
    public RateLimitRuleQuery withCoordinationMode(String nextCoordinationMode) {
        return new RateLimitRuleQuery(
                serviceName,
                quotaKey,
                context,
                limitMode,
                limitType,
                trafficProtocol,
                executionLocation,
                nextCoordinationMode,
                keyExtractorSource);
    }

    /**
     * 返回携带 key 提取来源过滤条件的新查询。
     */
    public RateLimitRuleQuery withKeyExtractorSource(String nextKeyExtractorSource) {
        return new RateLimitRuleQuery(
                serviceName,
                quotaKey,
                context,
                limitMode,
                limitType,
                trafficProtocol,
                executionLocation,
                coordinationMode,
                nextKeyExtractorSource);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
