package com.fulfilment.application.monolith.location;

import java.util.HashMap;
import java.util.Map;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LocationGateway implements LocationResolver {

  private static final Map<String, Location> locationsMap = new HashMap<>();

  static {
    locationsMap.put("ZWOLLE-001", new Location("ZWOLLE-001", 1, 40));
    locationsMap.put("ZWOLLE-002", new Location("ZWOLLE-002", 2, 50));
    locationsMap.put("AMSTERDAM-001", new Location("AMSTERDAM-001", 5, 100));
    locationsMap.put("AMSTERDAM-002", new Location("AMSTERDAM-002", 3, 75));
    locationsMap.put("TILBURG-001", new Location("TILBURG-001", 1, 40));
    locationsMap.put("HELMOND-001", new Location("HELMOND-001", 1, 45));
    locationsMap.put("EINDHOVEN-001", new Location("EINDHOVEN-001", 2, 70));
    locationsMap.put("VETSBY-001", new Location("VETSBY-001", 1, 90));
  }

  @Override
  public Location resolveByIdentifier(String identifier) {
    if(identifier == null || !locationsMap.containsKey(identifier.trim())) {
      return null;
    }
    return locationsMap.get(identifier.trim());
  }
}
