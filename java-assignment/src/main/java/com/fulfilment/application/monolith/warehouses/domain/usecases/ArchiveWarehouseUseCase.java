package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyArchivedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    if (warehouse == null || warehouse.id == null) {
      throw new WarehouseValidationException("Warehouse id must be provided.");
    }

    Warehouse existing = warehouseStore.findByDbId(warehouse.id);
    if (existing == null) {
      throw new WarehouseNotFoundException(
          "No warehouse found for id " + warehouse.id + ".");
    }
    if (existing.archivedAt != null) {
      throw new WarehouseAlreadyArchivedException(
          "Warehouse " + warehouse.id + " is already archived.");
    }

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);
  }
}
