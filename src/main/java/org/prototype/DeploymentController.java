package org.prototype;

import io.vertx.mutiny.core.eventbus.EventBus;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/deploy")
public class DeploymentController {
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentController.class);

    private final String tag;
    private final String nameSpace;
    private final EventBus bus;

    public DeploymentController(EventBus bus, @Named("namespace") String nameSpace) {
        this.bus = bus;
        this.tag = ConfigProvider.getConfig().getValue("validation.trigger.tag", String.class);
        this.nameSpace = nameSpace;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String deploy(Payload payload) {
        if (!payload.tagExists(tag)) {
            LOG.info("incoming event is not a part of this instance");
            return "skipped";
        }
        payload.setK8sNameSpace(nameSpace);
        bus.send("process.deployment", payload);
        return "accepted";
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public String cleanupK8SNameSpace() {
        bus.send("process.cleanup", nameSpace);
        return "accepted";
    }


}