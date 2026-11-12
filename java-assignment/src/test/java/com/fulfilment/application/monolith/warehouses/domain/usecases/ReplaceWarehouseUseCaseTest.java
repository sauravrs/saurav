package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReplaceWarehouseUseCaseTest {

  private Store store;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    store = new Store();
    useCase = new ReplaceWarehouseUseCase(store, new Resolver());
  }

  @Test
  void archivesOldWarehouseAndCreatesReplacementWithSameBusinessUnitCode() {
    Warehouse current = warehouse("MWH.001", "AMSTERDAM-001", 40, 20);
    store.items.add(current);
    Warehouse replacement = warehouse("MWH.001", "AMSTERDAM-001", 50, 20);

    useCase.replace(replacement);

    assertNotNull(current.archivedAt);
    assertSame(current, store.updated);
    assertSame(replacement, store.created);
    assertNotNull(replacement.createdAt);
    assertNull(replacement.archivedAt);
  }

  @Test
  void rejectsMissingBusinessUnitCode() {
    Warehouse replacement = warehouse(null, "AMSTERDAM-001", 50, 20);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertEquals("Business unit code is required.", ex.getMessage());
  }

  @Test
  void rejectsWhenCurrentWarehouseDoesNotExist() {
    Warehouse replacement = warehouse("MWH.999", "AMSTERDAM-001", 50, 20);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertTrue(ex.getMessage().contains("does not exist"));
  }

  @Test
  void rejectsDifferentLocation() {
    store.items.add(warehouse("MWH.001", "AMSTERDAM-001", 40, 20));
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-002", 30, 20);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertTrue(ex.getMessage().contains("same location"));
  }

  @Test
  void rejectsUnknownCurrentLocation() {
    store.items.add(warehouse("MWH.001", "UNKNOWN-001", 40, 20));
    Warehouse replacement = warehouse("MWH.001", "UNKNOWN-001", 30, 20);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertTrue(ex.getMessage().contains("Invalid location"));
  }

  @Test
  void rejectsNonPositiveReplacementCapacity() {
    store.items.add(warehouse("MWH.001", "AMSTERDAM-001", 40, 20));
    Warehouse replacement = warehouse("MWH.001", "AMSTERDAM-001", 0, 20);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertEquals("Capacity must be greater than zero.", ex.getMessage());
  }

  @Test
  void rejectsNegativeReplacementStock() {
    store.items.add(warehouse("MWH.001", "AMSTERDAM-001", 40, 20));
    Warehouse replacement = warehouse("MWH.001", "AMSTERDAM-001", 50, -1);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertEquals("Stock cannot be negative.", ex.getMessage());
  }

  @Test
  void rejectsCapacityAboveLocationMaximum() {
    store.items.add(warehouse("MWH.001", "AMSTERDAM-001", 40, 20));
    Warehouse replacement = warehouse("MWH.001", "AMSTERDAM-001", 101, 20);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertTrue(ex.getMessage().contains("exceeds maximum capacity"));
  }

  @Test
  void rejectsReplacementCapacityBelowExistingStock() {
    store.items.add(warehouse("MWH.001", "AMSTERDAM-001", 40, 20));
    Warehouse replacement = warehouse("MWH.001", "AMSTERDAM-001", 19, 20);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertTrue(ex.getMessage().contains("accommodate the existing stock"));
  }

  @Test
  void rejectsChangedStock() {
    store.items.add(warehouse("MWH.001", "AMSTERDAM-001", 40, 20));
    Warehouse replacement = warehouse("MWH.001", "AMSTERDAM-001", 50, 19);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertTrue(ex.getMessage().contains("stock must match"));
  }

  private static Warehouse warehouse(String code, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  private static class Resolver implements LocationResolver {
    @Override
    public Location resolveByIdentifier(String identifier) {
      if ("AMSTERDAM-001".equals(identifier)) return new Location(identifier, 5, 100);
      if ("ZWOLLE-002".equals(identifier)) return new Location(identifier, 2, 50);
      return null;
    }
  }

  private static class Store implements WarehouseStore {
    final List<Warehouse> items = new ArrayList<>();
    Warehouse updated;
    Warehouse created;

    @Override public List<Warehouse> getAll() { return new ArrayList<>(items); }
    @Override public void create(Warehouse warehouse) { created = warehouse; items.add(warehouse); }
    @Override public void update(Warehouse warehouse) { updated = warehouse; }
    @Override public void remove(Warehouse warehouse) { items.remove(warehouse); }
    @Override public Warehouse findByBusinessUnitCode(String buCode) {
      return items.stream().filter(w -> w.archivedAt == null && java.util.Objects.equals(w.businessUnitCode, buCode)).findFirst().orElse(null);
    }
    @Override public Warehouse findById(String id) { return null; }
  }
}
