package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/** Integration tests for the {@code /product} endpoints plus its ErrorMapper. */
@QuarkusTest
public class ProductResourceTest {

  private static final String PATH = "product";
  private static final long MISSING_ID = 999_999L;

  private long createProduct(String name, int stock) {
    return given()
        .contentType("application/json")
        .body("{\"name\":\"" + name + "\",\"stock\":" + stock + "}")
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
  public void createValidPersistsAndIsRetrievable() {
    long id = createProduct("PTEST-CREATE", 5);

    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("PTEST-CREATE"))
        .body("stock", is(5));
  }

  @Test
  public void createWithIdSetReturns422() {
    given()
        .contentType("application/json")
        .body("{\"id\":123,\"name\":\"PTEST-BAD\",\"stock\":1}")
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
  public void updateExistingReturns200() {
    long id = createProduct("PTEST-UPDATE", 1);

    given()
        .contentType("application/json")
        .body("{\"name\":\"PTEST-UPDATE-2\",\"stock\":9}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo("PTEST-UPDATE-2"))
        .body("stock", is(9));
  }

  @Test
  public void updateMissingReturns404() {
    given()
        .contentType("application/json")
        .body("{\"name\":\"NOPE\",\"stock\":1}")
        .when()
        .put(PATH + "/" + MISSING_ID)
        .then()
        .statusCode(404);
  }

  @Test
  public void updateWithoutNameReturns422() {
    long id = createProduct("PTEST-NONAME", 1);

    given()
        .contentType("application/json")
        .body("{\"stock\":2}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(422);
  }

  @Test
  public void deleteExistingReturns204AndMissingReturns404() {
    long id = createProduct("PTEST-DELETE", 1);

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH + "/" + id).then().statusCode(404);
    given().when().delete(PATH + "/" + MISSING_ID).then().statusCode(404);
  }

  @Test
  public void createDuplicateNameReturns500() {
    createProduct("PTEST-DUPLICATE", 1);

    given()
        .contentType("application/json")
        .body("{\"name\":\"PTEST-DUPLICATE\",\"stock\":2}")
        .when()
        .post(PATH)
        .then()
        .statusCode(500);
  }

  @Test
  public void errorMapperOmitsErrorFieldWhenMessageIsNull() {
    ProductResource.ErrorMapper mapper = new ProductResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    Response response = mapper.toResponse(new RuntimeException());

    assertEquals(500, response.getStatus());
    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(500, body.get("code").asInt());
    assertFalse(body.has("error"));
  }

  @Test
  public void errorMapperUsesStatusAndMessageForWebApplicationException() {
    ProductResource.ErrorMapper mapper = new ProductResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    Response response = mapper.toResponse(new WebApplicationException("boom", 404));

    assertEquals(404, response.getStatus());
    ObjectNode body = (ObjectNode) response.getEntity();
    assertEquals(404, body.get("code").asInt());
    assertEquals("boom", body.get("error").asText());
  }
}
