package io.openshift.example.service.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLOptions;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLRowStream;
import io.openshift.example.service.Store;
import rx.Completable;
import rx.Observable;
import rx.Single;

import java.util.NoSuchElementException;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAlias;

// import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * The implementation of the store.
 *
 */
public class JdbcProductStore implements Store {

  private static final String LOGIN = "SELECT * FROM private.user where username = ? and password = ?";

  private static final String INSERT = "INSERT INTO products (name, stock) VALUES (?, ?::BIGINT)";

  private static final String INSERT_FORM_REQUEST = "INSERT INTO private.form_request (subject, description, file, approval_type, category_id, created_at) VALUES (?, ?, ?, ?, ?::INTEGER, TIMEZONE('Asia/Jakarta', CURRENT_TIMESTAMP))";

  // private static final String INSERT_FORM_REQUEST = "INSERT INTO private.form_request (subject, description, file, approval_type, category_id, created_at, user_id) VALUES (?, ?, ?, ?, ?::INTEGER, TIMEZONE('Asia/Jakarta', CURRENT_TIMESTAMP), ?)";

  private static final String UPDATE_FORM_REQUEST = "UPDATE private.form_request SET (subject, description, file, approval_type, category_id, update_at) = (?, ?, ?, ?, ?::INTEGER, TIMEZONE('Asia/Jakarta', CURRENT_TIMESTAMP)) WHERE id = ?";

  private static final String LIST_FORM_REQUEST = "SELECT f.id, subject, f.description, file, approval_type, c.name AS category, f.created_at FROM private.form_request f LEFT JOIN private.category c ON c.id = f.category_id";

  private static final String SELECT_ONE_FORM_REQUEST = "SELECT fr.id, fr.subject, fr.description, fr.approval_type, c.name as category, array_to_json(array_agg(row_to_json(x))) AS status FROM (SELECT a.form_id, a.user_approval_id, a.approval_status as status, b.full_name as user_approval_name FROM private.log_form a JOIN private.user_approval b ON b.id = a.user_approval_id ORDER BY a.approver_seq ASC ) x JOIN private.form_request fr ON fr.id = x.form_id JOIN private.category c on c.id = fr.category_id JOIN private.user_approval ua on ua.id = x.user_approval_id WHERE fr.id = ? GROUP BY fr.id, c.name";
  //"SELECT DISTINCT fr.id, fr.subject, fr.description, fr.approval_type, c.name as category, lf.approval_status as status, ua.id as id, ua.full_name as user_approval FROM private.form_request fr JOIN private.category c on c.id = fr.category_id JOIN private.log_form lf on lf.form_id = fr.id JOIN private.user_approval ua on ua.id = lf.user_approval_id WHERE fr.id = ?";

  private static final String SELECT_ONE = "SELECT * FROM private.products WHERE id = ?";

  private static final String SELECT_ALL_CATEGORY = "SELECT * FROM private.category";//done

  private static final String SELECT_ALL_USER_APPROVAL = "SELECT * FROM private.user_approval";

  private static final String UPDATE = "UPDATE products SET name = ?, stock = ?::BIGINT WHERE id = ?";

  private static final String UPDATE_STATUS_FORM = "UPDATE private.log_form SET approval_status = ?, update_at = TIMEZONE('Asia/Jakarta', CURRENT_TIMESTAMP) WHERE form_id = ? and user_approval_id = ? ";

  private static final String UPDATE_SHOW_FORM = "UPDATE private.log_form SET is_show = true WHERE form_id = ? and approver_seq = ?";

  private static final String UPDATE_STATUS_FORM_LIST = "SELECT * FROM private.log_form WHERE form_id = ? ORDER BY approver_seq ASC";

  private static final String UPDATE_FORM_DISTRIBUTION = "UPDATE private.log_form SET user_approval_id = ?, approval_status = ?, is_show = ?, update_at = TIMEZONE('Asia/Jakarta', CURRENT_TIMESTAMP)  WHERE form_id = ? AND approver_seq = ?";

  private static final String DELETE = "DELETE FROM private.products WHERE id = ?";

  private static final String DELETE_FORM_REQUEST = "DELETE FROM private.log_form WHERE form_id IN (SELECT id FROM private.form_request); DELETE FROM private.form_request WHERE id = ?";

  private static final String INSERT_LOG_FORM = "INSERT INTO private.log_form (form_id, user_approval_id, approval_status, is_show, approver_seq, created_at) VALUES (?, ?, ?, ?, ?, TIMEZONE('Asia/Jakarta', CURRENT_TIMESTAMP))";

  private static final String LIST_FORM_DISTRIBUTION = "SELECT u.id as user_id, u.full_name AS user_name, fr.subject, fr.description, fr.file, fr.approval_type, c.name AS category, approval_status, is_show, approver_seq FROM private.log_form lf JOIN private.form_request fr ON fr.id = lf.form_id JOIN private.user_approval u ON u.id = lf.user_approval_id JOIN private.category c ON c.id = fr.category_id";

  private static final String SELECT_ONE_FORM_DISTRIBUTION = "SELECT fr.id AS form_id, u.id as user_id, u.full_name AS user_name, fr.subject, fr.description, fr.file, fr.approval_type, c.name AS category, approval_status, is_show, approver_seq, fr.created_at, fr.update_at FROM private.log_form lf JOIN private.form_request fr ON fr.id = lf.form_id JOIN private.user_approval u ON u.id = lf.user_approval_id JOIN private.category c ON c.id = fr.category_id WHERE u.id = ?";

  private final JDBCClient db;

  public JdbcProductStore(JDBCClient db) {
    this.db = db;
  }

  @Override
  public Single<JsonObject> create(JsonObject item) {
    Optional<Exception> error = validateRequestBody(item);
    if (validateRequestBody(item).isPresent()){
      return Single.error(error.get());
    }

    return db.rxGetConnection()
      .map(con -> con.setOptions(new SQLOptions().setAutoGeneratedKeys(true)))
      .flatMap(conn -> {
        JsonArray params = new JsonArray().add(item.getValue("name")).add(item.getValue("stock", 0));
        return conn
          .rxUpdateWithParams(INSERT, params)
          .map(ur -> item.put("id", ur.getKeys().getLong(0)))
          .doAfterTerminate(conn::close);
      });
  }


  @Override
  public Single<JsonObject> createFormRequest(JsonObject item) {
    
    return db.rxGetConnection()
    .map(con -> con.setOptions(new SQLOptions().setAutoGeneratedKeys(true)))
    .flatMap(conn -> {
      JsonArray params = new JsonArray()
        .add(item.getValue("subject", ""))
        .add(item.getValue("description", ""))
        .add(item.getValue("file", ""))
        .add(item.getValue("approval_type", ""))
        .add(item.getValue("category_id", ""));
        // .add(item.getValue("user_id"))readFormDistribution
      return conn
        .rxUpdateWithParams(INSERT_FORM_REQUEST, params)
        .map(e -> {
          return item.put("id", e.getKeys().getLong(0));
        })
        .doAfterTerminate(conn::close);        
    });


  }

  @Override
  public Single<JsonObject> createFormDistribution (JsonObject param) {
    return db.rxGetConnection()
    .map(con -> con.setOptions(new SQLOptions().setAutoGeneratedKeys(true)))
    .flatMap(conn -> {
      JsonArray params = new JsonArray()
        .add(param.getValue("form_id", ""))
        .add(param.getValue("user_approval_id", ""))
        .add(param.getValue("approval_status", ""))
        .add(param.getValue("is_show", ""))
        .add(param.getValue("approver_seq", ""));
      return conn
        .rxUpdateWithParams(INSERT_LOG_FORM, params)
        .map(e -> 
          param.put("id", e.getKeys().getLong(0))
          )
        .doAfterTerminate(conn::close);        
    });
  }

  @Override
  public Single<JsonObject> readUser(JsonObject item) {
    return db.rxGetConnection()
      .flatMap(conn -> {
        JsonArray params = new JsonArray().add(item.getValue("username", "")).add(item.getValue("password"));
        return conn
          .rxQueryWithParams(LOGIN, params)
          .map(ResultSet::getRows)
          .flatMap(list -> {
            if (list.isEmpty()) {
              return Single.error(new NoSuchElementException("Wrong username or password"));
            } else {
              return Single.just(list.get(0));
            }
          })
          .doAfterTerminate(conn::close);
      });
  }

  @Override
  public Completable update(long id, JsonObject item) {
    Optional<Exception> error = validateRequestBody(item);
    if (validateRequestBody(item).isPresent()){
      return Completable.error(error.get());
    }

    return db.rxGetConnection()
      .flatMapCompletable(conn -> {
        JsonArray params = new JsonArray().add(item.getValue("name")).add(item.getValue("stock", 0)).add(id);
        return conn.rxUpdateWithParams(UPDATE, params)
          .flatMapCompletable(up -> {
            if (up.getUpdated() == 0) {
              return Completable.error(new NoSuchElementException("Unknown item '" + id + "'"));
            }
            return Completable.complete();
          })
          .doAfterTerminate(conn::close);
      });
  }

  @Override
  public Completable updateShowForm(JsonObject item) {

    return db.rxGetConnection()
      .flatMapCompletable(conn -> {
        JsonArray param = new JsonArray()
          .add(item.getValue("form_id"))
          .add(item.getValue("approver_seq"));
        return conn.rxUpdateWithParams(UPDATE_SHOW_FORM, param)
          .flatMapCompletable(up -> {
            if (up.getUpdated() == 0) {
              return Completable.error(new NoSuchElementException("Unknown item '"));
            }
            return Completable.complete();
          })
          .doAfterTerminate(conn::close);
      });
  }

  @Override
  public Completable updateOneFormRequest (int id, JsonObject item) {

    return db.rxGetConnection()
      .flatMapCompletable(conn -> {
        JsonArray params = new JsonArray()
        // .add(id)
        .add(item.getValue("subject", ""))
        .add(item.getValue("description", ""))
        .add(item.getValue("file", ""))
        .add(item.getValue("approval_type", ""))
        .add(item.getValue("category_id", ""))
        .add(id);
      return conn
        .rxUpdateWithParams(UPDATE_FORM_REQUEST, params)
        .flatMapCompletable(up -> {
          if (up.getUpdated() == 0) {
            return Completable.error(new NoSuchElementException("Unknown item '" + id + "'"));
          }
          return Completable.complete();
        })
        .doAfterTerminate(conn::close);
      });
  }

  @Override
  public Completable updateStatusForm(int id, JsonObject item) {
    // Optional<Exception> error = validateRequestBody(item);
    // if (validateRequestBody(item).isPresent()){
    //   return Completable.error(error.get());
    // }

    return db.rxGetConnection()
      .flatMapCompletable(conn -> {
        JsonArray params = new JsonArray()
          .add(item.getValue("approval_status"))
          .add(item.getValue("form_id"))
          .add(id);
        return conn.rxUpdateWithParams(UPDATE_STATUS_FORM, params)
          .flatMapCompletable(up -> {
            if (up.getUpdated() == 0) {
              return Completable.error(new NoSuchElementException("Unknown item '" + id + "'"));
            }
            return Completable.complete();
          })
          .doAfterTerminate(conn::close);
      });
  }

  @Override
  public Observable<JsonObject> updateStatusFormList(JsonObject item) {
    return db.rxGetConnection()
      .flatMapObservable(conn -> {
        JsonArray param = new JsonArray()
          .add(item.getValue("form_id"));
        return conn
          .rxQueryStreamWithParams(UPDATE_STATUS_FORM_LIST, param)
          .flatMapObservable(SQLRowStream::toObservable)
          .doAfterTerminate(conn::close)
          .map(array ->
            new JsonObject()
            .put("form_id", array.getInteger(0))
            .put("user_approval", array.getInteger(1))
            .put("approval_status", array.getString(2))
            .put("is_show", array.getBoolean(3))
            .put("approval_seq", array.getLong(4))
          );
      });
  }

  @Override
  public Completable updateFormDistribution(JsonObject param) {
    return db.rxGetConnection()
      .flatMapCompletable(conn -> {
        JsonArray params = new JsonArray()
          .add(param.getValue("user_approval_id"))
          .add(param.getValue("approval_status"))
          .add(param.getValue("is_show"))
          .add(param.getValue("form_id"))
          .add(param.getValue("approver_seq"));
        return conn
          .rxUpdateWithParams(UPDATE_FORM_DISTRIBUTION, params)
          .flatMapCompletable(up -> {
            if (up.getUpdated() == 0) {
              return Completable.error(new NoSuchElementException("Unknown item"));
            }
            return Completable.complete();
          })
          .doAfterTerminate(conn::close);
      });
  }

  private Optional<Exception> validateRequestBody(JsonObject item) {
    if (item == null) {
      return Optional.of(new IllegalArgumentException("The item must not be null"));
    }
    if (!(item.getValue("name") instanceof String) || item.getString("name") == null
        || item.getString("name").isEmpty()) {
      return Optional.of(new IllegalArgumentException("The name is required!"));
    }
    if (!(item.getValue("stock") instanceof Integer) || item.getInteger("stock") < 0) {
      return Optional.of(new IllegalArgumentException("The stock must be greater or equal to 0!"));
    }
    if (item.containsKey("id")) {
      return Optional.of(new IllegalArgumentException("Id was invalidly set on request."));
    }
    return Optional.empty();
  }

  @Override
  public Observable<JsonObject> readAllCategory() {
    return db.rxGetConnection()
      .flatMapObservable(conn ->
        conn
          .rxQueryStream(SELECT_ALL_CATEGORY)
          .flatMapObservable(SQLRowStream::toObservable)
          .doAfterTerminate(conn::close))
      .map(array ->
        new JsonObject()
          .put("id", array.getInteger(0))
          .put("name", array.getString(1))
          .put("description", array.getString(2))
      );
  }

  @Override
  public Observable<JsonObject> readAllUserApproval() {
    return db.rxGetConnection()
      .flatMapObservable(conn ->
        conn
          .rxQueryStream(SELECT_ALL_USER_APPROVAL)
          .flatMapObservable(SQLRowStream::toObservable)
          .doAfterTerminate(conn::close))
      .map(array ->
        new JsonObject()
          .put("id", array.getInteger(0))
          .put("full_name", array.getString(1))
          .put("position", array.getString(2))
          .put("gender", array.getString(3))
          .put("email", array.getString(4))
          .put("phone_number", array.getString(5))
      );
  }


  @Override
  public Observable<JsonObject> readAllFormRequest() {
    return db.rxGetConnection()
      .flatMapObservable(conn ->
        conn
          .rxQueryStream(LIST_FORM_REQUEST)
          .flatMapObservable(SQLRowStream::toObservable)
          .doAfterTerminate(conn::close))
      .map(array ->
        new JsonObject()
          .put("id", array.getInteger(0))
          .put("subject", array.getString(1))
          .put("description", array.getString(2))
          .put("file", array.getString(3))
          .put("approval_type", array.getString(4))
          .put("category", array.getString(5))
          .put("created_at", array.getString(6))
      );
  }

  @Override
  public Observable<JsonObject> readFormDistribution(int id) {
    return db.rxGetConnection()
      .flatMapObservable(conn -> {
        JsonArray param = new JsonArray().add(id);
        return conn
          .rxQueryStreamWithParams(SELECT_ONE_FORM_DISTRIBUTION, param)
          .flatMapObservable(SQLRowStream::toObservable)
          .doAfterTerminate(conn::close)
          .map(array -> 
            new JsonObject()
            .put("form_id", array.getInteger(0))
            .put("user_id", array.getInteger(1))
            .put("user_name", array.getString(2))
            .put("subject", array.getString(3))
            .put("description", array.getString(4))
            .put("file", array.getString(5))
            .put("approval_type", array.getString(6))
            .put("category", array.getString(7))
            .put("approval_status", array.getString(8))
            .put("is_show", array.getBoolean(9))
            .put("approval_seq", array.getLong(10))
            .put("created_at", array.getString(11))
            .put("updated_at", array.getString(12))
          );
      });
  }

  @Override
  public Single<JsonObject> read(long id) {
    return db.rxGetConnection()
      .flatMap(conn -> {
        JsonArray param = new JsonArray().add(id);
        return conn
          .rxQueryWithParams(SELECT_ONE, param)
          .map(ResultSet::getRows)
          .flatMap(list -> {
            if (list.isEmpty()) {
              return Single.error(new NoSuchElementException("Item '" + id + "' not found"));
            } else {
              return Single.just(list.get(0));
            }
          })
          .doAfterTerminate(conn::close);
      });
  }

  @Override
  public Observable<JsonObject> readAllFormDistribution() {
    return db.rxGetConnection()
      .flatMapObservable(conn ->
        conn
          .rxQueryStream(LIST_FORM_DISTRIBUTION)
          .flatMapObservable(SQLRowStream::toObservable)
          .doAfterTerminate(conn::close))
      .map(array ->
        new JsonObject()
          .put("user_id", array.getInteger(0))
          .put("user_name", array.getString(1))
          .put("subject", array.getString(2))
          .put("description", array.getString(3))
          .put("file", array.getString(4))
          .put("approval_type", array.getString(5))
          .put("category", array.getString(6))
          .put("approval_status", array.getString(7))
          .put("is_show", array.getBoolean(8))
          .put("approval_seq", array.getInteger(9))
      );
  }

  @Override
  public Observable<JsonObject> readOneFormRequest(int id) {
    return db.rxGetConnection()
      .flatMapObservable(conn -> {
        JsonArray param = new JsonArray().add(id);
        return conn
          .rxQueryStreamWithParams(SELECT_ONE_FORM_REQUEST, param)
          .flatMapObservable(SQLRowStream::toObservable)
          .doAfterTerminate(conn::close)
          .map(array -> 
            new JsonObject()
              .put("id", array.getInteger(0))
              .put("subject", array.getString(1))
              .put("description", array.getString(2))
              .put("approval_type", array.getString(3))
              .put("category", array.getString(4))
              .put("status", array.getString(5))
              );
      });
  }

  @Override
  public Completable deleteFormRequest(int id) {
    return db.rxGetConnection()
      .flatMapCompletable(conn -> {
        JsonArray params = new JsonArray().add(id);
        return conn.rxUpdateWithParams(DELETE_FORM_REQUEST, params)
          .flatMapCompletable(up -> {
            if (up.getUpdated() == 0) {
              return Completable.error(new NoSuchElementException("Unknown item '" + id + "'"));
            }
            return Completable.complete();
          })
          .doAfterTerminate(conn::close);
      });
  }

  @Override
  public Completable delete(long id) {
    return db.rxGetConnection()
      .flatMapCompletable(conn -> {
        JsonArray params = new JsonArray().add(id);
        return conn.rxUpdateWithParams(DELETE, params)
          .flatMapCompletable(up -> {
            if (up.getUpdated() == 0) {
              return Completable.error(new NoSuchElementException("Unknown item '" + id + "'"));
            }
            return Completable.complete();
          })
          .doAfterTerminate(conn::close);
      });
  }
}
