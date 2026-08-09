package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FulfillmentRepository implements PanacheRepository<Fulfillment> {

  public boolean exists(Store store, Product product, DbWarehouse warehouse) {
    return count(
            "store = ?1 and product = ?2 and warehouse = ?3", store, product, warehouse)
        > 0;
  }

  /** Distinct active warehouses already fulfilling a given product for a given store. */
  public long countDistinctWarehousesForStoreAndProduct(Store store, Product product) {
    return find("store = ?1 and product = ?2", store, product).stream()
        .filter(f -> f.warehouse.archivedAt == null)
        .map(f -> f.warehouse.id)
        .distinct()
        .count();
  }

  /** Distinct active warehouses already fulfilling a given store (across all products). */
  public long countDistinctWarehousesForStore(Store store) {
    return find("store = ?1", store).stream()
        .filter(f -> f.warehouse.archivedAt == null)
        .map(f -> f.warehouse.id)
        .distinct()
        .count();
  }

  public boolean isWarehouseServingStore(Store store, DbWarehouse warehouse) {
    return count("store = ?1 and warehouse = ?2", store, warehouse) > 0;
  }

  /** Distinct products already stored by a given warehouse (across all stores). */
  public long countDistinctProductsForWarehouse(DbWarehouse warehouse) {
    return find("warehouse = ?1", warehouse).stream().map(f -> f.product.id).distinct().count();
  }

  public boolean isProductInWarehouse(Product product, DbWarehouse warehouse) {
    return count("product = ?1 and warehouse = ?2", product, warehouse) > 0;
  }
}
