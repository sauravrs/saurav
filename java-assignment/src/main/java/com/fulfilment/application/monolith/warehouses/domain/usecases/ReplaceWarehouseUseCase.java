package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse == null || newWarehouse.businessUnitCode == null) {
      throw new IllegalArgumentException("Business unit code is required.");
    }

    Warehouse current = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (current == null) {
      throw new IllegalArgumentException(
          "Active warehouse with business unit code "
              + newWarehouse.businessUnitCode
              + " does not exist.");
    }

    if (!Objects.equals(current.location, newWarehouse.location)) {
      throw new IllegalArgumentException(
          "Replacement warehouse must use the same location as the current warehouse.");
    }

    Location location = locationResolver.resolveByIdentifier(current.location);
    if (location == null) {
      throw new IllegalArgumentException("Invalid location: " + current.location);
    }

    if (newWarehouse.capacity == null || newWarehouse.capacity <= 0) {
      throw new IllegalArgumentException("Capacity must be greater than zero.");
    }

    if (newWarehouse.stock == null || newWarehouse.stock < 0) {
      throw new IllegalArgumentException("Stock cannot be negative.");
    }

    if (newWarehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException(
          "Warehouse capacity exceeds maximum capacity of location "
              + location.identification);
    }

    if (newWarehouse.capacity < current.stock) {
      throw new IllegalArgumentException(
          "Replacement warehouse capacity must accommodate the existing stock.");
    }

    if (!Objects.equals(newWarehouse.stock, current.stock)) {
      throw new IllegalArgumentException(
          "Replacement warehouse stock must match the stock of the current warehouse.");
    }

    // Archive the old record and create a new record with the same business unit code.
    current.archivedAt = LocalDateTime.now();
    warehouseStore.update(current);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }
}
