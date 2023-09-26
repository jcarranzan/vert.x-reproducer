import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.validation.RequestPredicate;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;

public class Server extends AbstractVerticle {
  private HttpServer httpServer;

  private SchemaParser schemaParser;
  private SchemaRouter schemaRouter;

  @Override
  public void start() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    httpServer = vertx.createHttpServer();
    schemaParser = createSchemas();

    router.post("/user").handler(
        ValidationHandlerBuilder
          .create(schemaParser)
          .predicate(RequestPredicate.BODY_REQUIRED)
          .build())
      .handler(routingContext -> {
        routingContext.response().setStatusCode(200).end("User request handled successfully");
      });
    httpServer.requestHandler(router).listen(8080);
  }

  private SchemaParser createSchemas() {
    schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    schemaParser= SchemaParser.createDraft7SchemaParser(schemaRouter);
    return schemaParser;
  }
}
