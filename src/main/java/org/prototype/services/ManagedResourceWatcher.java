package org.prototype.services;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.openshift.client.OpenShiftClient;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ManagedResourceWatcher {


    private static final Logger LOG = LoggerFactory.getLogger(K8sOps.class);

    private final OpenShiftClient openShiftClient;


    public ManagedResourceWatcher(OpenShiftClient openShiftClient) {
        this.openShiftClient = openShiftClient;
    }

    public void initiatePodWatcher(String nameSpace, Map<String,String> filter){
        openShiftClient.pods().inNamespace(nameSpace).withLabels(filter).watch(new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod resource) {
                var podName = resource.getMetadata().getName();
                LOG.debug("action {} performed on pod {}",action.name(),podName);
                var podReadyStatus = openShiftClient.pods().inNamespace(nameSpace).withName(podName).isReady();
                LOG.debug("pod {} is-ready status {}",podName,podReadyStatus);
                if(podName.equals("mongo-2") && podReadyStatus){
                    LOG.debug("All mongodb cluster pods are ready");
                    try {
                        initiateMongoReplicaSet(nameSpace);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    LOG.debug("executed the rs initiate command successfully");

                }
            }

            @Override
            public void onClose(WatcherException cause) {
                LOG.error("closing watcher due to {}",cause.getCause().getMessage());
            }
        });
    }

    public void initiateMongoReplicaSet(String nameSpace) throws InterruptedException {
        final CountDownLatch execLatch = new CountDownLatch(1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var execWatch=openShiftClient
                .pods()
                .inNamespace(nameSpace)
                .withName("mongo-0")
                .inContainer("mongo")
                .writingOutput(out)
                .withTTY()
                .usingListener(new ExecListener() {
                    @Override
                    public void onOpen(Response response) {
                        try {
                            var responseBody = response.body().string();
                            LOG.debug("ExecListener on-open response is {}",responseBody);
                        } catch (IOException e) {
                            LOG.error("error in ExecListener's onOpen method cause {}",e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Throwable t, Response response) {
                        try {
                            var responseBody = response.body().string();
                            LOG.debug("ExecListener onFailure executed, reason {} and response {}",t.getMessage(),responseBody);
                        } catch (IOException e) {
                            LOG.error("error in ExecListener's onOpen method",e);
                        }
                    }

                    @Override
                    public void onClose(int code, String reason) {
                        LOG.debug("ExecListener onClose code {}, reason {}",code,reason);
                    }
                }).exec("mongo","--eval","'rs.initiate()'");

        execLatch.await(5, TimeUnit.SECONDS);
        execWatch.close();
        LOG.debug("out is {}",out);
    }


}
