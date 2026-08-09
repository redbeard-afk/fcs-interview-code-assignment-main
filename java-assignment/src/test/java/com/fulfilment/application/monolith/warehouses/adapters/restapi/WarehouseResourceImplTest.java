package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Integration tests for the {@code /warehouse} endpoints. */
@QuarkusTest
public class WarehouseResourceImplTest {

  private static final String PATH = "warehouse";
  private static final long MISSING_ID = 999_999L;

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

  /** Creates a warehouse and returns its (numeric) id as a path segment. */
  private String createReturningId(String bu, String location, int capacity, int stock) {
    return given()
        .contentType("application/json")
        .body(body(bu, location, capacity, stock))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getString("id");
  }

  @Test
  public void listReturnsSeededWarehouses() {
    given().when().get(PATH).then().statusCode(200);
  }

  @Test
  public void createValidReturns201AndIsRetrievableById() {
    String id =
        given()
            .contentType("application/json")
            .body(body("REST.CREATE", "AMSTERDAM-002", 30, 10))
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .body("businessUnitCode", equalTo("REST.CREATE"))
            .body("id", notNullValue()) // the OpenAPI schema declares id
            .extract()
            .jsonPath()
            .getString("id");

    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("location", equalTo("AMSTERDAM-002"))
        .body("capacity", is(30))
        .body("id", equalTo(id));
  }

  @Test
  public void listExcludesArchivedWarehouses() {
    String id = createReturningId("REST.LISTARCH", "HELMOND-001", 10, 1);

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    // Once archived, it must not appear in the register listing.
    given().when().get(PATH).then().statusCode(200).body(not(containsString("REST.LISTARCH")));
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
    given().when().get(PATH + "/" + MISSING_ID).then().statusCode(404);
  }

  @Test
  public void getNonNumericIdReturns404() {
    given().when().get(PATH + "/NOT-A-NUMBER").then().statusCode(404);
  }

  @Test
  public void archiveReturns204ThenGetReturns404() {
    String id = createReturningId("REST.ARCHIVE", "EINDHOVEN-001", 10, 5);

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  public void archiveUnknownReturns404() {
    given().when().delete(PATH + "/" + MISSING_ID).then().statusCode(404);
  }

  @Test
  public void archiveAlreadyArchivedReturns409() {
    String id = createReturningId("REST.DBLARCH", "HELMOND-001", 10, 5);

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    // second archive of the same row: already archived
    given().when().delete(PATH + "/" + id).then().statusCode(409);
  }

  @Test
  public void replaceValidReturns200WithNewData() {
    // Replace seeded MWH.023 (TILBURG-001, cap 30, stock 27) with a larger-capacity unit.
    String newId =
        given()
            .contentType("application/json")
            .body(body("MWH.023", "TILBURG-001", 35, 27))
            .when()
            .post(PATH + "/MWH.023/replacement")
            .then()
            .statusCode(200)
            .body("capacity", is(35))
            .extract()
            .jsonPath()
            .getString("id");

    given().when().get(PATH + "/" + newId).then().statusCode(200).body("capacity", is(35));
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
