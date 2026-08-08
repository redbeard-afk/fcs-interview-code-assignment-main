package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    persist(DbWarehouse.fromWarehouse(warehouse));
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse entity = findActiveEntityByBusinessUnitCode(warehouse.businessUnitCode);
    if (entity == null) {
      return;
    }
    // Managed entity: dirty checking flushes these changes at commit.
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt;
    entity.archivedAt = warehouse.archivedAt;
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse entity = findActiveEntityByBusinessUnitCode(warehouse.businessUnitCode);
    if (entity != null) {
      delete(entity);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse entity = findActiveEntityByBusinessUnitCode(buCode);
    return entity == null ? null : entity.toWarehouse();
  }

  /** Returns the single active (not archived) warehouse for the business unit code, or null. */
  private DbWarehouse findActiveEntityByBusinessUnitCode(String buCode) {
    return find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
  }
}
