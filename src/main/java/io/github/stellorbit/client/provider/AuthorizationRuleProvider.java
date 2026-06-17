package io.github.stellorbit.client.provider;

import io.github.stellorbit.client.model.AuthorizationRuleQuery;
import io.github.stellorbit.client.rule.GovernanceRule;
import java.util.List;
import java.util.Optional;

public interface AuthorizationRuleProvider {

    /**
     * 查询匹配的鉴权规则。
     */
    List<GovernanceRule> find(AuthorizationRuleQuery query);

    /**
     * 查询第一个匹配的鉴权规则。
     */
    default Optional<GovernanceRule> first(AuthorizationRuleQuery query) {
        return find(query).stream().findFirst();
    }
}
