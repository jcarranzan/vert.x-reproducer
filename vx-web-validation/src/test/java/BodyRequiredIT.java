import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
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
  void testRequestWithOutBodyTest(Vertx vertx, VertxTestContext testContext) {
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
  void contentLenght0AsBodyTest(Vertx vertx, VertxTestContext testContext) {
    testContext.verify(() -> {
      vertx.createHttpClient()
        .request(HttpMethod.POST, 8080, "localhost", "/user")
        .compose(request -> {
          request.putHeader("Content-Length", "0");
          return request.send();
        })
        .onSuccess(httpClientResponse -> {
          System.out.println("STATUS RESPONSE CODE " + httpClientResponse.statusCode());
          assertEquals(400, httpClientResponse.statusCode());
          testContext.completeNow();
        }).onFailure(throwable -> testContext.failNow(throwable.getMessage()));
    });
  }
  @Test
  void predicateWithEmptyStringBody(Vertx vertx, VertxTestContext testContext) {

    vertx.createHttpClient()
      .request(HttpMethod.POST, 8080, "localhost", "/user")
      .compose(request -> {

        request.putHeader("content-type", "application/json").end("");
        return request.send();
      })
      .onSuccess(httpClientResponse -> {
        System.out.println("STATUS RESPONSE CODE " + httpClientResponse.statusCode());
        assertEquals(400, httpClientResponse.statusCode());
        testContext.completeNow();
      }).onFailure(throwable -> testContext.failNow(throwable.getMessage()));
  }
  @Test
  void testInvalidJsonRequestBody(Vertx vertx, VertxTestContext testContext) {
    testContext.verify(() -> {
      vertx.createHttpClient()
        .request(HttpMethod.POST, 8080, "localhost", "/user")
        .compose(request -> {
          request.putHeader("Content-Type", "application/json")
            .end("\"invalid\": \"json\""); // Cuerpo no válido según el esquema
          return request.send();
        })
        .onSuccess(httpClientResponse -> {
          assertEquals(400, httpClientResponse.statusCode()); // Se espera un código de estado 400
          testContext.completeNow();
        })
        .onFailure(throwable -> testContext.failNow(throwable.getMessage()));
    });
  }


}
