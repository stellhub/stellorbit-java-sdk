package io.github.stellorbit.client.model;

import java.util.Map;
import java.util.Objects;

public record RouteRequest(
        String serviceName, String routeKey, Map<String, String> attributes, RequestContext context) {

    public RouteRequest {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        context = context == null ? RequestContext.empty() : context;
    }

    public RouteRequest(String serviceName, String routeKey, Map<String, String> attributes) {
        this(serviceName, routeKey, attributes, RequestContext.empty());
    }
}
