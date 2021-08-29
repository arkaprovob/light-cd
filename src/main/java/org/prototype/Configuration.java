package org.prototype;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

@Singleton
public class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    @ConfigProperty(name = "application.k8s.namespace")
    Optional<String> nameSpaceFromEnv;

    @Produces
    @Named("namespace")
    String getNamespace(){
        String computedNameSpace = Optional.ofNullable(readNameFromFile()).orElse(whenNameSpaceMetaFileNotFound());
        LOG.info("computed namespace is {}", computedNameSpace);
        return computedNameSpace;
    }

    private String readNameFromFile() {
        String ns;
        try {
            ns = new String(Files
                    .readAllBytes(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace")));
        } catch (IOException e) {
            LOG.warn("failed to read namespace from metadata {}", e.getMessage());
            ns = null;
        }
        return ns;
    }

    private String whenNameSpaceMetaFileNotFound() {
        LOG.debug("namespace file does not exists.");
        return nameSpaceFromEnv.orElseGet(() -> {
            LOG.debug("namespace not on environment either! proceedi");
            return "default";
        });

    }

    @Produces
    public OpenShiftClient openshiftClient() {
        return new DefaultOpenShiftClient();
    }

}
