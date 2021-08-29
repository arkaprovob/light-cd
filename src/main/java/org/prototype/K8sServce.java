package org.prototype;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta1.HorizontalPodAutoscaler;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.core.eventbus.Message;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class K8sServce {

    private static final Logger LOG = LoggerFactory.getLogger(K8sServce.class);

    private final OpenShiftClient openShiftClient;

    public K8sServce(OpenShiftClient openShiftClient) {
        this.openShiftClient = openShiftClient;
    }

    @ConsumeEvent("process.deployment")
    public void processDeploymentEvent(Message<Payload> event) {

        Payload payload = event.body();
        String namespace = payload.getK8sNameSpace();

        Uni.createFrom().item(payload)
                .emitOn(Infrastructure.getDefaultExecutor())
                .map(item -> businessLogic(item, namespace))
                .subscribe()
                .with(consumer -> LOG.info("deployment success status {}", consumer));
    }

    private boolean businessLogic(Payload payload, String namespace) {

        var success = false;

        LOG.info("received payload {}", payload);
        Map<String, String> templateParameters = Map.of("TAG", payload.updatedTags.get(0),
                "STORAGE_CLASS", ConfigProvider.getConfig().getValue("template.storage.param.value", String.class));

        LOG.debug("templateParameters are as follows {}", templateParameters);

        var k8sResourceList
                = openShiftClient
                .templates()
                .inNamespace(namespace)
                .load(K8sServce
                        .class.getResourceAsStream("/openshift/spaship-express-template.yaml"))
                .processLocally(templateParameters);


        k8sResourceList.getItems().forEach(resource -> {

            var resourceName = resource.getMetadata().getName();

            if (resource instanceof StatefulSet)
                handleStatefulSet(namespace, (StatefulSet) resource, resourceName);
            if (resource instanceof Deployment)
                handleDeployment(payload, namespace, (Deployment) resource, resourceName);
            if (resource instanceof ConfigMap)
                handleConfigMap(namespace, (ConfigMap) resource, resourceName);
            if (resource instanceof Service)
                handleService(namespace, (Service) resource, resourceName);
            if (resource instanceof HorizontalPodAutoscaler)
                handleHPA(namespace, (HorizontalPodAutoscaler) resource, resourceName);


        });
        success = true;
        return success;
    }



    @ConsumeEvent("process.cleanup")
    void cleanupResources(String nameSpace){

        LOG.info("cleaning all resources from  {}", nameSpace);

        Uni.createFrom()
                .item(()->{
                    boolean dDStatus = openShiftClient.apps().deployments().inNamespace(nameSpace).delete();
                    boolean ssDStatus = openShiftClient.apps().statefulSets().inNamespace(nameSpace).delete();
                    boolean hpaDStatus = openShiftClient.autoscaling().v2beta1().horizontalPodAutoscalers()
                            .inNamespace(nameSpace).delete();
                    boolean svcDStatus = openShiftClient.services().inNamespace(nameSpace).delete();
                    boolean cmDStatus = openShiftClient.configMaps().inNamespace(nameSpace).delete();
                    return (dDStatus && ssDStatus && hpaDStatus && svcDStatus && cmDStatus);
                })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe()
                .with(result -> LOG.info("cleanup status is {}",result));

    }

    private void handleHPA(String namespace, HorizontalPodAutoscaler resource, String resourceName) {
        LOG.debug("dealing with the HorizontalPodAutoscaler");
        HorizontalPodAutoscaler hpaInK8s = openShiftClient.autoscaling().v2beta1().horizontalPodAutoscalers().inNamespace(namespace).withName(resourceName).get();
        if (Objects.isNull(hpaInK8s)) {
            LOG.debug("HorizontalPodAutoscaler {} doesn't exist creating new ", resourceName);
            openShiftClient.autoscaling().v2beta1().horizontalPodAutoscalers().inNamespace(namespace).createOrReplace(resource);
            LOG.debug("HPA {} created successfully ", resourceName);
        }
    }

    private void handleService(String namespace, Service resource, String resourceName) {
        LOG.debug("dealing with the Service in namespace {}", namespace);
        var serviceInK8s = openShiftClient.services().inNamespace(namespace).withName(resourceName).get();
        if (Objects.isNull(serviceInK8s)) {
            LOG.debug("Service {} doesn't exist creating new ", resourceName);
            openShiftClient.services().inNamespace(namespace).createOrReplace(resource);
            LOG.debug("Service {} created successfully ", resourceName);
        }
    }

    private void handleConfigMap(String namespace, ConfigMap resource, String resourceName) {
        LOG.debug("dealing with the ConfigMap");
        var configMapInK8s = openShiftClient.configMaps().inNamespace(namespace).withName(resourceName).get();
        if (Objects.isNull(configMapInK8s)) {
            LOG.debug("ConfigMap {} doesn't exist creating new ", resourceName);
            openShiftClient.configMaps().inNamespace(namespace).createOrReplace(resource);
            LOG.debug("ConfigMap {} created successfully ", resourceName);
        }
    }

    private void handleDeployment(Payload payload, String namespace, Deployment resource, String resourceName) {
        LOG.debug("dealing with the Deployment, payload name attribute {}, resource {}", payload.getName(), resourceName);
        var deploymentInK8s = openShiftClient.apps().deployments().inNamespace(namespace).withName(resourceName).get();
        if (Objects.isNull(deploymentInK8s)) {
            LOG.debug("Deployment {} doesn't exist creating new ", resourceName);
            openShiftClient.apps().deployments().inNamespace(namespace).createOrReplace(resource);
            LOG.debug("Deployment {} created successfully ", resourceName);
        } else if (payload.getName().equalsIgnoreCase(resourceName)) {
            LOG.debug("Deployment {} exists and need to update", resourceName);
            openShiftClient.apps().deployments().inNamespace(namespace).withName(resourceName).delete();
            LOG.debug("deleted deployment {}", resourceName);
            openShiftClient.apps().deployments().inNamespace(namespace).createOrReplace(resource);
            LOG.debug("re-created deployment {}", resourceName);
        }
    }

    private void handleStatefulSet(String namespace, StatefulSet resource, String resourceName) {
        LOG.debug("dealing with the stateful set");
        var statefulSetInK8s = openShiftClient.apps().statefulSets().inNamespace(namespace).withName(resourceName).get();
        if (Objects.isNull(statefulSetInK8s)) {
            LOG.debug("StatefulSet {} doesn't exist creating new ", resourceName);
            openShiftClient.apps().statefulSets().inNamespace(namespace).createOrReplace(resource);
            LOG.debug("StatefulSet {} created successfully ", resourceName);
        }
    }
}
