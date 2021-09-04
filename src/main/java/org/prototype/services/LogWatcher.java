package org.prototype.services;

import io.fabric8.openshift.client.OpenShiftClient;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class LogWatcher {


    private static final Logger LOG = LoggerFactory.getLogger(LogWatcher.class);

    private final OpenShiftClient openShiftClient;


    public LogWatcher(OpenShiftClient openShiftClient) {
        this.openShiftClient = openShiftClient;
    }

    Map<String,String> podLabelFilter = Map.of("app.kubernetes.io/managed-by","spaship");


    public Uni<String> watch(String nameSpace, String podName, int tailingLines){
        LOG.debug("namespace {}, podName {}, lines {}",nameSpace,podName,tailingLines);

        return Uni.createFrom().item(()->openShiftClient.pods()
                .inNamespace(nameSpace).withName(podName)
                .tailingLines(tailingLines).withPrettyOutput()
                .getLog(true))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .onFailure()
                .recoverWithItem(Throwable::getLocalizedMessage);

        //.watchLog(System.out);
    }


    public Uni<List<String>> listOfPods(String nameSpace){
        return Uni.createFrom().item(nameSpace)
                .emitOn(Infrastructure.getDefaultExecutor())
                .map(
                        ns-> openShiftClient.pods().inNamespace(nameSpace).withLabel("app.kubernetes.io/name").list().getItems().stream()
                        .map(pod -> pod.getMetadata().getName()).collect(Collectors.toList())
        );

    }
}
