package io.github.stellorbit.client;

import io.github.stellorbit.client.provider.AuthorizationRuleProvider;
import io.github.stellorbit.client.provider.CircuitBreakerRuleProvider;
import io.github.stellorbit.client.provider.DefaultAuthorizationRuleProvider;
import io.github.stellorbit.client.provider.DefaultCircuitBreakerRuleProvider;
import io.github.stellorbit.client.provider.DefaultRateLimitRuleProvider;
import io.github.stellorbit.client.provider.DefaultRouteRuleProvider;
import io.github.stellorbit.client.provider.RateLimitRuleProvider;
import io.github.stellorbit.client.provider.RouteRuleProvider;
import io.github.stellorbit.client.rule.GovernanceRuleRegistry;
import io.github.stellorbit.client.source.GovernanceRuleSource;
import io.github.stellorbit.client.source.StellnulaGovernanceRuleSource;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DefaultStellorbitClient implements StellorbitClient {

    private final GovernanceRuleSource ruleSource;
    private final CircuitBreakerRuleProvider circuitBreakerRuleProvider;
    private final RouteRuleProvider routeRuleProvider;
    private final AuthorizationRuleProvider authorizationRuleProvider;
    private final RateLimitRuleProvider rateLimitRuleProvider;
    private final AtomicBoolean started = new AtomicBoolean();

    public DefaultStellorbitClient(StellorbitClientOptions options) {
        this(new StellnulaGovernanceRuleSource(options));
    }

    public DefaultStellorbitClient(GovernanceRuleSource ruleSource) {
        this.ruleSource = Objects.requireNonNull(ruleSource, "ruleSource must not be null");
        this.circuitBreakerRuleProvider = new DefaultCircuitBreakerRuleProvider(this::rules);
        this.routeRuleProvider = new DefaultRouteRuleProvider(this::rules);
        this.authorizationRuleProvider = new DefaultAuthorizationRuleProvider(this::rules);
        this.rateLimitRuleProvider = new DefaultRateLimitRuleProvider(this::rules);
    }

    /**
     * 启动客户端并初始化治理规则。
     */
    @Override
    public void start() {
        if (started.compareAndSet(false, true)) {
            ruleSource.start();
        }
    }

    /**
     * 返回熔断规则 Provider。
     */
    @Override
    public CircuitBreakerRuleProvider circuitBreakers() {
        return circuitBreakerRuleProvider;
    }

    /**
     * 返回路由规则 Provider。
     */
    @Override
    public RouteRuleProvider routes() {
        return routeRuleProvider;
    }

    /**
     * 返回鉴权规则 Provider。
     */
    @Override
    public AuthorizationRuleProvider authorizations() {
        return authorizationRuleProvider;
    }

    /**
     * 返回限流规则 Provider。
     */
    @Override
    public RateLimitRuleProvider rateLimits() {
        return rateLimitRuleProvider;
    }

    /**
     * 返回当前本地治理规则视图。
     */
    @Override
    public GovernanceRuleRegistry rules() {
        return ruleSource.registry();
    }

    /**
     * 关闭客户端资源。
     */
    @Override
    public void close() {
        ruleSource.close();
    }
}
