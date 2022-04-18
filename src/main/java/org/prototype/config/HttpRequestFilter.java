package org.prototype.config;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.microprofile.config.ConfigProvider;
import org.prototype.services.security.AuthenticationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class HttpRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestFilter.class);
    private static final AuthenticationRepository AUTHENTICATION_REPOSITORY = new AuthenticationRepository();

    @RouteFilter
    void authFilter(RoutingContext rc) {

        if (rc.request().absoluteURI().contains("health") || rc.request().path().equals("/")) {
            rc.response().putHeader("X-Header", "free hit");
            rc.next();
            return;
        }
        rc.response().putHeader("X-Header", UUID.randomUUID().toString());
        var apiKey = Optional.ofNullable(rc.request().getHeader(
                ConfigProvider.getConfig().getValue("app.security.header", String.class)
        )).orElse("NF");


        rc.request().headers().add("auth", apiKey);

        var apiKeys = new ArrayList<>(AUTHENTICATION_REPOSITORY.getApiKeys().values());


        LOG.debug("listed api keys are as follows {} ", apiKeys);
        if (apiKey.equals("NF") || apiKey.isEmpty() || apiKey.isBlank()) {
            rc.response().setStatusCode(401).putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("alert", "key not found in the request").encodePrettily());
            return;
        }

        var match = apiKeys.stream().anyMatch(entry -> {
            LOG.debug("matching {} with {}", entry, apiKey);
            return entry.equals(apiKey);
        });
        if (!match) {
            rc.response().setStatusCode(401).putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("alert", "reported unauthorised access").encodePrettily());
            return;
        }

        rc.next();


    }
}
