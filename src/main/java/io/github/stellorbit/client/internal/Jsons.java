package io.github.stellorbit.client.internal;

import io.github.stellorbit.client.model.RouteRequest;
import java.util.Map;

public final class Jsons {

    private Jsons() {
    }

    public static String routeRequest(RouteRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        field(builder, "serviceName", request.serviceName());
        builder.append(',');
        field(builder, "routeKey", request.routeKey());
        builder.append(',');
        builder.append("\"attributes\":");
        map(builder, request.attributes());
        builder.append('}');
        return builder.toString();
    }

    private static void field(StringBuilder builder, String name, String value) {
        builder.append('"').append(escape(name)).append("\":");
        if (value == null) {
            builder.append("null");
            return;
        }
        builder.append('"').append(escape(value)).append('"');
    }

    private static void map(StringBuilder builder, Map<String, String> values) {
        builder.append('{');
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            field(builder, entry.getKey(), entry.getValue());
            first = false;
        }
        builder.append('}');
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
