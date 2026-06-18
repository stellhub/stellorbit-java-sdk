package io.github.stellorbit.client.source;

import io.github.stellnula.config.StellnulaConfigEntry;
import io.github.stellnula.config.StellnulaSnapshot;
import io.github.stellorbit.client.rule.GovernanceRule;
import io.github.stellorbit.client.rule.GovernanceRuleParser;
import io.github.stellorbit.client.rule.GovernanceRuleRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

final class GovernanceRuleSnapshotParser {

    private static final Logger LOGGER = Logger.getLogger(GovernanceRuleSnapshotParser.class.getName());

    private final GovernanceRuleParser parser;

    GovernanceRuleSnapshotParser(GovernanceRuleParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
    }

    /**
     * 将 StellNula 快照解析为本地规则注册表。
     */
    GovernanceRuleRegistry parse(StellnulaSnapshot snapshot, GovernanceRuleRegistry previous) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        GovernanceRuleRegistry previousRegistry = previous == null ? GovernanceRuleRegistry.empty() : previous;
        if (snapshot.entries().isEmpty()) {
            return GovernanceRuleRegistry.of(snapshot.revision(), snapshot.checksum(), List.of());
        }

        Map<String, GovernanceRule> previousRules = indexPreviousRules(previousRegistry);
        List<GovernanceRule> parsed = new ArrayList<>();
        boolean hasDeletedEntry = false;
        boolean hasInvalidEntry = false;
        boolean hasNonDeletedEntry = false;

        for (StellnulaConfigEntry entry : snapshot.entries()) {
            if (entry.deleted()) {
                hasDeletedEntry = true;
                removePreviousRules(previousRules, entry.configId());
                continue;
            }
            hasNonDeletedEntry = true;
            try {
                List<GovernanceRule> rules = parser.parseAll(entry, snapshot.checksum());
                removePreviousRules(previousRules, entry.configId());
                parsed.addAll(rules);
            } catch (RuntimeException ex) {
                hasInvalidEntry = true;
                List<GovernanceRule> fallbackRules = removePreviousRules(previousRules, entry.configId());
                if (!fallbackRules.isEmpty()) {
                    parsed.addAll(fallbackRules);
                } else {
                    LOGGER.log(
                            Level.WARNING,
                            "skip invalid governance rule {0}: {1}",
                            new Object[] {entry.configId(), ex.getMessage()});
                }
            }
        }

        if (parsed.isEmpty()
                && hasInvalidEntry
                && hasNonDeletedEntry
                && !hasDeletedEntry
                && !previousRegistry.rules().isEmpty()) {
            LOGGER.warning("all governance rules failed to parse, keep last-known-good registry");
            return previousRegistry;
        }
        return GovernanceRuleRegistry.of(snapshot.revision(), snapshot.checksum(), parsed);
    }

    private Map<String, GovernanceRule> indexPreviousRules(GovernanceRuleRegistry previous) {
        Map<String, GovernanceRule> indexed = new LinkedHashMap<>();
        for (GovernanceRule rule : previous.rules()) {
            indexed.put(rule.ruleId(), rule);
        }
        return indexed;
    }

    private List<GovernanceRule> removePreviousRules(Map<String, GovernanceRule> previousRules, String configId) {
        List<GovernanceRule> removed = new ArrayList<>();
        var iterator = previousRules.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, GovernanceRule> entry = iterator.next();
            if (entry.getValue().fromConfig(configId)) {
                removed.add(entry.getValue());
                iterator.remove();
            }
        }
        return removed;
    }
}
