package org.prototype.apis;

import io.vertx.mutiny.core.eventbus.EventBus;
import org.eclipse.microprofile.config.ConfigProvider;
import org.prototype.type.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/deploy")
public class Deployment {
    private static final Logger LOG = LoggerFactory.getLogger(Deployment.class);

    private final String tag;
    private final String nameSpace;
    private final EventBus bus;

    public Deployment(EventBus bus, @Named("namespace") String nameSpace) {
        this.bus = bus;
        this.tag = ConfigProvider.getConfig().getValue("validation.trigger.tag", String.class);
        this.nameSpace = nameSpace;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String deploy(Payload payload) {
        if (!tag.equals("*") && !payload.tagExists(tag)) {
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