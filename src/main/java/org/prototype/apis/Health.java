package org.prototype.apis;

import io.vertx.core.json.JsonObject;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Path("/health")
public class Health {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response health(@HeaderParam("auth") String requester) {
        return Response.ok(new JsonObject().put("status", "up").encodePrettily(), MediaType.APPLICATION_JSON)
                .header("source", "light-cd")
                .expires(Date.from(Instant.now().plus(Duration.ofSeconds(30))))
                .build();
    }


}
