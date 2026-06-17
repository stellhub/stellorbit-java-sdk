package io.github.stellorbit.client;

import io.github.stellorbit.client.provider.AuthorizationRuleProvider;
import io.github.stellorbit.client.provider.CircuitBreakerRuleProvider;
import io.github.stellorbit.client.provider.RateLimitRuleProvider;
import io.github.stellorbit.client.provider.RouteRuleProvider;
import io.github.stellorbit.client.rule.GovernanceRuleRegistry;

public interface StellorbitClient extends AutoCloseable {

    /**
     * 启动客户端并初始化治理规则。
     */
    void start();

    /**
     * 返回熔断规则 Provider。
     */
    CircuitBreakerRuleProvider circuitBreakers();

    /**
     * 返回路由规则 Provider。
     */
    RouteRuleProvider routes();

    /**
     * 返回鉴权规则 Provider。
     */
    AuthorizationRuleProvider authorizations();

    /**
     * 返回限流规则 Provider。
     */
    RateLimitRuleProvider rateLimits();

    /**
     * 返回当前本地治理规则视图。
     */
    GovernanceRuleRegistry rules();

    /**
     * 关闭客户端资源。
     */
    @Override
    void close();
}
