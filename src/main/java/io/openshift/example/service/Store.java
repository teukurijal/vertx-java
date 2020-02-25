package io.openshift.example.service;

import io.vertx.core.json.JsonObject;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * A CRUD to SQL interface
 */
public interface Store {


  Single<JsonObject> createFormRequest(JsonObject item);

  Single<JsonObject> createFormDistribution(JsonObject param);

  Observable<JsonObject> readAllCategory();

  Observable<JsonObject> readAllUserApproval();

  Observable<JsonObject> readAllFormDistribution();

  Observable<JsonObject> readAllFormRequest();

  Single<JsonObject> readUser(JsonObject item);

  Observable<JsonObject> readFormDistribution(int id);

  Single<JsonObject> readOneFormRequest(int id);

  Completable updateStatusForm(int id, JsonObject item);

  Completable updateShowForm(JsonObject item);

  Observable<JsonObject> updateStatusFormList(JsonObject item);


  Single<JsonObject> create(JsonObject item);
  Single<JsonObject> read(long id);
  Completable update(long id, JsonObject item);
  Completable delete(long id);
}
