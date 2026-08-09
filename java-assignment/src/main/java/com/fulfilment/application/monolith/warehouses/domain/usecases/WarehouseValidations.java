package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;

/** Shared helpers for warehouse use cases (location occupancy computed by filtering getAll). */
final class WarehouseValidations {

  private WarehouseValidations() {}

  static void requireBaseFields(Warehouse warehouse) {
    if (warehouse == null) {
      throw new WarehouseValidationException("Warehouse must be provided.");
    }
    if (isBlank(warehouse.businessUnitCode)) {
      throw new WarehouseValidationException("Business unit code must be provided.");
    }
    if (isBlank(warehouse.location)) {
      throw new WarehouseValidationException("Location must be provided.");
    }
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new WarehouseValidationException("Capacity must be a positive number.");
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new WarehouseValidationException("Stock must be zero or a positive number.");
    }
  }

  /** Number of active (non-archived) warehouses currently at the given location. */
  static long countActiveWarehousesAt(WarehouseStore store, String location) {
    return store.getAll().stream().filter(w -> isActiveAt(w, location)).count();
  }

  /** Sum of capacities of active warehouses currently at the given location. */
  static int usedCapacityAt(WarehouseStore store, String location) {
    return store.getAll().stream()
        .filter(w -> isActiveAt(w, location))
        .mapToInt(w -> w.capacity == null ? 0 : w.capacity)
        .sum();
  }

  /** Unboxes a nullable capacity/stock value, treating a missing value as 0. */
  static int nullSafeInt(Integer value) {
    return value == null ? 0 : value;
  }

  private static boolean isActiveAt(Warehouse warehouse, String location) {
    return warehouse.archivedAt == null && location.equals(warehouse.location);
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
