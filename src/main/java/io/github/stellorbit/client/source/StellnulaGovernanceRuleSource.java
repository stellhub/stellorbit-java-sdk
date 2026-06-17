package io.github.stellorbit.client.source;

import io.github.stellnula.client.StellnulaClient;
import io.github.stellnula.client.StellnulaClientOptions;
import io.github.stellnula.config.StellnulaListenerRegistration;
import io.github.stellnula.config.StellnulaSnapshot;
import io.github.stellorbit.client.StellorbitClientException;
import io.github.stellorbit.client.StellorbitClientOptions;
import io.github.stellorbit.client.rule.GovernanceRuleParser;
import io.github.stellorbit.client.rule.GovernanceRuleRegistry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class StellnulaGovernanceRuleSource implements GovernanceRuleSource {

    private final StellnulaClient client;
    private final GovernanceRuleSnapshotParser snapshotParser;
    private final AtomicReference<GovernanceRuleRegistry> registry =
            new AtomicReference<>(GovernanceRuleRegistry.empty());
    private volatile StellnulaListenerRegistration listenerRegistration;

    public StellnulaGovernanceRuleSource(StellorbitClientOptions options) {
        this(new StellnulaClient(toStellnulaOptions(options)), new GovernanceRuleParser());
    }

    public StellnulaGovernanceRuleSource(StellnulaClient client, GovernanceRuleParser parser) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.snapshotParser = new GovernanceRuleSnapshotParser(parser);
    }

    /**
     * 启动 StellNula 规则源。
     */
    @Override
    public void start() {
        try {
            client.start();
            listenerRegistration = client.listen(event -> replaceRegistry(event.currentSnapshot()), true);
            replaceRegistry(client.snapshot());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new StellorbitClientException("StellNula governance rule source startup was interrupted", ex);
        } catch (Exception ex) {
            throw new StellorbitClientException("failed to start StellNula governance rule source", ex);
        }
    }

    /**
     * 返回当前规则注册表。
     */
    @Override
    public GovernanceRuleRegistry registry() {
        return registry.get();
    }

    /**
     * 关闭 StellNula 规则源。
     */
    @Override
    public void close() {
        StellnulaListenerRegistration registration = listenerRegistration;
        if (registration != null) {
            registration.close();
        }
        client.close();
    }

    private void replaceRegistry(StellnulaSnapshot snapshot) {
        GovernanceRuleRegistry previous = registry.get();
        GovernanceRuleRegistry next = snapshotParser.parse(snapshot, previous);
        registry.set(next);
    }

    private static StellnulaClientOptions toStellnulaOptions(StellorbitClientOptions options) {
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(options.stellnulaEndpoint(), "stellnulaEndpoint must not be null");
        StellnulaClientOptions.Builder builder = StellnulaClientOptions.builder()
                .endpoint(options.stellnulaEndpoint())
                .grpcEndpoint(options.stellnulaGrpcEndpoint())
                .grpcPlaintext(options.stellnulaGrpcPlaintext())
                .apiToken(options.stellnulaApiToken())
                .appId(options.appId())
                .clientId(options.clientId())
                .env(options.env())
                .region(options.region())
                .zone(options.zone())
                .cluster(options.cluster())
                .namespace(options.ruleNamespace())
                .group(options.ruleGroup())
                .watchEnabled(options.watchEnabled())
                .failFastOnBootstrap(options.failFastOnBootstrap());
        if (options.snapshotDirectory() != null) {
            builder.snapshotDirectory(options.snapshotDirectory());
        }
        return builder.build();
    }
}
