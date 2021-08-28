package org.prototype;

import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/deploy")
public class DeploymentController {
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentController.class);

    private final DeploymentService deploymentService;
    private final String tag;
    private final String nameSPace;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
        this.tag = ConfigProvider.getConfig().getValue("validation.trigger.tag", String.class);
        this.nameSPace = ConfigProvider.getConfig().getValue("application.k8s.namespace", String.class);
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String deploy(Payload payload) {
        if (!payload.tagExists(tag)) {
            LOG.info("incoming event is not a part of this instance");
            return "skipped";
        }
        deploymentService.initiateDeploy(payload, nameSPace);
        return "accepted";
    }
}