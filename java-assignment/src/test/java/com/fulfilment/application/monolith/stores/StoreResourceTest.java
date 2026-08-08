package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the {@code /store} endpoints, covering CRUD behaviour, validation responses,
 * and the fact that a successful write triggers the legacy sync (via the AFTER_SUCCESS observer).
 */
@QuarkusTest
public class StoreResourceTest {

  private static final String PATH = "store";
  private static final long MISSING_ID = 999_999L;

  // Mocked so we assert the sync happened without touching the real file-writing gateway.
  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @BeforeEach
  public void resetMock() {
    reset(legacyStoreManagerGateway);
  }

  private long createStore(String name, int stock) {
    return given()
        .contentType("application/json")
        .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":" + stock + "}")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  @Test
  public void getAllReturnsOk() {
    given().when().get(PATH).then().statusCode(200);
  }

  @Test
  public void createValidPersistsAndSyncsLegacy() {
    long id = createStore("CREATE-OK", 5);

    given().when().get(PATH + "/" + id).then().statusCode(200).body("name", equalTo("CREATE-OK"));

    verify(legacyStoreManagerGateway, timeout(1000)).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void createWithIdSetReturns422() {
    given()
        .contentType("application/json")
        .body("{\"id\":123,\"name\":\"CREATE-BAD\",\"quantityProductsInStock\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  public void getSingleMissingReturns404() {
    given().when().get(PATH + "/" + MISSING_ID).then().statusCode(404);
  }

  @Test
  public void updateExistingReturns200AndSyncsLegacy() {
    long id = createStore("UPDATE-ME", 1);
    reset(legacyStoreManagerGateway);

    given()
        .contentType("application/json")
        .body("{\"name\":\"UPDATE-ME-2\",\"quantityProductsInStock\":9}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("UPDATE-ME-2"))
        .body("quantityProductsInStock", is(9));

    verify(legacyStoreManagerGateway, timeout(1000)).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void updateMissingReturns404() {
    given()
        .contentType("application/json")
        .body("{\"name\":\"NOPE\",\"quantityProductsInStock\":1}")
        .when()
        .put(PATH + "/" + MISSING_ID)
        .then()
        .statusCode(404);
  }

  @Test
  public void updateWithoutNameReturns422() {
    long id = createStore("UPDATE-NONAME", 1);

    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\":2}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(422);
  }

  @Test
  public void patchExistingReturns200AndSyncsLegacy() {
    long id = createStore("PATCH-ME", 4);
    reset(legacyStoreManagerGateway);

    given()
        .contentType("application/json")
        .body("{\"name\":\"PATCH-ME-2\",\"quantityProductsInStock\":6}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("PATCH-ME-2"));

    verify(legacyStoreManagerGateway, timeout(1000)).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void patchMissingReturns404() {
    given()
        .contentType("application/json")
        .body("{\"name\":\"NOPE\",\"quantityProductsInStock\":1}")
        .when()
        .patch(PATH + "/" + MISSING_ID)
        .then()
        .statusCode(404);
  }

  @Test
  public void patchKeepsStockWhenExistingStockIsZero() {
    // Documents current patch behaviour: when the stored stock is 0, the incoming stock is ignored
    // (the guard checks the existing entity's value, not the request), so only the name changes.
    long id = createStore("PATCH-ZERO", 0);

    given()
        .contentType("application/json")
        .body("{\"name\":\"PATCH-ZERO-2\",\"quantityProductsInStock\":6}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("PATCH-ZERO-2"))
        .body("quantityProductsInStock", is(0));
  }

  @Test
  public void createDuplicateNameReturns500() {
    // Second insert violates the unique name constraint at commit time. It is not a
    // WebApplicationException, so the ErrorMapper falls through to its default 500 branch.
    createStore("DUPLICATE-NAME", 1);

    given()
        .contentType("application/json")
        .body("{\"name\":\"DUPLICATE-NAME\",\"quantityProductsInStock\":2}")
        .when()
        .post(PATH)
        .then()
        .statusCode(500);
  }

  @Test
  public void patchWithoutNameReturns422() {
    long id = createStore("PATCH-NONAME", 1);

    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\":2}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(422);
  }

  @Test
  public void patchWhenExistingNameIsNullSkipsNameUpdate() {
    // create() does not require a name, so we can persist a store with a null name.
    long id =
        given()
            .contentType("application/json")
            .body("{\"quantityProductsInStock\":3}")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

    // Existing name is null, so the `entity.name != null` guard is false and the name is NOT applied
    // (documents the current behaviour); the stock guard is true (3 != 0) so stock is updated.
    given()
        .contentType("application/json")
        .body("{\"name\":\"WONT-APPLY\",\"quantityProductsInStock\":9}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", nullValue())
        .body("quantityProductsInStock", is(9));
  }

  @Test
  public void errorMapperOmitsErrorFieldWhenExceptionMessageIsNull() {
    StoreResource.ErrorMapper mapper = new StoreResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    // A non-WebApplicationException with no message: falls through to the 500 default and the
    // `getMessage() != null` guard is false, so no "error" field is added to the payload.
    Response response = mapper.toResponse(new RuntimeException());

    assertEquals(500, response.getStatus());
    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(500, body.get("code").asInt());
    assertFalse(body.has("error"));
  }

  @Test
  public void errorMapperIncludesErrorFieldAndStatusForWebApplicationException() {
    StoreResource.ErrorMapper mapper = new StoreResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    Response response = mapper.toResponse(new WebApplicationException("boom", 404));

    assertEquals(404, response.getStatus());
    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(404, body.get("code").asInt());
    assertEquals("boom", body.get("error").asText());
  }

  @Test
  public void deleteExistingReturns204AndMissingReturns404() {
    long id = createStore("DELETE-ME", 1);

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH + "/" + id).then().statusCode(404);
    given().when().delete(PATH + "/" + MISSING_ID).then().statusCode(404);
  }
}
