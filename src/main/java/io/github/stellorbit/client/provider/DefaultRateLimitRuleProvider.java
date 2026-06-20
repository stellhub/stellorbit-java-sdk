package io.github.stellorbit.client.provider;

import io.github.stellorbit.client.model.RateLimitRuleQuery;
import io.github.stellorbit.client.rule.GovernanceRule;
import io.github.stellorbit.client.rule.GovernanceRuleRegistry;
import io.github.stellorbit.client.rule.GovernanceRuleType;
import io.github.stellorbit.client.rule.RateLimitRules;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultRateLimitRuleProvider implements RateLimitRuleProvider {

    private final DefaultRuleProviderSupport support;

    public DefaultRateLimitRuleProvider(Supplier<GovernanceRuleRegistry> registrySupplier) {
        this.support = new DefaultRuleProviderSupport(registrySupplier);
    }

    /**
     * 查询匹配的限流规则。
     */
    @Override
    public List<GovernanceRule> find(RateLimitRuleQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return support.find(GovernanceRuleType.RATE_LIMIT, query.serviceName(), query.attributes()).stream()
                .filter(rule -> matches(query.limitMode(), RateLimitRules.limitMode(rule)))
                .filter(rule -> matches(query.limitType(), RateLimitRules.limitType(rule)))
                .filter(rule -> matches(query.trafficProtocol(), RateLimitRules.trafficProtocol(rule)))
                .filter(rule -> matches(query.executionLocation(), RateLimitRules.executionLocation(rule)))
                .filter(rule -> matches(query.coordinationMode(), RateLimitRules.coordinationMode(rule)))
                .filter(rule -> RateLimitRules.usesKeyExtractorSource(rule, query.keyExtractorSource()))
                .toList();
    }

    private boolean matches(String expected, String actual) {
        return expected == null || RateLimitRules.enumEquals(actual, expected);
    }
}
