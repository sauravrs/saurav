package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return find("archivedAt is null order by createdAt desc")
        .list()
        .stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse entity = toDbWarehouse(warehouse);
    persist(entity);
    warehouse.id = entity.id == null ? null : entity.id.toString();
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse entity =
        find("businessUnitCode = ?1 order by createdAt desc", warehouse.businessUnitCode)
            .firstResult();

    if (entity == null) {
      throw new IllegalArgumentException(
          "Warehouse with business unit code " + warehouse.businessUnitCode + " does not exist.");
    }

    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt;
    entity.archivedAt = warehouse.archivedAt;
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse entity =
        find("businessUnitCode = ?1 order by createdAt desc", warehouse.businessUnitCode)
            .firstResult();

    if (entity != null) {
      delete(entity);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    if (buCode == null || buCode.isBlank()) {
      return null;
    }

    return find("businessUnitCode = ?1 and archivedAt is null order by createdAt desc", buCode)
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }

  @Override
  public Warehouse findById(String id) {
    try {
      Long dbId = Long.valueOf(id);
      DbWarehouse entity = findById(dbId);
      return entity == null ? null : entity.toWarehouse();
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private DbWarehouse toDbWarehouse(Warehouse warehouse) {
    DbWarehouse entity = new DbWarehouse();
    entity.businessUnitCode = warehouse.businessUnitCode;
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt == null ? LocalDateTime.now() : warehouse.createdAt;
    entity.archivedAt = warehouse.archivedAt;
    return entity;
  }
}
