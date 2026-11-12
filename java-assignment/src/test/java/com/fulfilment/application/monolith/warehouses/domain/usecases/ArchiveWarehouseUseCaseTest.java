package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArchiveWarehouseUseCaseTest {

  private RecordingStore store;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    store = new RecordingStore();
    useCase = new ArchiveWarehouseUseCase(store);
  }

  @Test
  void archivesActiveWarehouseAndUpdatesStore() {
    Warehouse warehouse = warehouse("MWH.001");

    useCase.archive(warehouse);

    assertNotNull(warehouse.archivedAt);
    assertSame(warehouse, store.updated);
  }

  @Test
  void rejectsNullWarehouse() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.archive(null));
    assertEquals("Warehouse not found.", ex.getMessage());
    assertNull(store.updated);
  }

  @Test
  void doesNothingWhenAlreadyArchived() {
    Warehouse warehouse = warehouse("MWH.001");
    warehouse.archivedAt = java.time.LocalDateTime.now().minusDays(1);

    useCase.archive(warehouse);

    assertNull(store.updated);
  }

  private static Warehouse warehouse(String code) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 20;
    warehouse.stock = 5;
    return warehouse;
  }

  private static class RecordingStore implements WarehouseStore {
    Warehouse updated;
    @Override public List<Warehouse> getAll() { return new ArrayList<>(); }
    @Override public void create(Warehouse warehouse) { }
    @Override public void update(Warehouse warehouse) { updated = warehouse; }
    @Override public void remove(Warehouse warehouse) { }
    @Override public Warehouse findByBusinessUnitCode(String buCode) { return null; }
    @Override public Warehouse findById(String id) { return null; }
  }
}
