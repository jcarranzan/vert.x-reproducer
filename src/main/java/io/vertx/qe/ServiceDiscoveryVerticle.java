package io.vertx.qe;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.Arrays;


/*
 * @author Adam Koniar (akoniar@redhat.com)
 *
 */
public class ServiceDiscoveryVerticle extends AbstractVerticle {
    private static final String HTTP_SERVICE_NAME = "some-http-service";
    private ServiceDiscovery discovery;

    private Record httpRecord;


    @Override
    public void start(Promise<Void> startPromise) {
        this.discovery = ServiceDiscovery.create(vertx,
                new ServiceDiscoveryOptions()
                        .setAnnounceAddress("service-announce")
                        .setName("my-name"));

        httpRecord = initHttpServiceRecord();

        discovery.publish(httpRecord, this::publishRecordHandler);

        Router router = Router.router(getVertx());
        router.get("/health").handler(routingContext -> routingContext.response().end("OK"));
        router.get("/http-service").handler(this::handleHttpService);

        getVertx().createHttpServer().requestHandler(router).listen(8080, httpServerAsyncResult -> {
            if (httpServerAsyncResult.succeeded()) {
                startPromise.complete();
            } else {
                startPromise.fail(httpServerAsyncResult.cause());
            }
        });
    }

    private Record initHttpServiceRecord() {
        return HttpEndpoint.createRecord(HTTP_SERVICE_NAME, "localhost", 8081, "/");
    }

    private void publishRecordHandler(AsyncResult<Record> recordAsyncResult) {
        if (recordAsyncResult.failed()) {
            System.out.println("Something went wrong");
            throw new RuntimeException(recordAsyncResult.cause());
        }
    }

    private void consummerService(Record record) {
        discovery.getRecord(r -> r.getName().equals(record.getName()), ar -> {
            if (ar.succeeded()) {
                if (ar.result() != null) {
                    // Retrieve the service reference
                    ServiceReference reference = discovery.getReference(ar.result());
                    // Retrieve the service object
                    HttpClient client = reference.get();
                    System.out.println("Consuming "+record.getLocation());

                    client.request(HttpMethod.GET, String.valueOf(record.getLocation()), response -> {
                        //release the service
                        reference.release();

                    });
                }
            } else {
                System.out.println("Lookup failed");
                ar.cause().printStackTrace();
            }

        });
    }

    private void handleHttpService(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        System.out.println("started");
        HttpEndpoint.getWebClient(discovery, new JsonObject().put("name", HTTP_SERVICE_NAME), recordAsyncResult -> {
            System.out.println("client?");
            if (recordAsyncResult.succeeded()) {
                System.out.println("client!");
                try {
                    WebClient client = recordAsyncResult.result();

                    System.out.println("client!!");
                    client.get("").send(httpResponseAsyncResult -> {
                        System.out.println("request?");
                        if (httpResponseAsyncResult.succeeded()) {
                            System.out.println("request!");
                            response.end(new JsonObject()
                                    .put("httpResponse", "OK")
                                    .put("recordStatus", httpRecord.getStatus()).encodePrettily());
                            consummerService(httpRecord);
                        } else {
                            System.out.println("!request");
                            response.setStatusCode(502);
                            response.end(httpResponseAsyncResult.cause().getMessage());
                        }
                        ServiceDiscovery.releaseServiceObject(discovery, client);

                    });
                } catch (Throwable wtf) {
                    System.out.println("Something wicked this way came");
                    wtf.printStackTrace();
                    response.setStatusCode(502);
                    response.end(recordAsyncResult.cause().getMessage());

                }
            } else {
                System.out.println("!client");
                recordAsyncResult.cause().printStackTrace();
                response.setStatusCode(501);
                response.end(recordAsyncResult.cause().getMessage());
            }
        });
    }

    @Override
    public void stop() {
        discovery.unpublish(httpRecord.getRegistration(),
                unpublishHttpServiceAsyncResult -> unpublishNextServicesHandler(unpublishHttpServiceAsyncResult, closeVertxHandler -> {
                    discovery.close();
                    try {
                        super.stop();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
    }

    private void unpublishNextServicesHandler(AsyncResult<Void> asyncResult, Handler<Void> stopVertxHandler, String... serviceNames) {
        if (asyncResult.succeeded()) {
            if (serviceNames.length > 1) {
                discovery.unpublish(serviceNames[0], unpublishAsyncResult -> unpublishNextServicesHandler(unpublishAsyncResult, stopVertxHandler, Arrays.copyOfRange(serviceNames, 1, serviceNames.length - 1)));
            } else if (serviceNames.length == 1) {
                discovery.unpublish(serviceNames[0], unpublishAsyncResult -> {
                    if (unpublishAsyncResult.succeeded()) {
                        stopVertxHandler.handle(null);
                    } else {
                        throw new RuntimeException(unpublishAsyncResult.cause());
                    }
                });
            }
        } else {
            throw new RuntimeException(asyncResult.cause());
        }
    }
}
