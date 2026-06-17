# StellOrbit Java SDK

`stellorbit-java-sdk` is the Java client SDK for [`stellhub/stellorbit-service`](https://github.com/stellhub/stellorbit-service).
It consumes service governance rules through [`stellnula-java-sdk`](https://github.com/stellhub/stellnula-java-sdk)
and exposes strongly typed rule providers for routing, circuit breaker, authorization, and rate limit integrations.

## Positioning

`stellorbit-service` remains the control plane and rule producer. Rules are published to
`stellnula-service` under:

| Field | Value |
| --- | --- |
| `namespace` | `governance` |
| `group` | `service-governance` |
| `format` | `json` |

This SDK initializes a StellNula client for that rule channel, loads the startup snapshot,
watches runtime changes, builds a local immutable rule registry, and exposes governance
rules through `StellorbitClient`.

## Capabilities

- Strongly typed route rule provider.
- Strongly typed circuit breaker rule provider.
- Strongly typed authorization rule provider.
- Strongly typed rate limit rule provider.
- Generic rule condition matching for framework adapters.
- StellNula bootstrap, snapshot, revision, checksum, and gRPC Watch integration.
- Last-known-good rule registry fallback when a single rule update is invalid.
- Legacy HTTP client for existing StellOrbit management or compatibility calls.

## Current Status

| Item | Value |
| --- | --- |
| Stability | Early development |
| Language | Java |
| Minimum Java version | 25 |
| Rule channel | `stellnula-service` |
| Rule namespace | `governance` |
| Rule group | `service-governance` |
| Target service | `stellorbit-service` |
| Maintainer | StellHub |

## Installation

```xml
<dependency>
    <groupId>io.github.stellhub</groupId>
    <artifactId>stellorbit-java-sdk</artifactId>
    <version>${stellorbit-java-sdk.version}</version>
</dependency>
```

## Quick Start

```java
package example;

import io.github.stellorbit.client.DefaultStellorbitClient;
import io.github.stellorbit.client.StellorbitClient;
import io.github.stellorbit.client.StellorbitClientOptions;
import io.github.stellorbit.client.model.AuthorizationRuleQuery;
import io.github.stellorbit.client.model.RateLimitRuleQuery;
import io.github.stellorbit.client.model.RequestContext;
import io.github.stellorbit.client.model.RouteRuleQuery;
import java.net.URI;
import java.util.Map;
import java.util.Set;

public class StellorbitExample {

    public static void main(String[] args) {
        StellorbitClientOptions options = StellorbitClientOptions.builder()
                .stellnulaEndpoint(URI.create("http://localhost:8060"))
                .appId("payment-service")
                .clientId("payment-service-local-1")
                .env("dev")
                .build();

        try (StellorbitClient client = new DefaultStellorbitClient(options)) {
            client.start();

            RequestContext context = RequestContext.builder()
                    .tenantId("tenant-a")
                    .trafficTag("gray")
                    .attributes(Map.of("env", "dev"))
                    .build();

            System.out.println(client.routes().find(new RouteRuleQuery(
                    "payment-service",
                    "tenant-a",
                    Map.of("env", "dev"),
                    context)));

            System.out.println(client.authorizations().find(new AuthorizationRuleQuery(
                    "payment-service",
                    "alice",
                    "tenant-a",
                    Set.of("payment-admin"),
                    null,
                    context)));

            System.out.println(client.rateLimits().find(new RateLimitRuleQuery(
                    "payment-service",
                    "tenant-a",
                    context)));
        }
    }
}
```

## API Surface

| Method | Responsibility |
| --- | --- |
| `start()` | Start StellNula bootstrap/watch and load local rules |
| `circuitBreakers()` | Return `CircuitBreakerRuleProvider` |
| `routes()` | Return `RouteRuleProvider` |
| `authorizations()` | Return `AuthorizationRuleProvider` |
| `rateLimits()` | Return `RateLimitRuleProvider` |
| `rules()` | Return the current immutable local rule registry |

`StellorbitHttpClient` is retained as `StellorbitRemoteClient` for legacy remote HTTP calls.
New data-plane integrations should use `DefaultStellorbitClient`.

The core SDK does not embed Resilience4j, Bucket4j, Spring Security, Servlet filters,
Spring MVC interceptors, WebClient filters, or Feign interceptors. Those belong to
framework adapters and Spring Boot starters.

## Rule Schema

The parser accepts service-validated JSON rule content with a common envelope:

```json
{
  "ruleType": "ROUTE",
  "targetService": "payment-service",
  "status": "ACTIVE",
  "priority": 0
}
```

Supported local rule types:

| Rule type | Required payload |
| --- | --- |
| `ROUTE` | `routes` |
| `CIRCUIT_BREAKER` | `breaker` |
| `RATE_LIMIT` | `limit` |
| `AUTH` | `auth` |
| `DEGRADE` | `degrade` |

`AUTH` is implemented in this SDK for the target governance surface. If the current
`stellnula-service` validator does not yet accept `AUTH`, the control plane must be upgraded
before publishing auth rules through StellNula.

## Spring Boot Starter Plan

The core SDK intentionally stays Spring-free. Spring Boot integration should be split into
four focused starters:

| Starter | Auto-configured capability |
| --- | --- |
| `stellorbit-spring-boot-starter-route` | Route provider plus routing interceptors/adapters |
| `stellorbit-spring-boot-starter-circuit-breaker` | Circuit breaker provider plus Resilience4j adapter |
| `stellorbit-spring-boot-starter-auth` | Authorization provider plus Spring Security or interceptor adapter |
| `stellorbit-spring-boot-starter-rate-limit` | Rate limit provider plus Bucket4j or Resilience4j adapter |

Each starter should depend on this core SDK, bind `StellorbitClientOptions`, and register
only the beans needed by its capability. A later aggregate starter can depend on all four.

## Development

Run verification:

```bash
mvn test
```

## Architecture Decision

See [docs/ADR.md](docs/ADR.md).

## License

The license will be defined before the first stable release.
