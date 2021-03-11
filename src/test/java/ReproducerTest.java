import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.qe.ServiceDiscoveryVerticle;
import io.vertx.qe.SomeHttpVerticle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

@ExtendWith(VertxExtension.class)
public class ReproducerTest {
    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext vertxTestContext){
        final Checkpoint deploySDcheck = vertxTestContext.checkpoint(2);
        vertx.deployVerticle(ServiceDiscoveryVerticle.class.getName(), vertxTestContext.succeeding(id-> {
            await("Await until route /health is ready!")
                    .until(() -> given().port(8080).body("{}").get("/health").getStatusCode() == 200);
            deploySDcheck.flag();
        }));
        vertx.deployVerticle(SomeHttpVerticle.class.getName(), vertxTestContext.succeeding(id-> {
            await("Await until route /health is ready!")
                    .until(() -> given().port(8081).body("{}").get("/health").getStatusCode() == 200);
            deploySDcheck.flag();
        }));
    }

    @Test
    void reproduce(VertxTestContext vertxTestContext) {
        Checkpoint statusCheck = vertxTestContext.checkpoint();
        vertxTestContext.verify(()->{
            Response response = given().port(8080).get("/http-service");
            Assertions.assertEquals(200, response.getStatusCode());
            JsonPath body = response.body().jsonPath();
            Assertions.assertEquals("OK", body.get("httpResponse"));
            Assertions.assertEquals("UP", body.get("recordStatus"));
            statusCheck.flag();
        });
    }
}
