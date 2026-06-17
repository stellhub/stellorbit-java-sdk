package io.github.stellorbit.client.provider;

import io.github.stellorbit.client.model.CircuitBreakerRuleQuery;
import io.github.stellorbit.client.rule.GovernanceRule;
import java.util.List;
import java.util.Optional;

public interface CircuitBreakerRuleProvider {

    /**
     * 查询匹配的熔断规则。
     */
    List<GovernanceRule> find(CircuitBreakerRuleQuery query);

    /**
     * 查询第一个匹配的熔断规则。
     */
    default Optional<GovernanceRule> first(CircuitBreakerRuleQuery query) {
        return find(query).stream().findFirst();
    }
}
