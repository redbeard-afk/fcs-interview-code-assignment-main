package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.DuplicateBusinessUnitCodeException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    WarehouseValidations.requireBaseFields(warehouse);

    // 1. Business unit code must not already belong to an active warehouse.
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new DuplicateBusinessUnitCodeException(
          "A warehouse with business unit code " + warehouse.businessUnitCode + " already exists.");
    }

    // 2. Location must exist.
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WarehouseValidationException(
          "Location " + warehouse.location + " is not a valid location.");
    }

    // 3. The location must still have room for another warehouse.
    if (WarehouseValidations.countActiveWarehousesAt(warehouseStore, location.identification)
        >= location.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
          "The maximum number of warehouses for location "
              + location.identification
              + " has been reached.");
    }

    // 4. The new capacity must fit within the location's total capacity.
    int usedCapacity =
        WarehouseValidations.usedCapacityAt(warehouseStore, location.identification);
    if (usedCapacity + warehouse.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Capacity exceeds the maximum capacity available at location "
              + location.identification
              + ".");
    }

    // 5. The warehouse must be able to hold the stock informed.
    if (warehouse.stock > warehouse.capacity) {
      throw new WarehouseValidationException("Stock exceeds the warehouse capacity.");
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    warehouseStore.create(warehouse);
  }
}
