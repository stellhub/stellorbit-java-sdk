package io.github.stellorbit.client.source;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.stellnula.config.StellnulaConfigEntry;
import io.github.stellnula.config.StellnulaConfigScope;
import io.github.stellnula.config.StellnulaSnapshot;
import io.github.stellorbit.client.rule.GovernanceRule;
import io.github.stellorbit.client.rule.GovernanceRuleParser;
import io.github.stellorbit.client.rule.GovernanceRuleRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class GovernanceRuleSnapshotParserTest {

    private final GovernanceRuleParser ruleParser = new GovernanceRuleParser();
    private final GovernanceRuleSnapshotParser snapshotParser = new GovernanceRuleSnapshotParser(ruleParser);

    @Test
    void removesDeletedRuleInsteadOfKeepingLastKnownGood() {
        GovernanceRule previousRule = ruleParser.parse(entry(
                "rate-payment",
                false,
                """
                {
                  "ruleType": "RATE_LIMIT",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 0,
                  "limit": {
                    "quota": 100
                  }
                }
                """), "previous");
        GovernanceRuleRegistry previous = GovernanceRuleRegistry.of(1, "previous", List.of(previousRule));
        StellnulaSnapshot snapshot = new StellnulaSnapshot(2, "next", List.of(entry("rate-payment", true, "")));

        GovernanceRuleRegistry next = snapshotParser.parse(snapshot, previous);

        assertTrue(next.rules().isEmpty());
    }

    @Test
    void keepsPreviousRuleWhenCurrentContentIsInvalid() {
        GovernanceRule previousRule = ruleParser.parse(entry(
                "rate-payment",
                false,
                """
                {
                  "ruleType": "RATE_LIMIT",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 0,
                  "limit": {
                    "quota": 100
                  }
                }
                """), "previous");
        GovernanceRuleRegistry previous = GovernanceRuleRegistry.of(1, "previous", List.of(previousRule));
        StellnulaSnapshot snapshot = new StellnulaSnapshot(2, "next", List.of(entry("rate-payment", false, "{")));

        GovernanceRuleRegistry next = snapshotParser.parse(snapshot, previous);

        assertTrue(next.findById("rate-payment").isPresent());
    }

    private StellnulaConfigEntry entry(String ruleId, boolean deleted, String content) {
        return new StellnulaConfigEntry(
                ruleId,
                ruleId + ".json",
                "FILE",
                content,
                1,
                1,
                false,
                deleted,
                "BASE",
                null,
                null,
                null,
                "identity",
                "INLINE",
                content.length(),
                "",
                new StellnulaConfigScope("dev", "default", "default", "default"));
    }
}
