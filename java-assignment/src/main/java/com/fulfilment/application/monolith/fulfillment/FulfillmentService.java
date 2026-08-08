package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.fulfillment.exceptions.DuplicateFulfillmentException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentLimitExceededException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentNotFoundException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentValidationException;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/** Registers and manages warehouse⇄product⇄store fulfilment associations, enforcing the limits. */
@ApplicationScoped
public class FulfillmentService {

  static final int MAX_WAREHOUSES_PER_STORE_PRODUCT = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  @Inject FulfillmentRepository fulfillmentRepository;
  @Inject StoreRepository storeRepository;
  @Inject ProductRepository productRepository;
  @Inject WarehouseRepository warehouseRepository;

  public List<Fulfillment> listAll() {
    return fulfillmentRepository.listAll();
  }

  public Fulfillment getById(Long id) {
    Fulfillment fulfillment = fulfillmentRepository.findById(id);
    if (fulfillment == null) {
      throw new FulfillmentNotFoundException("Fulfillment with id " + id + " does not exist.");
    }
    return fulfillment;
  }

  public Fulfillment create(Long storeId, Long productId, String warehouseBusinessUnitCode) {
    if (storeId == null || productId == null || isBlank(warehouseBusinessUnitCode)) {
      throw new FulfillmentValidationException(
          "storeId, productId and warehouseBusinessUnitCode are required.");
    }

    Store store = storeRepository.findById(storeId);
    if (store == null) {
      throw new FulfillmentNotFoundException("Store with id " + storeId + " does not exist.");
    }
    Product product = productRepository.findById(productId);
    if (product == null) {
      throw new FulfillmentNotFoundException("Product with id " + productId + " does not exist.");
    }
    DbWarehouse warehouse =
        warehouseRepository.findActiveDbByBusinessUnitCode(warehouseBusinessUnitCode);
    if (warehouse == null) {
      throw new FulfillmentNotFoundException(
          "No active warehouse found for business unit code " + warehouseBusinessUnitCode + ".");
    }

    if (fulfillmentRepository.exists(store, product, warehouse)) {
      throw new DuplicateFulfillmentException(
          "This warehouse already fulfils this product for this store.");
    }

    // Rule 1: at most 2 distinct warehouses per (store, product). The new warehouse is not among
    // the existing ones (that would be the duplicate case handled above), so it adds one.
    if (fulfillmentRepository.countDistinctWarehousesForStoreAndProduct(store, product)
        >= MAX_WAREHOUSES_PER_STORE_PRODUCT) {
      throw new FulfillmentLimitExceededException(
          "A product can be fulfilled by at most "
              + MAX_WAREHOUSES_PER_STORE_PRODUCT
              + " warehouses per store.");
    }

    // Rule 2: at most 3 distinct warehouses per store — only counts if this warehouse is new to it.
    if (!fulfillmentRepository.isWarehouseServingStore(store, warehouse)
        && fulfillmentRepository.countDistinctWarehousesForStore(store)
            >= MAX_WAREHOUSES_PER_STORE) {
      throw new FulfillmentLimitExceededException(
          "A store can be fulfilled by at most " + MAX_WAREHOUSES_PER_STORE + " warehouses.");
    }

    // Rule 3: at most 5 distinct products per warehouse — only counts if this product is new to it.
    if (!fulfillmentRepository.isProductInWarehouse(product, warehouse)
        && fulfillmentRepository.countDistinctProductsForWarehouse(warehouse)
            >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new FulfillmentLimitExceededException(
          "A warehouse can store at most " + MAX_PRODUCTS_PER_WAREHOUSE + " types of products.");
    }

    Fulfillment fulfillment = new Fulfillment(store, product, warehouse);
    fulfillmentRepository.persist(fulfillment);
    return fulfillment;
  }

  public void delete(Long id) {
    Fulfillment fulfillment = fulfillmentRepository.findById(id);
    if (fulfillment == null) {
      throw new FulfillmentNotFoundException("Fulfillment with id " + id + " does not exist.");
    }
    fulfillmentRepository.delete(fulfillment);
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
