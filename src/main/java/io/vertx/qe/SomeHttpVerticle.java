package io.vertx.qe;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class SomeHttpVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise){
    Router router = Router.router(getVertx());
    router.get("/health").handler(routingContext -> routingContext.response().end("OK"));
    router.route().handler(BodyHandler.create());
    router.get("/").handler(rc -> {
      String message = rc.request().getParam("message");
      HttpServerResponse response = rc.response();
      response.setStatusCode(200);
      response.putHeader("content-type", "application/json;charset=UTF-8");
      JsonObject entries = new JsonObject().put("timestamp", System.currentTimeMillis()).put("message", message);
      response.end(entries.encodePrettily());
    });

    getVertx().createHttpServer().requestHandler(router).listen(8081, httpServerAsyncResult -> {
      if(httpServerAsyncResult.succeeded()) {
        startPromise.complete();
      } else {
        startPromise.fail(httpServerAsyncResult.cause());
      }
    });
  }
}