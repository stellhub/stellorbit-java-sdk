package io.github.stellorbit.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.stellnula.config.StellnulaConfigEntry;
import io.github.stellnula.config.StellnulaConfigScope;
import io.github.stellorbit.client.model.AuthorizationRuleQuery;
import io.github.stellorbit.client.model.CircuitBreakerRuleQuery;
import io.github.stellorbit.client.model.RateLimitRuleQuery;
import io.github.stellorbit.client.model.RequestContext;
import io.github.stellorbit.client.model.RouteRuleQuery;
import io.github.stellorbit.client.rule.GovernanceRule;
import io.github.stellorbit.client.rule.GovernanceRuleParser;
import io.github.stellorbit.client.rule.GovernanceRuleRegistry;
import io.github.stellorbit.client.rule.RateLimitRules;
import io.github.stellorbit.client.source.InMemoryGovernanceRuleSource;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefaultStellorbitClientTest {

    private final GovernanceRuleParser parser = new GovernanceRuleParser();

    @Test
    void providesRouteRulesByConditions() {
        GovernanceRule rule = rule(
                "route-payment",
                "ROUTE",
                """
                {
                  "ruleType": "ROUTE",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 10,
                  "conditions": {
                    "env": "prod"
                  },
                  "routes": [
                    {
                      "id": "payment-v2",
                      "targetService": "payment-service-v2",
                      "weight": 100
                    }
                  ]
                }
                """);

        try (StellorbitClient client = client(rule)) {
            client.start();

            List<GovernanceRule> rules = client.routes().find(new RouteRuleQuery(
                    "payment-service",
                    "tenant-a",
                    Map.of("env", "prod"),
                    RequestContext.empty()));

            assertEquals(1, rules.size());
            assertEquals("route-payment", rules.getFirst().ruleId());
        }
    }

    @Test
    void providesAuthorizationRulesWithoutExecutingAuth() {
        GovernanceRule rule = rule(
                "auth-payment",
                "AUTH",
                """
                {
                  "ruleType": "AUTH",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 0,
                  "auth": {
                    "requiredRoles": ["payment-admin"]
                  }
                }
                """);

        try (StellorbitClient client = client(rule)) {
            client.start();

            List<GovernanceRule> rules = client.authorizations().find(new AuthorizationRuleQuery(
                    "payment-service",
                    "alice",
                    "tenant-a",
                    Set.of("viewer"),
                    null,
                    RequestContext.empty()));

            assertEquals(1, rules.size());
            assertEquals("auth-payment", rules.getFirst().ruleId());
        }
    }

    @Test
    void matchesAuthorizationRulesWithMultiValueRoles() {
        GovernanceRule rule = rule(
                "auth-payment",
                "AUTH",
                """
                {
                  "ruleType": "AUTH",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 0,
                  "conditions": {
                    "roles": ["payment-admin"]
                  },
                  "auth": {
                    "requiredRoles": ["payment-admin"]
                  }
                }
                """);

        try (StellorbitClient client = client(rule)) {
            client.start();

            List<GovernanceRule> rules = client.authorizations().find(new AuthorizationRuleQuery(
                    "payment-service",
                    "alice",
                    "tenant-a",
                    Set.of("viewer", "payment-admin"),
                    null,
                    RequestContext.empty()));

            assertEquals(1, rules.size());
            assertEquals("auth-payment", rules.getFirst().ruleId());
        }
    }

    @Test
    void routeAttributesCannotOverrideServiceName() {
        GovernanceRule rule = rule(
                "route-payment",
                "ROUTE",
                """
                {
                  "ruleType": "ROUTE",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 10,
                  "conditions": {
                    "serviceName": "spoofed-service"
                  },
                  "routes": [
                    {
                      "id": "payment-v2",
                      "targetService": "payment-service-v2",
                      "weight": 100
                    }
                  ]
                }
                """);

        try (StellorbitClient client = client(rule)) {
            client.start();

            List<GovernanceRule> rules = client.routes().find(new RouteRuleQuery(
                    "payment-service",
                    "tenant-a",
                    Map.of("serviceName", "spoofed-service"),
                    RequestContext.empty()));

            assertTrue(rules.isEmpty());
        }
    }

    @Test
    void providesRateLimitRulesWithoutLocalLimiter() {
        GovernanceRule rule = rule(
                "rate-payment",
                "RATE_LIMIT",
                """
                {
                  "ruleType": "RATE_LIMIT",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 0,
                  "limit": {
                    "quota": 100,
                    "windowSeconds": 60,
                    "keyAttribute": "tenantId"
                  }
                }
                """);

        RequestContext context = RequestContext.builder().tenantId("tenant-a").build();
        try (StellorbitClient client = client(rule)) {
            client.start();

            List<GovernanceRule> rules =
                    client.rateLimits().find(new RateLimitRuleQuery("payment-service", null, context));

            assertEquals(1, rules.size());
            assertEquals("rate-payment", rules.getFirst().ruleId());
        }
    }

    @Test
    void filtersEnterpriseRateLimitRulesByNewContractFields() {
        GovernanceRule httpHeaderRule = rule(
                "rate-http-header",
                "RATE_LIMIT",
                """
                {
                  "ruleType": "RATE_LIMIT",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 0,
                  "limitMode": "HEADER",
                  "limitType": "HEADER",
                  "limitAlgorithm": "SLIDING_WINDOW",
                  "trafficProtocol": "HTTP",
                  "executionLocation": "APPLICATION",
                  "coordinationMode": "GLOBAL_QUOTA",
                  "targetSelector": {
                    "services": ["payment-service"]
                  },
                  "requestMatcher": {
                    "paths": ["/pay"]
                  },
                  "keyExtractor": {
                    "keys": [
                      {
                        "name": "tenant-header",
                        "source": "HEADER",
                        "key": "X-Tenant-Id",
                        "required": true,
                        "normalize": "LOWERCASE"
                      }
                    ]
                  },
                  "dimensions": ["tenantId", "path"],
                  "quotaConfig": {
                    "quota": 1000
                  },
                  "windowConfig": {
                    "windowSeconds": 60
                  },
                  "burstConfig": {
                    "burst": 100
                  },
                  "concurrencyConfig": {
                    "maxConcurrent": 20
                  },
                  "hotspotConfig": {
                    "topN": 50
                  },
                  "customPolicy": {
                    "type": "EXPRESSION"
                  },
                  "modelLimitConfig": {
                    "maxTokens": 10000
                  },
                  "fallbackPolicy": {
                    "failPolicy": "FAIL_OPEN"
                  },
                  "responsePolicy": {
                    "status": 429
                  },
                  "observabilityConfig": {
                    "metrics": true
                  },
                  "shadowConfig": {
                    "enabled": false
                  },
                  "limit": {
                    "quota": 1000,
                    "windowSeconds": 60
                  }
                }
                """);
        GovernanceRule grpcHeaderRule = rule(
                "rate-grpc-metadata",
                "RATE_LIMIT",
                """
                {
                  "ruleType": "RATE_LIMIT",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 1,
                  "limitMode": "HEADER",
                  "limitType": "GRPC_METADATA",
                  "trafficProtocol": "GRPC",
                  "executionLocation": "APPLICATION",
                  "coordinationMode": "GLOBAL_QUOTA",
                  "keyExtractor": {
                    "keys": [
                      {
                        "name": "tenant-metadata",
                        "source": "GRPC_METADATA",
                        "key": "tenant-id",
                        "required": true
                      }
                    ]
                  },
                  "limit": {
                    "quota": 1000,
                    "windowSeconds": 60
                  }
                }
                """);

        try (StellorbitClient client = client(httpHeaderRule, grpcHeaderRule)) {
            client.start();

            RateLimitRuleQuery query = new RateLimitRuleQuery(
                            "payment-service",
                            null,
                            RequestContext.builder().tenantId("tenant-a").build())
                    .withLimitMode("HEADER")
                    .withLimitType("HEADER")
                    .withTrafficProtocol("HTTP")
                    .withExecutionLocation("APPLICATION")
                    .withCoordinationMode("GLOBAL_QUOTA")
                    .withKeyExtractorSource("HEADER");

            List<GovernanceRule> rules = client.rateLimits().find(query);

            assertEquals(1, rules.size());
            GovernanceRule rule = rules.getFirst();
            assertEquals("rate-http-header", rule.ruleId());
            assertEquals("HEADER", RateLimitRules.limitMode(rule));
            assertEquals("SLIDING_WINDOW", RateLimitRules.limitAlgorithm(rule));
            assertEquals("HTTP", RateLimitRules.trafficProtocol(rule));
            assertEquals("APPLICATION", RateLimitRules.executionLocation(rule));
            assertEquals("GLOBAL_QUOTA", RateLimitRules.coordinationMode(rule));
            assertEquals(List.of("HEADER"), RateLimitRules.keyExtractorSources(rule));
            assertTrue(RateLimitRules.usesHttpHeaderExtractor(rule));
        }
    }

    @Test
    void exposesDistributedAndLocalRuntimeRateLimitHelpers() {
        GovernanceRule distributed = rule(
                "rate-global",
                "RATE_LIMIT",
                """
                {
                  "ruleType": "RATE_LIMIT",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 0,
                  "limitMode": "QUOTA",
                  "trafficProtocol": "HTTP",
                  "coordinationMode": "GLOBAL_SYNC",
                  "limit": {
                    "quota": 100,
                    "windowSeconds": 60
                  }
                }
                """);
        GovernanceRule local = rule(
                "rate-local",
                "RATE_LIMIT",
                """
                {
                  "ruleType": "RATE_LIMIT",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 1,
                  "limitMode": "QPS",
                  "trafficProtocol": "HTTP",
                  "coordinationMode": "LOCAL_ONLY",
                  "limit": {
                    "quota": 100,
                    "windowSeconds": 60
                  }
                }
                """);

        try (StellorbitClient client = client(distributed, local)) {
            client.start();

            RateLimitRuleQuery query = new RateLimitRuleQuery(
                    "payment-service",
                    null,
                    RequestContext.builder().tenantId("tenant-a").build());

            List<GovernanceRule> distributedRules = client.rateLimits().distributed(query);
            List<GovernanceRule> localRules = client.rateLimits().localRuntime(query);

            assertEquals(1, distributedRules.size());
            assertEquals("rate-global", distributedRules.getFirst().ruleId());
            assertEquals(1, localRules.size());
            assertEquals("rate-local", localRules.getFirst().ruleId());
        }
    }

    @Test
    void defaultsMissingLimitModeToQpsAndLocalRuntime() {
        GovernanceRule legacyQps = rule(
                "rate-legacy-qps",
                "RATE_LIMIT",
                """
                {
                  "ruleType": "RATE_LIMIT",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 0,
                  "limit": {
                    "quota": 100,
                    "windowSeconds": 60
                  }
                }
                """);

        try (StellorbitClient client = client(legacyQps)) {
            client.start();

            RateLimitRuleQuery query = new RateLimitRuleQuery(
                            "payment-service",
                            null,
                            RequestContext.builder().tenantId("tenant-a").build());
            List<GovernanceRule> rules = client.rateLimits().find(query.withLimitMode("QPS"));
            List<GovernanceRule> localRules = client.rateLimits().localRuntime(query);

            assertEquals(1, rules.size());
            assertEquals("rate-legacy-qps", rules.getFirst().ruleId());
            assertEquals(1, localRules.size());
            assertEquals("rate-legacy-qps", localRules.getFirst().ruleId());
            assertEquals("QPS", RateLimitRules.limitMode(legacyQps));
            assertEquals("LOCAL_ONLY", RateLimitRules.coordinationMode(legacyQps));
        }
    }

    @Test
    void providesCircuitBreakerRulesWithoutStateMachine() {
        GovernanceRule rule = rule(
                "breaker-payment",
                "CIRCUIT_BREAKER",
                """
                {
                  "ruleType": "CIRCUIT_BREAKER",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 0,
                  "breaker": {
                    "failureRateThreshold": 50,
                    "slidingWindowSize": 100
                  }
                }
                """);

        try (StellorbitClient client = client(rule)) {
            client.start();

            assertTrue(client.circuitBreakers()
                    .first(new CircuitBreakerRuleQuery(
                            "payment-service", "POST /pay", RequestContext.empty()))
                    .isPresent());
        }
    }

    private StellorbitClient client(GovernanceRule... rules) {
        GovernanceRuleRegistry registry = GovernanceRuleRegistry.of(1, "test", List.of(rules));
        return new DefaultStellorbitClient(new InMemoryGovernanceRuleSource(registry));
    }

    private GovernanceRule rule(String ruleId, String ruleType, String content) {
        String configId = "stellorbit.payment-service." + ruleType.toLowerCase(Locale.ROOT);
        String aggregateContent = """
                {
                  "schemaVersion": "stellorbit.governance.aggregate.v1",
                  "releaseVersion": 1,
                  "generatedAt": "2026-06-18T00:00:00+08:00",
                  "applicationCode": "payment-service",
                  "configId": "%s",
                  "ruleType": "%s",
                  "sourceRuleType": "%s",
                  "targetService": "payment-service",
                  "status": "ACTIVE",
                  "priority": 0,
                  "releaseName": "test-release",
                  "runtimeFormat": "JSON",
                  "ruleCount": 1,
                  "rules": [
                    {
                      "ruleId": "%s",
                      "configId": "%s",
                      "ruleType": "%s",
                      "stellnulaRuleType": "%s",
                      "ruleCode": "%s",
                      "ruleName": "%s",
                      "schemaVersion": "stellorbit.governance.v1",
                      "checksum": "rule-checksum",
                      "content": %s
                    }
                  ],
                  "%s": [],
                  "checksum": "aggregate-checksum"
                }
                """.formatted(
                configId,
                ruleType,
                ruleType,
                ruleId,
                configId,
                ruleType,
                ruleType,
                ruleId,
                ruleId,
                content,
                validatorField(ruleType));
        return parser.parse(new StellnulaConfigEntry(
                configId,
                configId + ".json",
                "FILE",
                aggregateContent,
                1,
                1,
                false,
                false,
                "BASE",
                null,
                null,
                null,
                "identity",
                "INLINE",
                aggregateContent.length(),
                "",
                new StellnulaConfigScope("dev", "default", "default", "default")), "test");
    }

    private String validatorField(String ruleType) {
        return switch (ruleType) {
            case "ROUTE" -> "routes";
            case "RATE_LIMIT" -> "limit";
            case "CIRCUIT_BREAKER" -> "breaker";
            case "AUTH" -> "auth";
            case "DEGRADE" -> "degrade";
            default -> throw new IllegalArgumentException("unsupported rule type: " + ruleType);
        };
    }
}
