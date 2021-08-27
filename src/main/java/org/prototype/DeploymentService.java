package org.prototype;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta1.HorizontalPodAutoscaler;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class DeploymentService {

    private static final Logger LOG = LoggerFactory.getLogger(DeploymentService.class);

    private final OpenShiftClient openShiftClient;

    public DeploymentService(OpenShiftClient openShiftClient) {
        this.openShiftClient = openShiftClient;
    }

    public void initiateDeploy(Payload payload,String namespace) {

        new Thread(() -> {






            LOG.info("received payload {}",payload);
            Map<String, String> templateParameters = Map.of("TAG",payload.updatedTags.get(0),
                    "STORAGE_CLASS", ConfigProvider.getConfig().getValue("template.storage.param.value", String.class));

            LOG.debug("templateParameters are as follows {}",templateParameters);

            var result
                    = openShiftClient
                    .templates()
                    .inNamespace(namespace)
                    .load(DeploymentService
                            .class.getResourceAsStream("/openshift/spaship-express-template.yaml"))
                    .processLocally(templateParameters);

            result.getItems().forEach(resource->{

                var resourceName = resource.getMetadata().getName();

                if(resource instanceof StatefulSet){
                    LOG.debug("dealing with the stateful set");
                    var statefulSetInK8s=openShiftClient.apps().statefulSets().inNamespace(namespace).withName(resourceName).get();
                    if(Objects.isNull(statefulSetInK8s)){
                        LOG.debug("StatefulSet {} doesn't exist creating new ",resourceName);
                        openShiftClient.apps().statefulSets().inNamespace(namespace).createOrReplace((StatefulSet) resource);
                        LOG.debug("StatefulSet {} created successfully ",resourceName);
                    }

                }else if(resource instanceof Deployment){
                    LOG.debug("dealing with the Deployment, payload name attribute {}, resource {}",payload.getName(),resourceName);
                    var deploymentInK8s= openShiftClient.apps().deployments().inNamespace(namespace).withName(resourceName).get();
                    if(Objects.isNull(deploymentInK8s)){
                        LOG.debug("Deployment {} doesn't exist creating new ",resourceName);
                        openShiftClient.apps().deployments().inNamespace(namespace).createOrReplace((Deployment) resource);
                        LOG.debug("Deployment {} created successfully ",resourceName);
                    }else if(payload.getName().equalsIgnoreCase(resourceName)){
                        LOG.debug("Deployment {} exists and need to update",resourceName);
                        openShiftClient.apps().deployments().inNamespace(namespace).withName(resourceName).delete();
                        LOG.debug("deleted deployment {}",resourceName);
                        openShiftClient.apps().deployments().inNamespace(namespace).createOrReplace((Deployment) resource);
                        LOG.debug("re-created deployment {}",resourceName);
                    }


                }else if(resource instanceof ConfigMap){
                    LOG.debug("dealing with the ConfigMap");
                    var configMapInK8s = openShiftClient.configMaps().inNamespace(namespace).withName(resourceName).get();
                    if(Objects.isNull(configMapInK8s)){
                        LOG.debug("ConfigMap {} doesn't exist creating new ",resourceName);
                        openShiftClient.configMaps().inNamespace(namespace).createOrReplace((ConfigMap)resource);
                        LOG.debug("ConfigMap {} created successfully ",resourceName);
                    }

                }else if(resource instanceof Service){
                    LOG.debug("dealing with the Service");
                    var serviceInK8s = openShiftClient.services().inNamespace(namespace).withName(resourceName).get();
                    if(Objects.isNull(serviceInK8s)){
                        LOG.debug("Service {} doesn't exist creating new ",resourceName);
                        openShiftClient.services().inNamespace(namespace).createOrReplace((Service) resource);
                        LOG.debug("Service {} created successfully ",resourceName);
                    }

                }else if(resource instanceof HorizontalPodAutoscaler){
                    LOG.debug("dealing with the HorizontalPodAutoscaler");
                    HorizontalPodAutoscaler hpaInK8s = openShiftClient.autoscaling().v2beta1().horizontalPodAutoscalers().inNamespace(namespace).withName(resourceName).get();
                    if(Objects.isNull(hpaInK8s)){
                        LOG.debug("HorizontalPodAutoscaler {} doesn't exist creating new ",resourceName);
                        openShiftClient.autoscaling().v2beta1().horizontalPodAutoscalers().inNamespace(namespace).createOrReplace((HorizontalPodAutoscaler) resource);
                        LOG.debug("HPA {} created successfully ",resourceName);
                    }

                }

            });











        }).start();




    }
}
