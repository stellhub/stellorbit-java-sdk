package io.github.stellhub.stellorbit.client;

import io.github.stellhub.stellorbit.client.model.ApiResponse;
import io.github.stellhub.stellorbit.client.model.RouteRequest;

public interface StellorbitClient extends AutoCloseable {

    /**
     * 请求服务路由决策。
     */
    ApiResponse route(RouteRequest request);

    /**
     * 查询指定服务的生命周期治理策略。
     */
    ApiResponse lifecyclePolicy(String serviceName);

    /**
     * 查询指定服务的流量治理策略。
     */
    ApiResponse trafficPolicy(String serviceName);

    /**
     * 关闭客户端资源。
     */
    @Override
    void close();
}
