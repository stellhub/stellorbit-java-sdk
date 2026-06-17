package io.github.stellorbit.client.provider;

import io.github.stellorbit.client.model.CircuitBreakerRuleQuery;
import io.github.stellorbit.client.rule.GovernanceRule;
import io.github.stellorbit.client.rule.GovernanceRuleRegistry;
import io.github.stellorbit.client.rule.GovernanceRuleType;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultCircuitBreakerRuleProvider implements CircuitBreakerRuleProvider {

    private final DefaultRuleProviderSupport support;

    public DefaultCircuitBreakerRuleProvider(Supplier<GovernanceRuleRegistry> registrySupplier) {
        this.support = new DefaultRuleProviderSupport(registrySupplier);
    }

    /**
     * 查询匹配的熔断规则。
     */
    @Override
    public List<GovernanceRule> find(CircuitBreakerRuleQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return support.find(
                List.of(GovernanceRuleType.CIRCUIT_BREAKER, GovernanceRuleType.DEGRADE),
                query.serviceName(),
                query.attributes());
    }
}
