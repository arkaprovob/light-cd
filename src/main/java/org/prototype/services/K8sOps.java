package org.prototype.services;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta1.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.dsl.Deletable;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.eclipse.microprofile.config.ConfigProvider;
import org.prototype.services.security.RSAUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class K8sOps {

    private static final Logger LOG = LoggerFactory.getLogger(K8sOps.class);

    private final OpenShiftClient openShiftClient;
    private final ManagedResourceWatcher managedResourceWatcher;

    public K8sOps(OpenShiftClient openShiftClient, ManagedResourceWatcher managedResourceWatcher) {
        this.openShiftClient = openShiftClient;
        this.managedResourceWatcher = managedResourceWatcher;
    }


    public boolean businessLogic(Map<String, String> templateParameters, String selectedResourceName,
                                 String namespace, String encryptedRequester) {

        var success = false;
        var requesterName = RSAUtil.deEncrypt(encryptedRequester); //todo

        LOG.info("templateParameters are as follows {} and selectedResourceName is {}",
                templateParameters, selectedResourceName);

        Map<String, String> envConfig = getEnvironmentConfigData(ConfigProvider.getConfig()
                .getValue("ops.environment.configmap.name", String.class), namespace);
        LOG.info("env parameters are as follows {}", envConfig);
        Map<String, String> combinedTemplateParams = Stream.of(templateParameters, envConfig)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
        LOG.info("final list of params are as follows {}", combinedTemplateParams);

        var k8sResourceList
                = openShiftClient
                .templates()
                .inNamespace(namespace)
                .load(K8sOps
                        .class.getResourceAsStream("/openshift/spaship-express-template.yaml"))
                .processLocally(combinedTemplateParams);


        k8sResourceList.getItems().forEach(resource -> {

            var resourceName = resource.getMetadata().getName();

            LOG.info("resource type is {}", resource.getKind());

            if (resource instanceof StatefulSet)
                handleStatefulSet(namespace, (StatefulSet) resource, resourceName, requesterName);
            if (resource instanceof Deployment)
                handleDeployment(selectedResourceName, namespace, (Deployment) resource, resourceName, requesterName);
            if (resource instanceof ConfigMap)
                handleConfigMap(selectedResourceName, namespace, (ConfigMap) resource, resourceName);
            if (resource instanceof Service)
                handleService(namespace, (Service) resource, resourceName);
            if (resource instanceof HorizontalPodAutoscaler)
                handleHPA(selectedResourceName, namespace, (HorizontalPodAutoscaler) resource, resourceName);
            if (resource instanceof Ingress)
                handleIngress(namespace, (Ingress) resource, resourceName);


        });
        success = true;
        return success;
    }

    private Map<String, String> getEnvironmentConfigData(String configMapName, String nameSpace) {

        LOG.info("Looking for configmap {} in namespace {}", configMapName, nameSpace);

        return Optional.ofNullable(openShiftClient.configMaps().inNamespace(nameSpace).withName(configMapName))
                .map(configMap -> configMap.get().getData())
                .orElse(Collections.emptyMap());

    }


    @ConsumeEvent("process.cleanup")
    void cleanupResources(String nameSpace) {
        cleanupResources(nameSpace,"app.kubernetes.io/managed-by","light-cd");
        cleanupResources(nameSpace,"app.kubernetes.io/managed-by","spaship");
    }


    void cleanupResources(String nameSpace,String key, String value) {

        LOG.info("cleaning all resources from  {}", nameSpace);
        var labelFilter = Map.of(key, value);

        Uni.createFrom()
                .item(() -> {
                    boolean dDStatus = Optional.ofNullable(
                                    openShiftClient.apps().deployments().inNamespace(nameSpace).withLabels(labelFilter))
                            .map(Deletable::delete)
                            .orElse(false);

                    boolean ssDStatus = Optional.ofNullable(
                                    openShiftClient.apps().statefulSets().inNamespace(nameSpace).withLabels(labelFilter))
                            .map(Deletable::delete)
                            .orElse(false);

                    boolean hpaDStatus = Optional.ofNullable(
                                    openShiftClient.autoscaling().v2beta1().horizontalPodAutoscalers().inNamespace(nameSpace).withLabels(labelFilter))
                            .map(Deletable::delete)
                            .orElse(false);

                    boolean svcDStatus = Optional.ofNullable(
                                    openShiftClient.services().inNamespace(nameSpace).withLabels(labelFilter))
                            .map(Deletable::delete)
                            .orElse(false);

                    boolean cmDStatus = Optional.ofNullable(
                                    openShiftClient.configMaps().inNamespace(nameSpace).withLabels(labelFilter))
                            .map(Deletable::delete)
                            .orElse(false);

                    boolean icDStatus = Optional.ofNullable(
                                    openShiftClient.network().v1().ingresses().inNamespace(nameSpace).withLabels(labelFilter))
                            .map(Deletable::delete)
                            .orElse(false);
                    return (dDStatus && ssDStatus && hpaDStatus && svcDStatus && cmDStatus && icDStatus);
                })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe()
                .with(result -> LOG.info("cleanup status is {}", result));

    }

    private void handleHPA(String selectedResourceName, String namespace, HorizontalPodAutoscaler resource, String resourceName) {
        LOG.debug("dealing with the HorizontalPodAutoscaler");
        HorizontalPodAutoscaler hpaInK8s = openShiftClient.autoscaling().v2beta1().horizontalPodAutoscalers().inNamespace(namespace).withName(resourceName).get();
        if (Objects.isNull(hpaInK8s)) {
            LOG.debug("HorizontalPodAutoscaler {} doesn't exist creating new ", resourceName);
            openShiftClient.autoscaling().v2beta1().horizontalPodAutoscalers().inNamespace(namespace).createOrReplace(resource);
            LOG.info("HPA {} created successfully ", resourceName);
        } else if (selectedResourceName.equalsIgnoreCase(resourceName)) {
            LOG.info("HPA {} exists and needs to update", resourceName);
            openShiftClient.autoscaling().v2beta1().horizontalPodAutoscalers().inNamespace(namespace).withName(resourceName).delete();
            LOG.info("deleted HPA {}", resourceName);
            openShiftClient.autoscaling().v2beta1().horizontalPodAutoscalers().inNamespace(namespace).createOrReplace(resource);
            LOG.info("re-created HPA {}", resourceName);
        }
    }

    private void handleService(String namespace, Service resource, String resourceName) {
        LOG.debug("dealing with the Service in namespace {}", namespace);
        var serviceInK8s = openShiftClient.services().inNamespace(namespace).withName(resourceName).get();
        if (Objects.isNull(serviceInK8s)) {
            LOG.debug("Service {} doesn't exist creating new ", resourceName);
            openShiftClient.services().inNamespace(namespace).createOrReplace(resource);
            LOG.info("Service {} created successfully ", resourceName);
        }
    }

    private void handleConfigMap(String selectedResourceName, String namespace, ConfigMap resource, String resourceName) {
        LOG.debug("dealing with the ConfigMap");
        var configMapInK8s = openShiftClient.configMaps().inNamespace(namespace).withName(resourceName).get();
        if (Objects.isNull(configMapInK8s)) {
            LOG.debug("ConfigMap {} doesn't exist creating new ", resourceName);
            openShiftClient.configMaps().inNamespace(namespace).createOrReplace(resource);
            LOG.info("ConfigMap {} created successfully ", resourceName);
        } else if (resourceName.contains(selectedResourceName)) {
            LOG.debug("Config Map {} exists and need to update", resourceName);
            openShiftClient.configMaps().inNamespace(namespace).withName(resourceName).delete();
            LOG.debug("deleted Config Map {}", resourceName);
            openShiftClient.configMaps().inNamespace(namespace).createOrReplace(resource);
            LOG.info("re-created Config Map {}", resourceName);
        }
    }

    private void handleDeployment(String selectedResourceName, String namespace, Deployment resource, String resourceName, String requester) {
        LOG.debug("dealing with the Deployment, payload name attribute {}, resource {}", selectedResourceName, resourceName);
        LOG.info("operation triggered by {}",requester);
        var deploymentInK8s = openShiftClient.apps().deployments().inNamespace(namespace).withName(resourceName).get();
        //var status = new DeploymentStatus();
        //status.setAdditionalProperty("executedBy", requester);
        //resource.setStatus(status);

        if (Objects.isNull(deploymentInK8s)) {
            LOG.info("Deployment {} doesn't exist creating new ", resourceName);
            openShiftClient.apps().deployments().inNamespace(namespace).createOrReplace(resource);
            LOG.info("Deployment {} created successfully ", resourceName);
        } else if (selectedResourceName.equalsIgnoreCase(resourceName)) {
            LOG.info("Deployment {} exists and need to update", resourceName);
            openShiftClient.apps().deployments().inNamespace(namespace).withName(resourceName).delete();
            LOG.debug("deleted deployment {}", resourceName);
            openShiftClient.apps().deployments().inNamespace(namespace).createOrReplace(resource);
            openShiftClient.apps().deployments().inNamespace(namespace).withName(resource.getMetadata().getName()).rolling();
            LOG.info("re-created deployment {}", resourceName);
        }
    }

    private void handleStatefulSet(String namespace, StatefulSet resource, String resourceName, String requester) {
        LOG.debug("dealing with the stateful set");
        LOG.info("operation triggered by {}",requester);
        var statefulSetInK8s = openShiftClient.apps().statefulSets().inNamespace(namespace).withName(resourceName).get();
        if (Objects.isNull(statefulSetInK8s)) {
            LOG.debug("StatefulSet {} doesn't exist creating new ", resourceName);
            //var status = new StatefulSetStatus();
            //status.setAdditionalProperty("executedBy", requester);
            //resource.setStatus(status);
            var statefulSet = openShiftClient.apps().statefulSets().inNamespace(namespace).createOrReplace(resource);

            LOG.info("StatefulSet {} created successfully ", resourceName);
            String noOfReplica = Integer.toString(statefulSet.getSpec().getReplicas() - 1);
            managedResourceWatcher.initiatePodWatcher(namespace, Map.of("app", "mongo"), noOfReplica);
        }
    }

    private void handleIngress(String namespace, Ingress resource, String resourceName) {
        LOG.debug("dealing with the ingress in namespace {}", namespace);
        var ingressInK8s = openShiftClient.network().v1().ingresses().inNamespace(namespace).withName(resourceName).get();
        if (Objects.isNull(ingressInK8s)) {
            LOG.debug("Ingress {} doesn't exist creating new ", resourceName);
            openShiftClient.network().v1().ingresses().inNamespace(namespace).createOrReplace(resource);
            LOG.info("Ingress {} created successfully ", resourceName);
        }
    }


}
