package io.github.stellorbit.client.provider;

import io.github.stellorbit.client.rule.GovernanceRule;
import io.github.stellorbit.client.rule.GovernanceRuleMatcher;
import io.github.stellorbit.client.rule.GovernanceRuleRegistry;
import io.github.stellorbit.client.rule.GovernanceRuleType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

final class DefaultRuleProviderSupport {

    private final Supplier<GovernanceRuleRegistry> registrySupplier;
    private final GovernanceRuleMatcher matcher = new GovernanceRuleMatcher();

    DefaultRuleProviderSupport(Supplier<GovernanceRuleRegistry> registrySupplier) {
        this.registrySupplier = Objects.requireNonNull(registrySupplier, "registrySupplier must not be null");
    }

    List<GovernanceRule> find(
            GovernanceRuleType ruleType, String serviceName, Map<String, String> attributes) {
        return registrySupplier.get().activeRules(ruleType, serviceName).stream()
                .filter(rule -> matcher.matches(rule, attributes))
                .toList();
    }

    List<GovernanceRule> find(
            List<GovernanceRuleType> ruleTypes, String serviceName, Map<String, String> attributes) {
        return registrySupplier.get().activeRules(ruleTypes, serviceName).stream()
                .filter(rule -> matcher.matches(rule, attributes))
                .toList();
    }
}
