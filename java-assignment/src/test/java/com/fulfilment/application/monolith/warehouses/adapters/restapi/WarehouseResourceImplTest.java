package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Integration tests for the {@code /warehouse} endpoints. */
@QuarkusTest
public class WarehouseResourceImplTest {

  private static final String PATH = "warehouse";

  private String body(String bu, String location, int capacity, int stock) {
    return "{\"businessUnitCode\":\""
        + bu
        + "\",\"location\":\""
        + location
        + "\",\"capacity\":"
        + capacity
        + ",\"stock\":"
        + stock
        + "}";
  }

  @Test
  public void listReturnsSeededWarehouses() {
    given().when().get(PATH).then().statusCode(200);
  }

  @Test
  public void createValidReturns201AndIsRetrievable() {
    given()
        .contentType("application/json")
        .body(body("REST.CREATE", "AMSTERDAM-002", 30, 10))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("businessUnitCode", equalTo("REST.CREATE"));

    given()
        .when()
        .get(PATH + "/REST.CREATE")
        .then()
        .statusCode(200)
        .body("location", equalTo("AMSTERDAM-002"))
        .body("capacity", is(30));
  }

  @Test
  public void createDuplicateBusinessUnitCodeReturns409() {
    given()
        .contentType("application/json")
        .body(body("MWH.001", "AMSTERDAM-002", 10, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(409);
  }

  @Test
  public void createWithInvalidLocationReturns400() {
    given()
        .contentType("application/json")
        .body(body("REST.BADLOC", "NOWHERE", 10, 1))
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void getUnknownReturns404() {
    given().when().get(PATH + "/UNKNOWN.CODE").then().statusCode(404);
  }

  @Test
  public void archiveReturns204ThenGetReturns404() {
    given()
        .contentType("application/json")
        .body(body("REST.ARCHIVE", "EINDHOVEN-001", 10, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given().when().delete(PATH + "/REST.ARCHIVE").then().statusCode(204);
    given().when().get(PATH + "/REST.ARCHIVE").then().statusCode(404);
  }

  @Test
  public void archiveUnknownReturns404() {
    given().when().delete(PATH + "/UNKNOWN.CODE").then().statusCode(404);
  }

  @Test
  public void replaceValidReturns200WithNewData() {
    // Replace seeded MWH.023 (TILBURG-001, cap 30, stock 27) with a larger-capacity unit.
    given()
        .contentType("application/json")
        .body(body("MWH.023", "TILBURG-001", 35, 27))
        .when()
        .post(PATH + "/MWH.023/replacement")
        .then()
        .statusCode(200)
        .body("capacity", is(35));

    given().when().get(PATH + "/MWH.023").then().statusCode(200).body("capacity", is(35));
  }

  @Test
  public void replaceUnknownReturns404() {
    given()
        .contentType("application/json")
        .body(body("UNKNOWN.CODE", "AMSTERDAM-001", 10, 1))
        .when()
        .post(PATH + "/UNKNOWN.CODE/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  public void replaceWithStockMismatchReturns400() {
    given()
        .contentType("application/json")
        .body(body("REST.REPL", "VETSBY-001", 50, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    // old stock is 5; new stock 6 does not match
    given()
        .contentType("application/json")
        .body(body("REST.REPL", "VETSBY-001", 50, 6))
        .when()
        .post(PATH + "/REST.REPL/replacement")
        .then()
        .statusCode(400);
  }
}
