package io.github.stellorbit.client.provider;

import io.github.stellorbit.client.model.RateLimitRuleQuery;
import io.github.stellorbit.client.rule.GovernanceRule;
import java.util.List;
import java.util.Optional;

public interface RateLimitRuleProvider {

    /**
     * 查询匹配的限流规则。
     */
    List<GovernanceRule> find(RateLimitRuleQuery query);

    /**
     * 查询第一个匹配的限流规则。
     */
    default Optional<GovernanceRule> first(RateLimitRuleQuery query) {
        return find(query).stream().findFirst();
    }
}
