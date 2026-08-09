package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    WarehouseValidations.requireBaseFields(newWarehouse);

    // 1. There must be an active warehouse to replace.
    Warehouse old = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (old == null) {
      throw new WarehouseNotFoundException(
          "No active warehouse found for business unit code "
              + newWarehouse.businessUnitCode
              + ".");
    }

    // 2. Location must exist.
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WarehouseValidationException(
          "Location " + newWarehouse.location + " is not a valid location.");
    }

    // The old warehouse comes from persisted data whose numeric columns are nullable; guard them.
    int oldStock = WarehouseValidations.nullSafeInt(old.stock);
    int oldCapacity = WarehouseValidations.nullSafeInt(old.capacity);

    // 3. The new warehouse must be able to hold the stock carried over from the old one.
    if (newWarehouse.capacity < oldStock) {
      throw new WarehouseValidationException(
          "New warehouse capacity cannot accommodate the stock of the warehouse being replaced.");
    }

    // 4. The stock of the new warehouse must match the stock of the previous one.
    if (newWarehouse.stock != oldStock) {
      throw new WarehouseValidationException(
          "New warehouse stock must match the stock of the warehouse being replaced.");
    }

    // 5. The new capacity must fit the location once the old warehouse's share is freed.
    int usedCapacity =
        WarehouseValidations.usedCapacityAt(warehouseStore, location.identification);
    if (location.identification.equals(old.location)) {
      usedCapacity -= oldCapacity;
    }
    if (usedCapacity + newWarehouse.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Capacity exceeds the maximum capacity available at location "
              + location.identification
              + ".");
    }

    // Archive the old, then create the new under the same business unit code.
    old.archivedAt = LocalDateTime.now();
    warehouseStore.update(old);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }
}
