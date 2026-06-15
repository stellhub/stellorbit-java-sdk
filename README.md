# StellOrbit Java SDK

`stellorbit-java-sdk` is the Java client SDK for [`stellhub/stellorbit-service`](https://github.com/stellhub/stellorbit-service), the Stell service governance engine responsible for routing, load balancing, retries, traffic shifting, and service lifecycle policies.

## Positioning

This repository provides the Java client implementation for applications, platform services, and middleware components that need to consume StellOrbit governance capabilities.

It does not implement governance rules locally. The SDK delegates policy decisions to `stellorbit-service` and keeps Java applications aligned with the central Stell service governance control plane.

## Capabilities

- Route decision requests for service-to-service traffic.
- Service lifecycle policy lookup.
- Traffic governance policy lookup.
- API key based authentication header support.
- Standard Java `HttpClient` transport without third-party runtime dependencies.
- Timeout configuration for connection and request execution.

## Current Status

| Item | Value |
| --- | --- |
| Stability | Early development |
| Language | Java |
| Minimum Java version | 17 |
| Transport | `java.net.http.HttpClient` |
| Target service | `stellorbit-service` |
| Maintainer | StellHub |

## Installation

The artifact coordinates are reserved for future publishing:

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

import io.github.stellhub.stellorbit.client.StellorbitClient;
import io.github.stellhub.stellorbit.client.StellorbitClientOptions;
import io.github.stellhub.stellorbit.client.StellorbitHttpClient;
import io.github.stellhub.stellorbit.client.model.ApiResponse;
import io.github.stellhub.stellorbit.client.model.RouteRequest;
import java.net.URI;
import java.util.Map;

public class StellorbitExample {

    public static void main(String[] args) {
        StellorbitClientOptions options = StellorbitClientOptions.builder()
                .endpoint(URI.create("http://localhost:8080"))
                .apiKey("local-dev-api-key")
                .build();

        try (StellorbitClient client = new StellorbitHttpClient(options)) {
            RouteRequest request = new RouteRequest(
                    "payment-service",
                    "tenant-a",
                    Map.of("env", "dev", "region", "local")
            );

            ApiResponse response = client.route(request);
            System.out.println(response.body());
        }
    }
}
```

## API Surface

| Method | Responsibility |
| --- | --- |
| `route(RouteRequest request)` | Request a route decision from StellOrbit |
| `lifecyclePolicy(String serviceName)` | Fetch service lifecycle governance policy |
| `trafficPolicy(String serviceName)` | Fetch routing, retry, and traffic shifting policy |

## Development

Run verification:

```bash
mvn test
```

## Repository Scope

This SDK intentionally keeps the first version small. Future releases can add:

- Strongly typed policy models.
- Retry and failover helpers.
- OpenAPI generated DTOs when the service contract is stable.
- Integration tests against local `stellorbit-service`.
- Observability hooks for client-side metrics and tracing.

## License

The license will be defined before the first stable release.
