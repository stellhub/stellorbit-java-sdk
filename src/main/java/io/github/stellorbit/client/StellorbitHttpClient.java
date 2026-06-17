package io.github.stellorbit.client;

import io.github.stellorbit.client.internal.Jsons;
import io.github.stellorbit.client.model.ApiResponse;
import io.github.stellorbit.client.model.RouteRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class StellorbitHttpClient implements StellorbitRemoteClient {

    private static final String API_KEY_HEADER = "X-Stellorbit-Api-Key";

    private final StellorbitClientOptions options;
    private final HttpClient httpClient;

    public StellorbitHttpClient(StellorbitClientOptions options) {
        this.options = Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(options.endpoint(), "endpoint must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(options.connectTimeout())
                .build();
    }

    /**
     * 请求服务路由决策。
     */
    @Override
    public ApiResponse route(RouteRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        HttpRequest httpRequest = requestBuilder("/api/stellorbit/v1/routes/decide")
                .POST(HttpRequest.BodyPublishers.ofString(Jsons.routeRequest(request)))
                .header("Content-Type", "application/json")
                .build();
        return send(httpRequest);
    }

    /**
     * 查询指定服务的生命周期治理策略。
     */
    @Override
    public ApiResponse lifecyclePolicy(String serviceName) {
        HttpRequest httpRequest = requestBuilder("/api/stellorbit/v1/services/" + encode(serviceName) + "/lifecycle-policy")
                .GET()
                .build();
        return send(httpRequest);
    }

    /**
     * 查询指定服务的流量治理策略。
     */
    @Override
    public ApiResponse trafficPolicy(String serviceName) {
        HttpRequest httpRequest = requestBuilder("/api/stellorbit/v1/services/" + encode(serviceName) + "/traffic-policy")
                .GET()
                .build();
        return send(httpRequest);
    }

    /**
     * 关闭客户端资源。
     */
    @Override
    public void close() {
        // java.net.http.HttpClient does not require explicit shutdown.
    }

    private HttpRequest.Builder requestBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(resolve(path))
                .timeout(options.requestTimeout())
                .header("Accept", "application/json")
                .header("User-Agent", "stellorbit-java-sdk");
        if (options.apiKey() != null && !options.apiKey().isBlank()) {
            builder.header(API_KEY_HEADER, options.apiKey());
        }
        return builder;
    }

    private ApiResponse send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new StellorbitClientException("failed to call StellOrbit service", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StellorbitClientException("StellOrbit request was interrupted", e);
        }
    }

    private URI resolve(String path) {
        String base = options.endpoint().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return URI.create(base + path);
    }

    private String encode(String value) {
        Objects.requireNonNull(value, "value must not be null");
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
