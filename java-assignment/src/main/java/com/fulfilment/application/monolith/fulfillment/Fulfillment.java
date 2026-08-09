package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Association declaring that a {@link DbWarehouse} fulfils a {@link Product} for a {@link Store}.
 *
 * <p>The {@code (store, product, warehouse)} triple is unique — a given warehouse fulfils a given
 * product for a given store at most once.
 */
@Entity
@Table(
    name = "fulfillment",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"store_id", "product_id", "warehouse_id"}))
public class Fulfillment extends PanacheEntity {

  @ManyToOne public Store store;

  @ManyToOne public Product product;

  @ManyToOne public DbWarehouse warehouse;

  public Fulfillment() {}

  public Fulfillment(Store store, Product product, DbWarehouse warehouse) {
    this.store = store;
    this.product = product;
    this.warehouse = warehouse;
  }
}
