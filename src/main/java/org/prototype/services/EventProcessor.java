package org.prototype.services;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.core.eventbus.Message;
import org.eclipse.microprofile.config.ConfigProvider;
import org.prototype.exception.CheckedException;
import org.prototype.type.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class EventProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(EventProcessor.class);
    private final K8sOps k8sOps;

    public EventProcessor(K8sOps k8sOps) {
        this.k8sOps = k8sOps;
    }

    @ConsumeEvent("process.deployment")
    public void processDeploymentEvent(Message<Payload> event) {

        Payload payload = event.body();
        String namespace = payload.getK8sNameSpace();

        Uni.createFrom().item(payload)
                .emitOn(Infrastructure.getDefaultExecutor())
                .map(this::buildTemplateParameters)
                .map(item -> k8sOps.businessLogic(item, payload.getName(), namespace, payload.getRequester()))
                .subscribe()
                .with(consumer -> LOG.info("deployment success status {}", consumer));
    }


    private Map<String, String> buildTemplateParameters(Payload payload) {
        if (Objects.isNull(payload))
            throw new CheckedException("payload not found!");
        var nameInCaps = payload.getName().toUpperCase();
        var tagNameAttribute = "TAG".concat("_").concat(nameInCaps);
        var repoAttribute = nameInCaps.concat("_REPOSITORY");

        var output = Map.of("TAG", payload.getUpdatedTags().get(0),
                "STORAGE_CLASS", ConfigProvider.getConfig()
                        .getValue("template.storage.param.value", String.class),
                "API_URL", ConfigProvider.getConfig()
                        .getValue("template.api.url.param.value", String.class),
                "UI_MEMORY_LIMIT", ConfigProvider.getConfig()
                        .getValue("template.ui.memory.limit", String.class),
                "DOMAIN", ConfigProvider.getConfig().getValue("template.domain", String.class),
                tagNameAttribute, payload.getUpdatedTags().get(0),
                repoAttribute, payload.getDockerUrl(),
                "APP_INSTANCE", ConfigProvider.getConfig()
                        .getValue("app.instance", String.class)
        );
        LOG.info("buildTemplateParameters output is {}", output);
        return output;
    }


}
