package io.github.stellorbit.client.provider;

import io.github.stellorbit.client.model.RateLimitRuleQuery;
import io.github.stellorbit.client.rule.GovernanceRule;
import io.github.stellorbit.client.rule.RateLimitRules;
import java.util.List;
import java.util.Optional;

public interface RateLimitRuleProvider {

    /**
     * 查询匹配的限流规则。
     */
    List<GovernanceRule> find(RateLimitRuleQuery query);

    /**
     * 查询第一个匹配的限流规则。
     */
    default Optional<GovernanceRule> first(RateLimitRuleQuery query) {
        return find(query).stream().findFirst();
    }

    /**
     * 查询需要分布式限流链路执行的规则。
     */
    default List<GovernanceRule> distributed(RateLimitRuleQuery query) {
        return find(query).stream()
                .filter(RateLimitRules::isDistributedRule)
                .toList();
    }

    /**
     * 查询应由本地运行时执行的规则。
     */
    default List<GovernanceRule> localRuntime(RateLimitRuleQuery query) {
        return find(query).stream()
                .filter(RateLimitRules::isLocalRuntimeRule)
                .toList();
    }

    /**
     * 查询 HTTP Header 限流规则。
     */
    default List<GovernanceRule> httpHeader(RateLimitRuleQuery query) {
        return find(query
                        .withLimitMode(RateLimitRules.LIMIT_MODE_HEADER)
                        .withKeyExtractorSource(RateLimitRules.KEY_EXTRACTOR_SOURCE_HEADER))
                .stream()
                .filter(RateLimitRules::usesHttpHeaderExtractor)
                .toList();
    }

    /**
     * 查询 gRPC Metadata 限流规则。
     */
    default List<GovernanceRule> grpcMetadata(RateLimitRuleQuery query) {
        return find(query
                        .withLimitMode(RateLimitRules.LIMIT_MODE_HEADER)
                        .withKeyExtractorSource(RateLimitRules.KEY_EXTRACTOR_SOURCE_GRPC_METADATA))
                .stream()
                .filter(RateLimitRules::usesGrpcMetadataExtractor)
                .toList();
    }
}
