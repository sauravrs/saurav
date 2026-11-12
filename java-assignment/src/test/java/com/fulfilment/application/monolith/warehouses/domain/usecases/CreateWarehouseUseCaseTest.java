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

class CreateWarehouseUseCaseTest {

  private InMemoryWarehouseStore store;
  private StubLocationResolver locations;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    store = new InMemoryWarehouseStore();
    locations = new StubLocationResolver();
    useCase = new CreateWarehouseUseCase(store, locations);
  }

  @Test
  void createsValidWarehouseAndSetsCreatedAt() {
    Warehouse warehouse = warehouse("MWH.100", "AMSTERDAM-001", 30, 10);

    useCase.create(warehouse);

    assertEquals(1, store.created.size());
    assertSame(warehouse, store.created.get(0));
    assertNotNull(warehouse.createdAt);
    assertNull(warehouse.archivedAt);
  }

  @Test
  void rejectsNullPayload() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(null));
    assertEquals("Warehouse payload is required.", ex.getMessage());
  }

  @Test
  void rejectsMissingBusinessUnitCode() {
    Warehouse warehouse = warehouse(null, "AMSTERDAM-001", 30, 10);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertEquals("Business unit code is required.", ex.getMessage());
  }

  @Test
  void rejectsMissingLocation() {
    Warehouse warehouse = warehouse("MWH.100", null, 30, 10);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertEquals("Location is required.", ex.getMessage());
  }

  @Test
  void rejectsNonPositiveCapacity() {
    Warehouse warehouse = warehouse("MWH.100", "AMSTERDAM-001", 0, 0);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertEquals("Capacity must be greater than zero.", ex.getMessage());
  }

  @Test
  void rejectsNegativeStock() {
    Warehouse warehouse = warehouse("MWH.100", "AMSTERDAM-001", 30, -1);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertEquals("Stock cannot be negative.", ex.getMessage());
  }

  @Test
  void rejectsDuplicateActiveBusinessUnitCode() {
    store.items.add(warehouse("MWH.100", "AMSTERDAM-001", 20, 5));
    Warehouse warehouse = warehouse("MWH.100", "AMSTERDAM-001", 20, 5);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("already exists"));
  }

  @Test
  void rejectsUnknownLocation() {
    locations.locations.remove("AMSTERDAM-001");
    Warehouse warehouse = warehouse("MWH.100", "AMSTERDAM-001", 20, 5);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("Invalid location"));
  }

  @Test
  void rejectsWhenMaximumWarehouseCountIsReached() {
    locations.locations.put("ZWOLLE-001", new Location("ZWOLLE-001", 1, 40));
    store.items.add(warehouse("MWH.001", "ZWOLLE-001", 20, 5));

    Warehouse warehouse = warehouse("MWH.100", "ZWOLLE-001", 10, 5);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("Maximum number of warehouses"));
  }

  @Test
  void rejectsWarehouseCapacityAboveLocationMaximum() {
    Warehouse warehouse = warehouse("MWH.100", "ZWOLLE-001", 41, 10);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("exceeds maximum capacity"));
  }

  @Test
  void rejectsTotalCapacityAboveLocationMaximum() {
    store.items.add(warehouse("MWH.001", "ZWOLLE-002", 30, 5));
    Warehouse warehouse = warehouse("MWH.100", "ZWOLLE-002", 25, 5);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("Total warehouse capacity"));
  }

  @Test
  void rejectsStockAboveCapacity() {
    Warehouse warehouse = warehouse("MWH.100", "AMSTERDAM-001", 20, 21);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertEquals("Warehouse stock cannot exceed warehouse capacity.", ex.getMessage());
  }

  private static Warehouse warehouse(String buCode, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  private static class StubLocationResolver implements LocationResolver {
    final java.util.Map<String, Location> locations = new java.util.HashMap<>();

    StubLocationResolver() {
      locations.put("AMSTERDAM-001", new Location("AMSTERDAM-001", 5, 100));
      locations.put("ZWOLLE-001", new Location("ZWOLLE-001", 1, 40));
      locations.put("ZWOLLE-002", new Location("ZWOLLE-002", 2, 50));
    }

    @Override
    public Location resolveByIdentifier(String identifier) {
      return locations.get(identifier);
    }
  }

  private static class InMemoryWarehouseStore implements WarehouseStore {
    final List<Warehouse> items = new ArrayList<>();
    final List<Warehouse> created = new ArrayList<>();
    final List<Warehouse> updated = new ArrayList<>();

    @Override public List<Warehouse> getAll() { return new ArrayList<>(items); }
    @Override public void create(Warehouse warehouse) { created.add(warehouse); items.add(warehouse); }
    @Override public void update(Warehouse warehouse) { updated.add(warehouse); }
    @Override public void remove(Warehouse warehouse) { items.remove(warehouse); }
    @Override public Warehouse findByBusinessUnitCode(String buCode) {
      return items.stream()
          .filter(w -> w.archivedAt == null && java.util.Objects.equals(w.businessUnitCode, buCode))
          .findFirst().orElse(null);
    }
    @Override public Warehouse findById(String id) { return null; }
  }
}
