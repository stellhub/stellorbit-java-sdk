package io.github.stellorbit.client.source;

import io.github.stellorbit.client.rule.GovernanceRuleRegistry;
import java.util.Objects;

public final class InMemoryGovernanceRuleSource implements GovernanceRuleSource {

    private final GovernanceRuleRegistry registry;

    public InMemoryGovernanceRuleSource(GovernanceRuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    /**
     * 创建空内存规则源。
     */
    public static InMemoryGovernanceRuleSource empty() {
        return new InMemoryGovernanceRuleSource(GovernanceRuleRegistry.empty());
    }

    /**
     * 启动内存规则源。
     */
    @Override
    public void start() {
        // No startup work is required for immutable in-memory rules.
    }

    /**
     * 返回当前规则注册表。
     */
    @Override
    public GovernanceRuleRegistry registry() {
        return registry;
    }

    /**
     * 关闭内存规则源。
     */
    @Override
    public void close() {
        // No resource needs to be released.
    }
}
