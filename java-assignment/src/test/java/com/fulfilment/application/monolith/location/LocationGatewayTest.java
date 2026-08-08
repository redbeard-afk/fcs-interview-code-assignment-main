package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;


public class LocationGatewayTest {

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    // given
    LocationGateway locationGateway = new LocationGateway();

    // when
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    // then
    assertEquals(location.identification, "ZWOLLE-001");
    
  }

  @Test
  public void testWhenResolveNullLocationShouldReturnNull() {
    // given
    LocationGateway locationGateway = new LocationGateway();

    assertNull(locationGateway.resolveByIdentifier(null));

    assertNull(locationGateway.resolveByIdentifier(""));

  }

  @Test
  public void testWhenResolveBlankLocationShouldReturnNull() {
    // given
    LocationGateway locationGateway = new LocationGateway();

    // then
    assertNull(locationGateway.resolveByIdentifier(" "));
    
  }

  @Test
  public void testWhenResolveExistingLocationWithSpaceShouldReturn() {
    // given
    LocationGateway locationGateway = new LocationGateway();

    // when
    Location location = locationGateway.resolveByIdentifier(" ZWOLLE-001    ");

    // then
    assertEquals(location.identification, "ZWOLLE-001");
    
  }
}
