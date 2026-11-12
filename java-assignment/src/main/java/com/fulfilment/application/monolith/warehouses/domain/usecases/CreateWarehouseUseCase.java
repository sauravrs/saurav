package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Objects;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(
      WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    validateRequiredFields(warehouse);

    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new IllegalArgumentException(
          "Active warehouse with business unit code "
              + warehouse.businessUnitCode
              + " already exists.");
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new IllegalArgumentException("Invalid location: " + warehouse.location);
    }

    long activeWarehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> Objects.equals(w.location, location.identification))
            .count();

    if (activeWarehousesAtLocation >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException(
          "Maximum number of warehouses reached for location " + location.identification);
    }

    if (warehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException(
          "Warehouse capacity exceeds maximum capacity of location "
              + location.identification);
    }

    int existingCapacity =
        warehouseStore.getAll().stream()
            .filter(w -> Objects.equals(w.location, location.identification))
            .mapToInt(w -> w.capacity == null ? 0 : w.capacity)
            .sum();

    if (existingCapacity + warehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException(
          "Total warehouse capacity exceeds maximum capacity of location "
              + location.identification);
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new IllegalArgumentException("Warehouse stock cannot exceed warehouse capacity.");
    }

    warehouse.createdAt = warehouse.createdAt == null ? java.time.LocalDateTime.now() : warehouse.createdAt;
    warehouse.archivedAt = null;

    warehouseStore.create(warehouse);
  }

  private void validateRequiredFields(Warehouse warehouse) {
    if (warehouse == null) {
      throw new IllegalArgumentException("Warehouse payload is required.");
    }
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new IllegalArgumentException("Business unit code is required.");
    }
    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new IllegalArgumentException("Location is required.");
    }
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new IllegalArgumentException("Capacity must be greater than zero.");
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new IllegalArgumentException("Stock cannot be negative.");
    }
  }
}
