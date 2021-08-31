package org.prototype;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class DeploymentTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/health")
                .then()
                .statusCode(200)
                .body(is(new JsonObject().put("status","up").encodePrettily()));
    }

}