package org.prototype;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
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
    String defaultNamespace() throws IOException {
        String computedNameSpace = Optional
                .of(new String
                        (Files.readAllBytes(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace")))
                ).orElse(nameSpaceFromEnv
                        .orElse("default")
                );
        LOG.info("computed namespace is {}",computedNameSpace);
        return computedNameSpace;
    }

    @Produces
    public KubernetesClient openshiftClient() {
        return new DefaultOpenShiftClient();
    }

}
