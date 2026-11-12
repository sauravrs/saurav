package com.fulfilment.application.monolith.warehouses.adapters.restapi;

public class WarehouseNotFoundException extends RuntimeException {
  public WarehouseNotFoundException(String message) {
    super(message);
  }
}
