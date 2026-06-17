package io.github.stellorbit.client.model;

import java.util.Map;

public record RequestContext(
        String clientId,
        String traceId,
        String spanId,
        String tenantId,
        String quotaKey,
        String authContextId,
        String trafficClass,
        String trafficTag,
        Map<String, String> attributes) {

    public RequestContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * 创建空请求上下文。
     */
    public static RequestContext empty() {
        return builder().build();
    }

    /**
     * 创建请求上下文构造器。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回带标准字段的属性视图。
     */
    public Map<String, String> asAttributes() {
        MapBuilder builder = new MapBuilder(attributes);
        builder.put("clientId", clientId);
        builder.put("traceId", traceId);
        builder.put("spanId", spanId);
        builder.put("tenantId", tenantId);
        builder.put("quotaKey", quotaKey);
        builder.put("authContextId", authContextId);
        builder.put("trafficClass", trafficClass);
        builder.put("trafficTag", trafficTag);
        return builder.build();
    }

    public static final class Builder {

        private String clientId;
        private String traceId;
        private String spanId;
        private String tenantId;
        private String quotaKey;
        private String authContextId;
        private String trafficClass;
        private String trafficTag;
        private Map<String, String> attributes = Map.of();

        private Builder() {
        }

        /**
         * 设置调用方客户端标识。
         */
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * 设置 traceId。
         */
        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        /**
         * 设置 spanId。
         */
        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        /**
         * 设置租户标识。
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * 设置限流 key。
         */
        public Builder quotaKey(String quotaKey) {
            this.quotaKey = quotaKey;
            return this;
        }

        /**
         * 设置鉴权上下文标识。
         */
        public Builder authContextId(String authContextId) {
            this.authContextId = authContextId;
            return this;
        }

        /**
         * 设置流量分类。
         */
        public Builder trafficClass(String trafficClass) {
            this.trafficClass = trafficClass;
            return this;
        }

        /**
         * 设置流量标签。
         */
        public Builder trafficTag(String trafficTag) {
            this.trafficTag = trafficTag;
            return this;
        }

        /**
         * 设置扩展属性。
         */
        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
            return this;
        }

        /**
         * 构建请求上下文。
         */
        public RequestContext build() {
            return new RequestContext(
                    clientId,
                    traceId,
                    spanId,
                    tenantId,
                    quotaKey,
                    authContextId,
                    trafficClass,
                    trafficTag,
                    attributes);
        }
    }

    private static final class MapBuilder {

        private final java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();

        private MapBuilder(Map<String, String> attributes) {
            values.putAll(attributes);
        }

        private void put(String key, String value) {
            if (value != null && !value.isBlank()) {
                values.put(key, value);
            }
        }

        private Map<String, String> build() {
            return Map.copyOf(values);
        }
    }
}
