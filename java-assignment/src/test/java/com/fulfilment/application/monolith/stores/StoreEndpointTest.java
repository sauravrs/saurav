package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class StoreEndpointTest {

  @Test
  void listsInitialStores() {
    given()
        .when()
        .get("store")
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  void createsStore() {
    String name = "INTERVIEW-STORE-001";

    given()
        .contentType("application/json")
        .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":25}")
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .body("name", equalTo(name))
        .body("quantityProductsInStock", equalTo(25));

    given().when().get("store").then().statusCode(200).body(containsString(name));
  }

  @Test
  void updatesStore() {
    String name = "INTERVIEW-UPDATE-001";

    String id =
        given()
            .contentType("application/json")
            .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":10}")
            .when()
            .post("store")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given()
        .contentType("application/json")
        .body("{\"name\":\"" + name + "-UPDATED\",\"quantityProductsInStock\":30}")
        .when()
        .put("store/" + id)
        .then()
        .statusCode(200)
        .body("name", equalTo(name + "-UPDATED"))
        .body("quantityProductsInStock", equalTo(30));
  }

  @Test
  void returns404ForMissingStore() {
    given().when().get("store/999999").then().statusCode(404);
  }

  @Test
  void deletesStore() {
    String name = "INTERVIEW-DELETE-001";

    String id =
        given()
            .contentType("application/json")
            .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":5}")
            .when()
            .post("store")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given().when().delete("store/" + id).then().statusCode(204);
    given().when().get("store").then().statusCode(200).body(not(containsString(name)));
  }
}
