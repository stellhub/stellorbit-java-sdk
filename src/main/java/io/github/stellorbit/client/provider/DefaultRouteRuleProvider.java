package io.github.stellorbit.client.provider;

import io.github.stellorbit.client.model.RouteRuleQuery;
import io.github.stellorbit.client.rule.GovernanceRule;
import io.github.stellorbit.client.rule.GovernanceRuleRegistry;
import io.github.stellorbit.client.rule.GovernanceRuleType;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultRouteRuleProvider implements RouteRuleProvider {

    private final DefaultRuleProviderSupport support;

    public DefaultRouteRuleProvider(Supplier<GovernanceRuleRegistry> registrySupplier) {
        this.support = new DefaultRuleProviderSupport(registrySupplier);
    }

    /**
     * 查询匹配的路由规则。
     */
    @Override
    public List<GovernanceRule> find(RouteRuleQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return support.find(GovernanceRuleType.ROUTE, query.serviceName(), query.attributes());
    }
}
