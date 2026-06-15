package io.github.stellhub.stellorbit.client.model;

import java.util.Map;
import java.util.Objects;

public record RouteRequest(String serviceName, String routeKey, Map<String, String> attributes) {

    public RouteRequest {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
