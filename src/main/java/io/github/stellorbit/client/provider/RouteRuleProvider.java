package io.github.stellorbit.client.provider;

import io.github.stellorbit.client.model.RouteRuleQuery;
import io.github.stellorbit.client.rule.GovernanceRule;
import java.util.List;
import java.util.Optional;

public interface RouteRuleProvider {

    /**
     * 查询匹配的路由规则。
     */
    List<GovernanceRule> find(RouteRuleQuery query);

    /**
     * 查询第一个匹配的路由规则。
     */
    default Optional<GovernanceRule> first(RouteRuleQuery query) {
        return find(query).stream().findFirst();
    }
}
