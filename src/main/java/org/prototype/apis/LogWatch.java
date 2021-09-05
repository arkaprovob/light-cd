package org.prototype.apis;

import io.smallrye.mutiny.Uni;
import org.prototype.services.LogWatcher;

import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/pods")
public class LogWatch {

    private final LogWatcher logWatcher;
    private final String nameSpace;

    public LogWatch(LogWatcher logWatcher, @Named("namespace") String nameSpace) {
        this.logWatcher = logWatcher;
        this.nameSpace = nameSpace;
    }

    @GET
    @Path("/watch/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> cleanupK8SNameSpace(@PathParam("name") String podName, @QueryParam("line") String line) {
        return logWatcher.watch(nameSpace, podName, Integer.parseInt(line));
    }

    @GET
    @Path("/list")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<List<String>> cleanupK8SNameSpace() {
        return logWatcher.listOfPods(nameSpace);
    }


}
