package io.github.stellorbit.client.source;

import io.github.stellorbit.client.rule.GovernanceRuleRegistry;

public interface GovernanceRuleSource extends AutoCloseable {

    /**
     * 启动规则源并加载初始规则。
     */
    void start();

    /**
     * 返回当前规则注册表。
     */
    GovernanceRuleRegistry registry();

    /**
     * 关闭规则源。
     */
    @Override
    void close();
}
