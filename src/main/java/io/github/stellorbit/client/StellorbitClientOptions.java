package io.github.stellorbit.client;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public final class StellorbitClientOptions {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String DEFAULT_ENV = "dev";
    private static final String DEFAULT_REGION = "default";
    private static final String DEFAULT_ZONE = "default";
    private static final String DEFAULT_CLUSTER = "default";
    private static final String DEFAULT_RULE_NAMESPACE = "governance";
    private static final String DEFAULT_RULE_GROUP = "service-governance";

    private final URI endpoint;
    private final String apiKey;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final URI stellnulaEndpoint;
    private final URI stellnulaGrpcEndpoint;
    private final boolean stellnulaGrpcPlaintext;
    private final String stellnulaApiToken;
    private final String appId;
    private final String clientId;
    private final String env;
    private final String region;
    private final String zone;
    private final String cluster;
    private final String ruleNamespace;
    private final String ruleGroup;
    private final boolean watchEnabled;
    private final boolean failFastOnBootstrap;
    private final Path snapshotDirectory;

    private StellorbitClientOptions(Builder builder) {
        this.endpoint = builder.endpoint;
        this.apiKey = builder.apiKey;
        this.connectTimeout = positive(builder.connectTimeout, "connectTimeout");
        this.requestTimeout = positive(builder.requestTimeout, "requestTimeout");
        this.stellnulaEndpoint = builder.stellnulaEndpoint;
        this.stellnulaGrpcEndpoint = builder.stellnulaGrpcEndpoint;
        this.stellnulaGrpcPlaintext = builder.stellnulaGrpcPlaintext;
        this.stellnulaApiToken = builder.stellnulaApiToken == null ? "" : builder.stellnulaApiToken;
        this.appId = defaultText(builder.appId, "stellorbit-java-sdk");
        this.clientId = defaultText(builder.clientId, "stellorbit-" + UUID.randomUUID());
        this.env = defaultText(builder.env, DEFAULT_ENV);
        this.region = defaultText(builder.region, DEFAULT_REGION);
        this.zone = defaultText(builder.zone, DEFAULT_ZONE);
        this.cluster = defaultText(builder.cluster, DEFAULT_CLUSTER);
        this.ruleNamespace = defaultText(builder.ruleNamespace, DEFAULT_RULE_NAMESPACE);
        this.ruleGroup = defaultText(builder.ruleGroup, DEFAULT_RULE_GROUP);
        this.watchEnabled = builder.watchEnabled;
        this.failFastOnBootstrap = builder.failFastOnBootstrap;
        this.snapshotDirectory = builder.snapshotDirectory;
    }

    /**
     * 创建客户端配置构建器。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回 StellOrbit 服务端地址。
     */
    public URI endpoint() {
        return endpoint;
    }

    /**
     * 返回 API Key。
     */
    public String apiKey() {
        return apiKey;
    }

    /**
     * 返回连接超时时间。
     */
    public Duration connectTimeout() {
        return connectTimeout;
    }

    /**
     * 返回请求超时时间。
     */
    public Duration requestTimeout() {
        return requestTimeout;
    }

    /**
     * 返回 StellNula HTTP 地址。
     */
    public URI stellnulaEndpoint() {
        return stellnulaEndpoint;
    }

    /**
     * 返回 StellNula gRPC Watch 地址。
     */
    public URI stellnulaGrpcEndpoint() {
        return stellnulaGrpcEndpoint;
    }

    /**
     * 返回 gRPC 是否使用明文连接。
     */
    public boolean stellnulaGrpcPlaintext() {
        return stellnulaGrpcPlaintext;
    }

    /**
     * 返回 StellNula API Token。
     */
    public String stellnulaApiToken() {
        return stellnulaApiToken;
    }

    /**
     * 返回当前应用标识。
     */
    public String appId() {
        return appId;
    }

    /**
     * 返回当前客户端实例标识。
     */
    public String clientId() {
        return clientId;
    }

    /**
     * 返回当前环境。
     */
    public String env() {
        return env;
    }

    /**
     * 返回当前区域。
     */
    public String region() {
        return region;
    }

    /**
     * 返回当前可用区。
     */
    public String zone() {
        return zone;
    }

    /**
     * 返回当前集群。
     */
    public String cluster() {
        return cluster;
    }

    /**
     * 返回治理规则 namespace。
     */
    public String ruleNamespace() {
        return ruleNamespace;
    }

    /**
     * 返回治理规则 group。
     */
    public String ruleGroup() {
        return ruleGroup;
    }

    /**
     * 返回是否开启治理规则 watch。
     */
    public boolean watchEnabled() {
        return watchEnabled;
    }

    /**
     * 返回启动同步失败时是否快速失败。
     */
    public boolean failFastOnBootstrap() {
        return failFastOnBootstrap;
    }

    /**
     * 返回本地规则快照目录。
     */
    public Path snapshotDirectory() {
        return snapshotDirectory;
    }

    private static Duration positive(Duration duration, String fieldName) {
        Objects.requireNonNull(duration, fieldName + " must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return duration;
    }

    private static String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static final class Builder {

        private URI endpoint;
        private String apiKey;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;
        private URI stellnulaEndpoint;
        private URI stellnulaGrpcEndpoint;
        private boolean stellnulaGrpcPlaintext = true;
        private String stellnulaApiToken = "";
        private String appId = "stellorbit-java-sdk";
        private String clientId;
        private String env = DEFAULT_ENV;
        private String region = DEFAULT_REGION;
        private String zone = DEFAULT_ZONE;
        private String cluster = DEFAULT_CLUSTER;
        private String ruleNamespace = DEFAULT_RULE_NAMESPACE;
        private String ruleGroup = DEFAULT_RULE_GROUP;
        private boolean watchEnabled = true;
        private boolean failFastOnBootstrap;
        private Path snapshotDirectory;

        private Builder() {
        }

        /**
         * 设置 StellOrbit 服务端地址。
         */
        public Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * 设置 API Key。
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * 设置连接超时时间。
         */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
            return this;
        }

        /**
         * 设置请求超时时间。
         */
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
            return this;
        }

        /**
         * 设置 StellNula HTTP 地址。
         */
        public Builder stellnulaEndpoint(URI stellnulaEndpoint) {
            this.stellnulaEndpoint = stellnulaEndpoint;
            return this;
        }

        /**
         * 设置 StellNula gRPC Watch 地址。
         */
        public Builder stellnulaGrpcEndpoint(URI stellnulaGrpcEndpoint) {
            this.stellnulaGrpcEndpoint = stellnulaGrpcEndpoint;
            return this;
        }

        /**
         * 设置 gRPC 是否使用明文连接。
         */
        public Builder stellnulaGrpcPlaintext(boolean stellnulaGrpcPlaintext) {
            this.stellnulaGrpcPlaintext = stellnulaGrpcPlaintext;
            return this;
        }

        /**
         * 设置 StellNula API Token。
         */
        public Builder stellnulaApiToken(String stellnulaApiToken) {
            this.stellnulaApiToken = stellnulaApiToken;
            return this;
        }

        /**
         * 设置当前应用标识。
         */
        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        /**
         * 设置当前客户端实例标识。
         */
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * 设置当前环境。
         */
        public Builder env(String env) {
            this.env = env;
            return this;
        }

        /**
         * 设置当前区域。
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * 设置当前可用区。
         */
        public Builder zone(String zone) {
            this.zone = zone;
            return this;
        }

        /**
         * 设置当前集群。
         */
        public Builder cluster(String cluster) {
            this.cluster = cluster;
            return this;
        }

        /**
         * 设置治理规则 namespace。
         */
        public Builder ruleNamespace(String ruleNamespace) {
            this.ruleNamespace = ruleNamespace;
            return this;
        }

        /**
         * 设置治理规则 group。
         */
        public Builder ruleGroup(String ruleGroup) {
            this.ruleGroup = ruleGroup;
            return this;
        }

        /**
         * 设置是否开启治理规则 watch。
         */
        public Builder watchEnabled(boolean watchEnabled) {
            this.watchEnabled = watchEnabled;
            return this;
        }

        /**
         * 设置启动同步失败时是否快速失败。
         */
        public Builder failFastOnBootstrap(boolean failFastOnBootstrap) {
            this.failFastOnBootstrap = failFastOnBootstrap;
            return this;
        }

        /**
         * 设置本地规则快照目录。
         */
        public Builder snapshotDirectory(Path snapshotDirectory) {
            this.snapshotDirectory = snapshotDirectory;
            return this;
        }

        /**
         * 构建客户端配置。
         */
        public StellorbitClientOptions build() {
            return new StellorbitClientOptions(this);
        }
    }
}
