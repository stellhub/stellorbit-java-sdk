package io.github.stellorbit.client.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void expandsAggregatedRuleConfigIntoIndividualRules() {
        StellnulaSnapshot snapshot = new StellnulaSnapshot(2, "aggregate", List.of(entry(
                "stellorbit.payment-service.route",
                false,
                routeAggregateContent("ACTIVE", aggregateRouteRule("route-a")))));

        GovernanceRuleRegistry next = snapshotParser.parse(snapshot, GovernanceRuleRegistry.empty());

        assertEquals(1, next.rules().size());
        GovernanceRule rule = next.rules().getFirst();
        assertEquals("route-a", rule.ruleId());
        assertEquals("stellorbit.payment-service.route", rule.configKey());
        assertEquals("payment-service", rule.targetService());
        assertTrue(next.findById("route-a").isPresent());
    }

    @Test
    void removesDeletedAggregatedRuleConfig() {
        GovernanceRuleRegistry previous = snapshotParser.parse(new StellnulaSnapshot(1, "previous", List.of(entry(
                "stellorbit.payment-service.route",
                false,
                routeAggregateContent("ACTIVE", aggregateRouteRule("route-a"))))), GovernanceRuleRegistry.empty());
        StellnulaSnapshot snapshot =
                new StellnulaSnapshot(2, "next", List.of(entry("stellorbit.payment-service.route", true, "")));

        GovernanceRuleRegistry next = snapshotParser.parse(snapshot, previous);

        assertTrue(next.rules().isEmpty());
    }

    @Test
    void keepsPreviousAggregatedRulesWhenCurrentContentIsInvalid() {
        GovernanceRuleRegistry previous = snapshotParser.parse(new StellnulaSnapshot(1, "previous", List.of(entry(
                "stellorbit.payment-service.route",
                false,
                routeAggregateContent("ACTIVE", aggregateRouteRule("route-a"))))), GovernanceRuleRegistry.empty());
        StellnulaSnapshot snapshot =
                new StellnulaSnapshot(2, "next", List.of(entry("stellorbit.payment-service.route", false, "{")));

        GovernanceRuleRegistry next = snapshotParser.parse(snapshot, previous);

        assertEquals(1, next.rules().size());
        assertTrue(next.findById("route-a").isPresent());
    }

    @Test
    void ignoresEmptyAggregatedRuleConfig() {
        StellnulaSnapshot snapshot = new StellnulaSnapshot(2, "aggregate", List.of(entry(
                "stellorbit.payment-service.route",
                false,
                routeAggregateContent("DISABLED", ""))));

        GovernanceRuleRegistry next = snapshotParser.parse(snapshot, GovernanceRuleRegistry.empty());

        assertTrue(next.rules().isEmpty());
    }

    @Test
    void rejectsSingleRuleConfig() {
        StellnulaConfigEntry entry = entry(
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
                """);

        assertThrows(IllegalArgumentException.class, () -> ruleParser.parseAll(entry, "test"));
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

    private String routeAggregateContent(String status, String rulePayload) {
        String rules = rulePayload.isBlank() ? "" : rulePayload;
        int ruleCount = rulePayload.isBlank() ? 0 : 1;
        String routesPayload = rulePayload.isBlank()
                ? "[]"
                : """
                  [
                    [
                      {
                        "targetService": "payment-service-v2",
                        "weight": 100
                      }
                    ]
                  ]
                  """;
        return """
                {
                  "schemaVersion": "stellorbit.governance.aggregate.v1",
                  "releaseVersion": 7,
                  "generatedAt": "2026-06-17T10:15:30+08:00",
                  "applicationCode": "payment-service",
                  "configId": "stellorbit.payment-service.route",
                  "ruleType": "ROUTE",
                  "sourceRuleType": "ROUTE",
                  "targetService": "payment-service",
                  "status": "%s",
                  "priority": 10,
                  "releaseName": "release-7",
                  "runtimeFormat": "JSON",
                  "ruleCount": %d,
                  "rules": [%s],
                  "routes": %s,
                  "checksum": "aggregate-checksum"
                }
                """.formatted(status, ruleCount, rules, routesPayload);
    }

    private String aggregateRouteRule(String ruleId) {
        return """
                {
                  "ruleId": "%s",
                  "configId": "stellorbit.payment-service.route",
                  "ruleType": "ROUTE",
                  "stellnulaRuleType": "ROUTE",
                  "ruleCode": "%s",
                  "ruleName": "Route %s",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 10,
                  "draftVersion": 3,
                  "schemaVersion": "stellorbit.governance.v1",
                  "checksum": "rule-checksum",
                  "content": {
                    "ruleType": "ROUTE",
                    "targetService": "payment-service",
                    "status": "ACTIVE",
                    "priority": 10,
                    "conditions": {
                      "env": "prod"
                    },
                    "routes": [
                      {
                        "targetService": "payment-service-v2",
                        "weight": 100
                      }
                    ]
                  }
                }
                """.formatted(ruleId, ruleId, ruleId);
    }
}
