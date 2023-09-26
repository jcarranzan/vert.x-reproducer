import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class BodyRequiredIT {

  @BeforeAll
  public static void deployServer(Vertx vertx, VertxTestContext vertxTestContext) {
    Checkpoint deployCheck = vertxTestContext.checkpoint();
    vertxTestContext.verify(()->{
      vertx.deployVerticle(Server.class.getName());
      deployCheck.flag();
    });
  }
  @Test
  void testRequestBodyRequiredTest(Vertx vertx, VertxTestContext testContext) {
    testContext.verify(()->{
      vertx.createHttpClient()
        .request(HttpMethod.POST, 8080, "localhost", "/user")
        .compose(HttpClientRequest::send)
        .onSuccess(httpClientResponse -> {
          System.out.println("STATUS RESPONSE CODE " + httpClientResponse.statusCode());
          assertEquals(400, httpClientResponse.statusCode());
          testContext.completeNow();
        });
    });
  }
  @Test
  void emptyStringAsBodyRequiredTest(Vertx vertx, VertxTestContext testContext) {
    testContext.verify(() -> {
      vertx.createHttpClient()
        .request(HttpMethod.POST, 8080, "localhost", "/user")
        .compose(request -> {
          request.putHeader("Content-Length", "0");
          return request.send();
        })
        .onSuccess(httpClientResponse -> {
          System.out.println("STATUS RESPONSE CODE " + httpClientResponse.statusCode());
          assertEquals(200, httpClientResponse.statusCode());
          testContext.completeNow();
        }).onFailure(throwable -> testContext.failNow(throwable.getMessage()));
    });
  }


}
