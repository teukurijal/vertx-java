package io.openshift.example;

import io.openshift.example.service.Store;
import io.openshift.example.service.impl.JdbcProductStore;
import io.openshift.example.service.impl.StatusSchema;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
// import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
// import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.unit.Async;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.redis.client.Response;
// import io.vertx.rxjava.ext.web.handler.St
import rx.Single;
import rx.Subscriber;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
// import java.util.HashMap;
// import java.util.Map;
import java.util.NoSuchElementException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static io.openshift.example.Errors.error;

public class CrudApplication extends AbstractVerticle {


  private JWTAuth authProvider;
  private Store store;
  // EventBus eb = (EventBus) vertx.eventBus();
  
  // private AuthProvider authProvider;

  @Override
  public void start() {
    // Create a router object.
    Router router = Router.router(vertx);
    // enable parsing of request bodies
    router.route().handler(BodyHandler.create());
    // perform validation of the :id parameter
    router.route("/api/fruits/:id").handler(this::validateId);
    router.route("/api/form-distribution/user/:id").handler(this::validateId);
    router.route("/api/form-request/:id").handler(this::validateId);

    // router.post("/api/login").handler(this::getAuth);
    router.post("/api/login").handler(this::getUser);
    // implement a basic REST CRUD mapping
    router.get("/api/category").handler(this::getAllCategory);
    router.get("/api/user-approval").handler(this::getAllUserApproval);

    router.post("/api/form-request-add").handler(this::addFormRequest);
    router.get("/api/form-request").handler(this::listFormRequest);
    router.get("/api/form-request/:id").handler(this::getOneFormRequest);
    router.put("/api/form-request/:id").handler(this::updateOneFormRequest);
    // router.get("api/form_request/:id").handler(this::getFormRequest);
    // router.get("api/form_request").handler(this::getAllFormRequest);
    router.get("/api/form-distribution").handler(this::listFormDistribution);
    router.get("/api/form-distribution/user/:id").handler(this::getOneFormDistribution);
    router.post("/api/form-distribution/user/:id").handler(this::updateStatusForm);

    // router.post("/api/fruits").handler(this::addOne);
    // router.get("/api/fruits/:id").handler(this::getOne);
    // router.put("/api/fruits/:id").handler(this::updateOne);
    // router.delete("/api/fruits/:id").handler(this::deleteOne);

    // health check
    router.get("/").handler(rc -> rc.response().end("OK"));
    // web interface
    // router.get().handler(StaticHandler.create());

    // Create a JDBC client
    // development
    // JDBCClient jdbc = JDBCClient.createShared(vertx,
    //     new JsonObject()
    //         .put("url", "jdbc:postgresql://" + getEnv("MY_DATABASE_SERVICE_HOST", "172.17.0.3") + ":5432/ecompliance")
    //         .put("driver_class", "org.postgresql.Driver").put("user", getEnv("DB_USERNAME", "user"))
    //         .put("password", getEnv("DB_PASSWORD", "password")));
      // production
    JDBCClient jdbc = JDBCClient.createShared(vertx,
        new JsonObject()
            .put("url", "jdbc:postgresql://" + getEnv("MY_DATABASE_SERVICE_HOST", "52.203.160.194") + ":5432/dfbpd3cplr4ds0")
            .put("driver_class", "org.postgresql.Driver").put("user", getEnv("DB_USERNAME", "pdjrxskyjczyov"))
            .put("password", getEnv("DB_PASSWORD", "9f4e2a63ecd68d18cc16943a30bb77830e77454fffa518a69778cb61b35cbddf")));

    DBInitHelper.initDatabase(vertx, jdbc).andThen(initHttpServer(router, jdbc)).subscribe(
        (http) -> System.out.println("Server ready on port " + http.actualPort()), Throwable::printStackTrace);
  }

  private Single<HttpServer> initHttpServer(Router router, JDBCClient client) {
    store = new JdbcProductStore(client);
    // Create the HTTP server and pass the "accept" method to the request handler.
    // return vertx.createHttpServer().requestHandler(router).rxListen(8080);
    //production
    return vertx.createHttpServer().requestHandler(router).listen(System.getenv('PORT') as int, '0.0.0.0')
  }

  private void validateId(RoutingContext ctx) {
    try {
      ctx.put("fruitId", Long.parseLong(ctx.pathParam("id")));
      ctx.put("userApprovalId", Integer.parseInt(ctx.pathParam("id")));
      ctx.put("formId", Integer.parseInt(ctx.pathParam("id")));
      // continue with the next handler in the route
      ctx.next();
    } catch (NumberFormatException e) {
      error(ctx, 400, "invalid id: " + e.getCause());
    }
  }


  // private void getAuth(RoutingContext ctx) {
  //   JsonObject authInfo = new JsonObject()
  //     .put("username", ctx.request().getParam("username"))
  //     .put("password",ctx.request().getParam("password"));
  //   authProvider.authenticate(authInfo, res -> {
  //     if (res.succeeded()) {
  //       JWTAuth jwt = JWTAuth.create((Vertx) ctx.vertx(), new JsonObject().put("keyStore",
  //           new JsonObject().put("type", "jceks").put("path", "keystore.jceks").put("password", "secret")));
  //       String token = jwt.generateToken(new JsonObject().put("sub", ctx.request().getParam("username")),
  //           new JWTOptions().setExpiresInMinutes(60));
  //         ctx.setUser((User) res.result());
  //         ctx.session().put("token", token);
  //         ctx.response()
  //             .putHeader("location", (String) ctx.session().remove("return_url"))
  //             .setStatusCode(302)
  //             .end();
  //     } else {
  //         ctx.fail(401);
  //     }
  //   });
  // }

  private void getAllCategory(RoutingContext ctx) {
    HttpServerResponse response = ctx.response()
      .putHeader("Content-Type", "application/json");
    JsonArray res = new JsonArray();
    store.readAllCategory()
      .subscribe(
        res::add,
        err -> error(ctx, 415, err),
        () -> response.end(res.encodePrettily())
      );
  }

  private void getAllUserApproval(RoutingContext ctx) {
    HttpServerResponse response = ctx.response()
      .putHeader("Content-Type", "application/json");
    JsonArray res = new JsonArray();
    store.readAllUserApproval()
      .subscribe(
        res::add,
        err -> error(ctx, 415, err),
        () -> response.end(res.encodePrettily())
      );
  }

  private void listFormRequest(RoutingContext ctx) {
    HttpServerResponse response = ctx.response()
      .putHeader("Content-Type", "application/json");
    JsonArray res = new JsonArray();
    store.readAllFormRequest()
      .subscribe(
        res::add,
        err -> error(ctx, 415, err),
        () -> response.end(res.encodePrettily())
      );
  }

  private void listFormDistribution(RoutingContext ctx) {
    HttpServerResponse response = ctx.response()
      .putHeader("Content-Type", "application/json");
    JsonArray res = new JsonArray();
    store.readAllFormDistribution()
      .subscribe(
        res::add,
        err -> error(ctx, 415, err),
        () -> response.end(res.encodePrettily())
      );
  }

  private void getOne(RoutingContext ctx) {
    HttpServerResponse response = ctx.response()
      .putHeader("Content-Type", "application/json");

    store.read(ctx.get("fruitId"))
      .subscribe(
        json -> response.end(json.encodePrettily()),
        err -> {
          if (err instanceof NoSuchElementException) {
            error(ctx, 404, err);
          } else if (err instanceof IllegalArgumentException) {
            error(ctx, 415, err);
          } else {
            error(ctx, 500, err);
          }
        }
      );
  }

  private void getOneFormDistribution(RoutingContext ctx) {
    HttpServerResponse response = ctx.response()
      .putHeader("Content-Type", "application/json");

    JsonArray res = new JsonArray();
    store.readFormDistribution(ctx.get("userApprovalId"))
      .subscribe(
        res::add,
        err -> error(ctx, 415, err),
        () -> response.end(res.encodePrettily())
      );
      //   err -> {
      //     if (err instanceof NoSuchElementException) {
      //       error(ctx, 404, err);
      //     } else if (err instanceof IllegalArgumentException) {
      //       error(ctx, 415, err);
      //     } else {
      //       error(ctx, 500, err);
      //     }
      //   }
      // );
  }

  private void getOneFormRequest(RoutingContext ctx) {
    HttpServerResponse response = ctx.response()
      .putHeader("Content-Type", "application/json");

    JsonArray array = new JsonArray();

    store.readOneFormRequest(ctx.get("formId"))
      .subscribe (
        json -> { 
          try {
            ObjectMapper mapper = new ObjectMapper();
            List<StatusSchema> status = mapper.readValue(json.getString("status"), new TypeReference<List<StatusSchema>>() {});

            json.put("status", status);
            array.add(json);
           
            System.out.println(">>>>>>>>>>>>resssssss>>>>"+array);
            System.out.println(">>>>>>>>>>>>>>>>>json>>"+json);
            
          } catch(Exception e) {
            e.printStackTrace();
          }
        },
        err -> writeError(ctx, err),
        () -> {

          
          
          
          // JsonObject user = new JsonObject();
          // for( int i = 0; i < res.size(); i ++) {

          //   // user.add("name", res.getJsonObject(i).getString("approval_name"));
          //   System.out.println(">>>>>>>>>>>>>>>>>>>"+user);
          // }
          // System.out.println(">>>>>>>>>>>>>>>>>>>...."+user);
          response.end(array.encodePrettily());
        }
      );
  }

  private void addOne(RoutingContext ctx) {
    JsonObject item;
    try {
      item = ctx.getBodyAsJson();
    } catch (RuntimeException e) {
      error(ctx, 415, "invalid payload");
      return;
    }

    if (item == null) {
      error(ctx, 415, "invalid payload");
      return;
    }

    store.create(item)
      .subscribe(
        res ->
          ctx.response()
            .putHeader("Location", "/api/fruits/" + res.getLong("id"))
            .putHeader("Content-Type", "application/json")
            .setStatusCode(201)
            .end(res.encodePrettily()),
        err -> writeError(ctx, err)
      );
  }

  private void addFormRequest(RoutingContext ctx) {
    // HttpServerResponse response = ctx.response()
    // .putHeader("Content-Type", "application/json");

    JsonObject item;

    try {
      item = ctx.getBodyAsJson();
    } catch (RuntimeException e) {
      error(ctx, 415, "invalid payload");
      return;
    }

    if (item == null) {
      error(ctx, 415, "invalid payload");
      return;
    }

    JsonArray userApproval = (JsonArray) item.getValue("user_approval");
    String approvalType = (String) item.getValue("approval_type");

    JsonObject idForm = new JsonObject();

    store.createFormRequest(item)
      .subscribe(
        json -> {

          idForm.put("id", json.getValue("id"));

          for (int i = 0; i < userApproval.size(); i ++ ) {

            JsonObject param = new JsonObject()
              .put("form_id", idForm.getInteger("id"))
              .put("user_approval_id", userApproval.getInteger(i))
              .put("approval_status","PENDING")
              .put("approver_seq", i + 1);
      
      
              if (approvalType.equals("serial")) {
                if(i == 0) {
                  param.put("is_show", true);
                } else {
                  param.put("is_show", false);
                }
              } else {
                  param.put("is_show", true);
              }
      
            store.createFormDistribution(param)
            .subscribe(
              res ->
                ctx.response()
                  .putHeader("Location", "/api/form_request/" + res.getLong("id"))
                  .putHeader("Content-Type", "application/json")
                  .setStatusCode(201)
                  .end(json.encodePrettily()),
              err -> writeError(ctx, err)
            );
          }

          
        },
        err -> writeError(ctx, err)
      );

   

      // System.out.println("==================================="+idForm);
      // JsonArray userApproval = (JsonArray) item.getValue("user_approval");
      // System.out.println(userApproval);

      // for (int i = 0; i < userApproval.size(); i ++) {
      //   System.out.println("========================" + userApproval.getValue(i));

      //   // store.createFormRequest(item)
      //   // .subscribe(
      //   //   json ->
      //   //     ctx.response()
      //   //       .putHeader("Location", "/api/form_request/" + json.getLong("id"))
      //   //       .putHeader("Content-Type", "application/json")
      //   //       .setStatusCode(201)
      //   //       .end(json.encodePrettily()),
      //   //   err -> writeError(ctx, err)
      //   // );

      // }

  }

  private void updateOneFormRequest(RoutingContext ctx) {
    JsonObject item;
    try {
      item = ctx.getBodyAsJson();
    } catch (RuntimeException e) {
      error(ctx, 415, "invalid payload");
      return;
    }

    if (item == null) {
      error(ctx, 415, "invalid payload");
      return;
    }

    // store.updateOneFormRequest(ctx.get("formId"), item)

  }

  private void updateStatusForm(RoutingContext ctx) {
    JsonObject item;
    try {
      item = ctx.getBodyAsJson();
    } catch (RuntimeException e) {
      error(ctx, 415, "invalid payload");
      return;
    }

    if (item == null) {
      error(ctx, 415, "invalid payload");
      return;
    }

    store.updateStatusForm(ctx.get("userApprovalId"), item)
      .subscribe(
        () ->
          ctx.response()
            .putHeader("Content-Type", "application/json")
            .setStatusCode(200)
            .end(item.put("id", ctx.<Integer>get("userApprovalId")).encodePrettily())
          ,
        err -> writeError(ctx, err)
      );

      JsonArray res = new JsonArray();
      JsonObject param = new JsonObject()
        .put("form_id", item.getValue("form_id"));
      
      store.updateStatusFormList(param)
        .subscribe(
          res::add,
          err -> writeError(ctx, err),
          () -> { 
          
          // System.out.println(">>>>>>>>>sizecut>>>"+res.size());
          // System.out.println(res.getJsonObject(0).getString("approval_status").equals("APPROVED"));

          for (int i = 0; i < res.size(); i ++) {

            if(res.getJsonObject(i).getString("approval_status").equals("APPROVED")) {
              item.put("approver_seq", i+2);
              store.updateShowForm(item)
                .subscribe(
                  () -> {
                    ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(200).end();
                  }
                  ,
                  err -> writeError(ctx, err)
                );
            }

          }

        }
        
      );
  }

  private void updateOne(RoutingContext ctx) {
    JsonObject item;
    try {
      item = ctx.getBodyAsJson();
    } catch (RuntimeException e) {
      error(ctx, 415, "invalid payload");
      return;
    }

    if (item == null) {
      error(ctx, 415, "invalid payload");
      return;
    }

    store.update(ctx.get("fruitId"), item)
      .subscribe(
        () ->
          ctx.response()
            .putHeader("Content-Type", "application/json")
            .setStatusCode(200)
            .end(item.put("id", ctx.<Long>get("fruitId")).encodePrettily()),
        err -> writeError(ctx, err)
      );
  }

  private void getUser(RoutingContext ctx) {
    HttpServerResponse response = ctx.response()
    .putHeader("Content-Type", "application/json");

    JsonObject item = ctx.getBodyAsJson();

    store.readUser(item)
      .subscribe(
        json -> response.end(json.encodePrettily()),
        err -> {
          if (err instanceof NoSuchElementException) {
            error(ctx, 404, err);
          } else if (err instanceof IllegalArgumentException) {
            error(ctx, 415, err);
          } else {
            error(ctx, 500, err);
          }
        }
      );
  }

  private void writeError(RoutingContext ctx, Throwable err) {
    if (err instanceof NoSuchElementException) {
      error(ctx, 404, err);
    } else if (err instanceof IllegalArgumentException) {
      error(ctx, 422, err);
    } else {
      error(ctx, 409, err);
    }
  }

  private void deleteOne(RoutingContext ctx) {
    store.delete(ctx.get("fruitId"))
      .subscribe(
        () ->
          ctx.response()
            .setStatusCode(204)
            .end(),
        err -> {
          if (err instanceof NoSuchElementException) {
            error(ctx, 404, err);
          } else {
            error(ctx, 415, err);
          }
        }
      );
  }

  private String getEnv(String key, String dv) {
    String s = System.getenv(key);
    if (s == null) {
      return dv;
    }
    return s;
  }
}
