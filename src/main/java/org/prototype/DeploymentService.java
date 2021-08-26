package org.prototype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeploymentService {

    private static final Logger LOG = LoggerFactory.getLogger(DeploymentService.class);

    public Payload initiateDeploy(Payload payload) {
        LOG.info("received payload {}",payload);
        return payload;
    }
}
