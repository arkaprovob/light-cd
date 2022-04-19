package org.prototype.apis;

import io.fabric8.openshift.client.OpenShiftClient;
import io.smallrye.common.annotation.Blocking;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/mt")
public class MultiTenancy {

    @Inject
    private  OpenShiftClient openShiftClient;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    @Blocking
    public String getNoOfPods(String nameSpace){

        return String.valueOf(openShiftClient.pods().inNamespace(nameSpace).list().getItems().size());

    }


}
