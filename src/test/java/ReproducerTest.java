import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, timeUnit = TimeUnit.SECONDS)
public class ReproducerTest {
    private static final String HTTP_SERVICE_NAME = "some-http-service";
    private ServiceDiscovery discovery;
    private final Record httpRecord = HttpEndpoint.createRecord(HTTP_SERVICE_NAME, "localhost", 8081, "/");

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext context) {
        Checkpoint init = context.checkpoint(1);
        this.discovery = ServiceDiscovery.create(vertx,
                new ServiceDiscoveryOptions()
                        .setAnnounceAddress("service-announce")
                        .setName("my-name"));
        discovery.publish(httpRecord, result -> {
            if (result.failed()) {
                System.out.println("Something went wrong");
                context.failNow(result.cause());
            }
            init.flag();
        });
    }

    @Test
    void reproduce(VertxTestContext context) {
        HttpEndpoint.getWebClient(discovery, new JsonObject().put("name", HTTP_SERVICE_NAME),
                clientResult -> {
                    if (clientResult.failed()) {
                        context.failNow(clientResult.cause());
                    }
                    context.verify(() -> {
                        WebClient client = clientResult.result();
                        System.out.println(client.getClass());
                        context.completeNow();
                    });
                });
    }
}
