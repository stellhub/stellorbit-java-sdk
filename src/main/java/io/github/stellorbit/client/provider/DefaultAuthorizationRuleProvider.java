package io.github.stellorbit.client.provider;

import io.github.stellorbit.client.model.AuthorizationRuleQuery;
import io.github.stellorbit.client.rule.GovernanceRule;
import io.github.stellorbit.client.rule.GovernanceRuleRegistry;
import io.github.stellorbit.client.rule.GovernanceRuleType;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultAuthorizationRuleProvider implements AuthorizationRuleProvider {

    private final DefaultRuleProviderSupport support;

    public DefaultAuthorizationRuleProvider(Supplier<GovernanceRuleRegistry> registrySupplier) {
        this.support = new DefaultRuleProviderSupport(registrySupplier);
    }

    /**
     * 查询匹配的鉴权规则。
     */
    @Override
    public List<GovernanceRule> find(AuthorizationRuleQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return support.find(GovernanceRuleType.AUTH, query.serviceName(), query.attributes());
    }
}
