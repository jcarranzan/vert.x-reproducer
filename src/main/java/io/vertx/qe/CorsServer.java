package io.vertx.qe;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.HashSet;
import java.util.Set;

/*
 * @author <a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 * reviewed by: Giacomo Venturini mail: giacomo.venturini3@gmail.com"
 */
public class CorsServer extends AbstractVerticle {

  @Override
  public void start() throws Exception {

    Router router = Router.router(vertx);

    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("x-requested-with");
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("origin");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("accept");
    allowedHeaders.add("X-PINGARUNER");

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    /*
     * these methods aren't necessary for this sample,
     * but you may need them for your projects
     */
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);

    router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

    router.get("/access-control-with-get").handler(ctx -> {
      System.out.println("Processing get request ");
      HttpServerResponse httpServerResponse = ctx.response();
      httpServerResponse.setChunked(true);
      MultiMap headers = ctx.request().headers();
      for (String key : headers.names()) {
        httpServerResponse.write(key + ": ");
        httpServerResponse.write(headers.get(key));
        httpServerResponse.write("<br>");
      }
      httpServerResponse.putHeader("Content-Type", "application/text").end("Success");
    });

    // Serve the static resources
    router.route().handler(StaticHandler.create("webroot"));

    vertx.createHttpServer().requestHandler(router).listen(8080);
  }
}
