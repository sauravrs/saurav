package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class WarehouseEndpointIT {

  @Test
  void listsInitialActiveWarehouses() {
    given()
        .when()
        .get("warehouse")
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"));
  }

  @Test
  void createsWarehouseAndReturns201() {
    String code = "IT-CREATE-001";

    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"" + code + "\",\"location\":\"VETSBY-001\",\"capacity\":40,\"stock\":10}")
        .when()
        .post("warehouse")
        .then()
        .statusCode(201)
        .body("businessUnitCode", equalTo(code))
        .body("location", equalTo("VETSBY-001"))
        .body("capacity", equalTo(40))
        .body("stock", equalTo(10));

    given()
        .when()
        .get("warehouse")
        .then()
        .statusCode(200)
        .body(containsString(code));
  }

  @Test
  void rejectsInvalidWarehouseWith400() {
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"IT-INVALID-001\",\"location\":\"VETSBY-001\",\"capacity\":10,\"stock\":11}")
        .when()
        .post("warehouse")
        .then()
        .statusCode(400)
        .body("code", equalTo(400));
  }

  @Test
  void getsExistingWarehouse() {
    given()
        .when()
        .get("warehouse/1")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.001"))
        .body("location", equalTo("ZWOLLE-001"));
  }

  @Test
  void returns404ForMissingWarehouse() {
    given()
        .when()
        .get("warehouse/999999")
        .then()
        .statusCode(404)
        .body("code", equalTo(404));
  }

  @Test
  void archivesWarehouseAndReturns404AfterArchive() {
    String code = "IT-ARCHIVE-001";

    String warehouseId =
        given()
            .contentType("application/json")
            .body("{\"businessUnitCode\":\"" + code + "\",\"location\":\"HELMOND-001\",\"capacity\":20,\"stock\":5}")
            .when()
            .post("warehouse")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
            .toString();

    given().when().delete("warehouse/" + warehouseId).then().statusCode(204);
    given().when().get("warehouse/" + warehouseId).then().statusCode(404);

    given()
        .when()
        .get("warehouse")
        .then()
        .statusCode(200)
        .body(not(containsString(code)));
  }

  @Test
  void replacesWarehouseAndKeepsBusinessUnitCode() {
    String code = "IT-REPLACE-001";

    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"" + code + "\",\"location\":\"EINDHOVEN-001\",\"capacity\":30,\"stock\":10}")
        .when()
        .post("warehouse")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body("{\"location\":\"EINDHOVEN-001\",\"capacity\":40,\"stock\":10}")
        .when()
        .post("warehouse/" + code + "/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo(code))
        .body("capacity", equalTo(40))
        .body("stock", equalTo(10));

    given()
        .when()
        .get("warehouse")
        .then()
        .statusCode(200)
        .body(containsString(code));
  }

  @Test
  void rejectsReplacementWithDifferentLocation() {
    String code = "IT-REPLACE-INVALID-001";

    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"" + code + "\",\"location\":\"AMSTERDAM-002\",\"capacity\":20,\"stock\":5}")
        .when()
        .post("warehouse")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body("{\"location\":\"AMSTERDAM-001\",\"capacity\":20,\"stock\":5}")
        .when()
        .post("warehouse/" + code + "/replacement")
        .then()
        .statusCode(400)
        .body("code", equalTo(400));
  }
}
