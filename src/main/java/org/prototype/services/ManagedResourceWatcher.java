package org.prototype.services;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.openshift.client.OpenShiftClient;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
//todo check why is not it working
public class ManagedResourceWatcher {


    private static final Logger LOG = LoggerFactory.getLogger(ManagedResourceWatcher.class);
    private static final String CONTAINER_NAME = "mongo";
    private final OpenShiftClient openShiftClient;
    private Watch watch = null;


    public ManagedResourceWatcher(OpenShiftClient openShiftClient) {
        this.openShiftClient = openShiftClient;
    }

    public void initiatePodWatcher(String nameSpace, Map<String, String> filter, String noOfPods) {

        String lastMongoPodName = "mongo-".concat(noOfPods);
        LOG.info("lastMongoPodName {}", lastMongoPodName);
        if (Objects.nonNull(watch)) {
            LOG.warn("watcher already exists closing it to start a new");
            watch.close();
            watch = null;
        }
        watch = openShiftClient.pods().inNamespace(nameSpace).withLabels(filter).watch(podWatcher(nameSpace, lastMongoPodName));
    }

    private Watcher<Pod> podWatcher(String nameSpace, String lastMongoPodName) {
        return new Watcher<>() {
            @Override
            public void eventReceived(Action action, Pod resource) {
                var podName = resource.getMetadata().getName();
                var actionName = action.name();
                var podReadyStatus = openShiftClient.pods().inNamespace(nameSpace).withName(podName).isReady();
                LOG.debug("pod {} action {} is-ready status {}", podName, actionName, podReadyStatus);
                if (!podReadyStatus) {
                    LOG.debug("pod is not ready.. skipping this event");
                    return;
                }
                initiateMongoReplicaSet(nameSpace, resource.getMetadata().getName());
                if (podName.equals(lastMongoPodName)) {
                    LOG.info("all mongo pods are ready");
                }
            }

            @Override
            public void onClose(WatcherException cause) {
                LOG.error("closing watcher due to {}", cause.getCause().getMessage());
            }
        };
    }

    public void initiateMongoReplicaSet(String nameSpace, String podName) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Uni.createFrom()
                .nullItem()
                .emitOn(Infrastructure.getDefaultExecutor())
                .onItem().delayIt().by(Duration.ofMillis(3000))
                .map(ocClient -> {
                    var mongoContainerCount = openShiftClient.pods().inNamespace(nameSpace).withName(podName)
                            .get().getSpec().getContainers().stream()
                            .filter(container -> container.getName().equalsIgnoreCase(CONTAINER_NAME))
                            .count();
                    return mongoContainerCount > 0;
                }).map(container -> {
                    if (Boolean.FALSE.equals(container)) {
                        return "container doesn't exists hence skipping rs.initiate() exec operation";
                    } else {
                        LOG.debug("Initiating rsinit for pod {}", podName);
                        openShiftClient
                                .pods()
                                .inNamespace(nameSpace)
                                .withName(podName)
                                .inContainer(CONTAINER_NAME)
                                .writingOutput(out)
                                .withTTY()
                                .usingListener(execListener()).exec("mongo", "--eval", "'rs.initiate()'").close();
                        return "rs.initiate() invoked on pod".concat(podName);
                    }
                }).subscribe()
                .with(LOG::info, failure -> LOG.error("failed to execute rs.initiate() due to {} ", failure.getMessage()));


    }

    private ExecListener execListener() {
        return new ExecListener() {
            @Override
            public void onOpen(Response response) {
                try {
                    var responseBody = response.body() != null ? response.body().string() : "none";
                    LOG.debug("ExecListener on-open response is {}", responseBody);
                    LOG.debug("executed the rs initiate command successfully");
                } catch (IOException e) {
                    LOG.error("error in ExecListener's onOpen method cause {}", e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t, Response response) {
                LOG.info("ExecListener onFailure executed, reason {}", t.getMessage());
            }

            @Override
            public void onClose(int code, String reason) {
                LOG.info("ExecListener onClose code {}, reason {}", code, reason);
            }
        };
    }


}
