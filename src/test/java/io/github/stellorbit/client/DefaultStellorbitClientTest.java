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
import io.github.stellorbit.client.source.InMemoryGovernanceRuleSource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefaultStellorbitClientTest {

    private final GovernanceRuleParser parser = new GovernanceRuleParser();

    @Test
    void providesRouteRulesByConditions() {
        GovernanceRule rule = rule(
                "route-payment",
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
    void providesCircuitBreakerRulesWithoutStateMachine() {
        GovernanceRule rule = rule(
                "breaker-payment",
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

    private GovernanceRule rule(String ruleId, String content) {
        return parser.parse(new StellnulaConfigEntry(
                ruleId,
                ruleId + ".json",
                "FILE",
                content,
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
                content.length(),
                "",
                new StellnulaConfigScope("dev", "default", "default", "default")), "test");
    }
}
