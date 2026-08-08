package com.fulfilment.application.monolith.products;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ProductEndpointTest {

  // @Test
  // public void testCrudProduct() {
  //   final String path = "product";

  //   // List all, should have all 3 products the database has initially:
  //   given()
  //       .when()
  //       .get(path)
  //       .then()
  //       .statusCode(200)
  //       .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

  //   // Delete the TONSTAD:
  //   given().when().delete(path + "/1").then().statusCode(204);

  //   // List all, TONSTAD should be missing now:
  //   given()
  //       .when()
  //       .get(path)
  //       .then()
  //       .statusCode(200)
  //       .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  // }
}
