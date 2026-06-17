package io.github.stellorbit.client.rule;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class GovernanceRuleRegistry {

    private static final Comparator<GovernanceRule> EVALUATION_ORDER =
            Comparator.comparingInt(GovernanceRule::priority)
                    .thenComparing(Comparator.comparingLong(GovernanceRule::revision).reversed())
                    .thenComparing(GovernanceRule::ruleId);

    private final long revision;
    private final String checksum;
    private final List<GovernanceRule> rules;
    private final Map<String, GovernanceRule> rulesById;

    private GovernanceRuleRegistry(long revision, String checksum, List<GovernanceRule> rules) {
        this.revision = revision;
        this.checksum = checksum == null ? "" : checksum;
        this.rules = rules.stream().sorted(EVALUATION_ORDER).toList();
        Map<String, GovernanceRule> indexed = new LinkedHashMap<>();
        for (GovernanceRule rule : this.rules) {
            indexed.put(rule.ruleId(), rule);
        }
        this.rulesById = Map.copyOf(indexed);
    }

    /**
     * 创建空规则注册表。
     */
    public static GovernanceRuleRegistry empty() {
        return new GovernanceRuleRegistry(0, "", List.of());
    }

    /**
     * 创建规则注册表。
     */
    public static GovernanceRuleRegistry of(long revision, String checksum, List<GovernanceRule> rules) {
        return new GovernanceRuleRegistry(revision, checksum, rules == null ? List.of() : List.copyOf(rules));
    }

    /**
     * 返回注册表 revision。
     */
    public long revision() {
        return revision;
    }

    /**
     * 返回注册表 checksum。
     */
    public String checksum() {
        return checksum;
    }

    /**
     * 返回所有规则。
     */
    public List<GovernanceRule> rules() {
        return rules;
    }

    /**
     * 按规则 ID 查询规则。
     */
    public Optional<GovernanceRule> findById(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rulesById.get(ruleId));
    }

    /**
     * 查询指定类型和服务的启用规则。
     */
    public List<GovernanceRule> activeRules(GovernanceRuleType ruleType, String serviceName) {
        Objects.requireNonNull(ruleType, "ruleType must not be null");
        return rules.stream()
                .filter(GovernanceRule::active)
                .filter(rule -> rule.ruleType() == ruleType)
                .filter(rule -> rule.matchesService(serviceName))
                .toList();
    }

    /**
     * 查询多个类型和服务的启用规则。
     */
    public List<GovernanceRule> activeRules(List<GovernanceRuleType> ruleTypes, String serviceName) {
        List<GovernanceRuleType> types = ruleTypes == null ? List.of() : List.copyOf(ruleTypes);
        return rules.stream()
                .filter(GovernanceRule::active)
                .filter(rule -> types.contains(rule.ruleType()))
                .filter(rule -> rule.matchesService(serviceName))
                .toList();
    }
}
