package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Integration tests for the {@code /fulfillment} endpoints. */
@QuarkusTest
public class FulfillmentResourceTest {

  private static final String PATH = "fulfillment";

  private String body(Long storeId, Long productId, String warehouse) {
    return "{\"storeId\":"
        + storeId
        + ",\"productId\":"
        + productId
        + ",\"warehouseBusinessUnitCode\":"
        + (warehouse == null ? "null" : "\"" + warehouse + "\"")
        + "}";
  }

  @Test
  public void createGetDeleteLifecycle() {
    long id =
        given()
            .contentType("application/json")
            .body(body(2L, 2L, "MWH.012"))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

    given().when().get(PATH + "/" + id).then().statusCode(200);
    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  public void listReturnsOk() {
    given().when().get(PATH).then().statusCode(200);
  }

  @Test
  public void createWithMissingFieldReturns400() {
    given()
        .contentType("application/json")
        .body(body(null, 2L, "MWH.001"))
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void createWithUnknownStoreReturns404() {
    given()
        .contentType("application/json")
        .body(body(9999L, 1L, "MWH.001"))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  public void createWithUnknownProductReturns404() {
    given()
        .contentType("application/json")
        .body(body(1L, 9999L, "MWH.001"))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  public void createWithUnknownWarehouseReturns404() {
    given()
        .contentType("application/json")
        .body(body(1L, 1L, "UNKNOWN.CODE"))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  public void duplicateAssociationReturns409() {
    given()
        .contentType("application/json")
        .body(body(3L, 3L, "MWH.023"))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(body(3L, 3L, "MWH.023"))
        .when()
        .post(PATH)
        .then()
        .statusCode(409);
  }

  @Test
  public void thirdWarehouseForSameStoreProductReturns409() {
    // store 1, product 1 may be fulfilled by at most 2 warehouses
    given()
        .contentType("application/json")
        .body(body(1L, 1L, "MWH.001"))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);
    given()
        .contentType("application/json")
        .body(body(1L, 1L, "MWH.012"))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);
    given()
        .contentType("application/json")
        .body(body(1L, 1L, "MWH.023"))
        .when()
        .post(PATH)
        .then()
        .statusCode(409);
  }
}
