package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;

class LocationGatewayTest {

  private final LocationGateway gateway = new LocationGateway();

  @Test
  void resolvesExistingLocation() {
    Location location = gateway.resolveByIdentifier("ZWOLLE-001");

    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  void resolvesLocationIgnoringCaseAndWhitespace() {
    Location location = gateway.resolveByIdentifier("  amsterdam-001 ");

    assertNotNull(location);
    assertEquals("AMSTERDAM-001", location.identification);
  }

  @Test
  void returnsNullForUnknownLocation() {
    assertNull(gateway.resolveByIdentifier("UNKNOWN-001"));
  }

  @Test
  void returnsNullForNullOrBlankIdentifier() {
    assertNull(gateway.resolveByIdentifier(null));
    assertNull(gateway.resolveByIdentifier("   "));
  }
}
