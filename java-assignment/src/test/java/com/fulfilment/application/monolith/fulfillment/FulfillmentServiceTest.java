package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FulfillmentServiceTest {

  private FulfillmentRepository fulfillmentRepository;
  private StoreRepository storeRepository;
  private ProductRepository productRepository;
  private WarehouseRepository warehouseRepository;
  private FulfillmentService service;

  private Store store;
  private Product product;
  private DbWarehouse warehouse;

  @BeforeEach
  public void setUp() {
    fulfillmentRepository = mock(FulfillmentRepository.class);
    storeRepository = mock(StoreRepository.class);
    productRepository = mock(ProductRepository.class);
    warehouseRepository = mock(WarehouseRepository.class);

    service = new FulfillmentService();
    service.fulfillmentRepository = fulfillmentRepository;
    service.storeRepository = storeRepository;
    service.productRepository = productRepository;
    service.warehouseRepository = warehouseRepository;

    store = new Store("TONSTAD");
    store.id = 1L;
    product = new Product("KALLAX");
    product.id = 2L;
    warehouse = new DbWarehouse();
    warehouse.id = 3L;
    warehouse.businessUnitCode = "MWH.001";

    when(storeRepository.findById(1L)).thenReturn(store);
    when(productRepository.findById(2L)).thenReturn(product);
    when(warehouseRepository.findActiveDbByBusinessUnitCode("MWH.001")).thenReturn(warehouse);
  }

  @Test
  public void createsWhenWithinAllLimits() {
    Fulfillment result = service.create(1L, 2L, "MWH.001");

    assertSame(store, result.store);
    assertSame(product, result.product);
    assertSame(warehouse, result.warehouse);
    verify(fulfillmentRepository).persist(result);
  }

  @Test
  public void rejectsMissingFields() {
    assertThrows(
        FulfillmentValidationException.class, () -> service.create(null, 2L, "MWH.001"));
    assertThrows(FulfillmentValidationException.class, () -> service.create(1L, null, "MWH.001"));
    assertThrows(FulfillmentValidationException.class, () -> service.create(1L, 2L, "  "));
    verify(fulfillmentRepository, never()).persist(any(Fulfillment.class));
  }

  @Test
  public void rejectsUnknownStore() {
    when(storeRepository.findById(1L)).thenReturn(null);
    assertThrows(FulfillmentNotFoundException.class, () -> service.create(1L, 2L, "MWH.001"));
  }

  @Test
  public void rejectsUnknownProduct() {
    when(productRepository.findById(2L)).thenReturn(null);
    assertThrows(FulfillmentNotFoundException.class, () -> service.create(1L, 2L, "MWH.001"));
  }

  @Test
  public void rejectsUnknownWarehouse() {
    when(warehouseRepository.findActiveDbByBusinessUnitCode("MWH.001")).thenReturn(null);
    assertThrows(FulfillmentNotFoundException.class, () -> service.create(1L, 2L, "MWH.001"));
  }

  @Test
  public void rejectsDuplicateAssociation() {
    when(fulfillmentRepository.exists(store, product, warehouse)).thenReturn(true);
    assertThrows(DuplicateFulfillmentException.class, () -> service.create(1L, 2L, "MWH.001"));
    verify(fulfillmentRepository, never()).persist(any(Fulfillment.class));
  }

  @Test
  public void rejectsMoreThanTwoWarehousesPerStoreProduct() {
    when(fulfillmentRepository.countDistinctWarehousesForStoreAndProduct(store, product))
        .thenReturn(2L);
    assertThrows(FulfillmentLimitExceededException.class, () -> service.create(1L, 2L, "MWH.001"));
    verify(fulfillmentRepository, never()).persist(any(Fulfillment.class));
  }

  @Test
  public void rejectsMoreThanThreeWarehousesPerStore() {
    when(fulfillmentRepository.isWarehouseServingStore(store, warehouse)).thenReturn(false);
    when(fulfillmentRepository.countDistinctWarehousesForStore(store)).thenReturn(3L);
    assertThrows(FulfillmentLimitExceededException.class, () -> service.create(1L, 2L, "MWH.001"));
    verify(fulfillmentRepository, never()).persist(any(Fulfillment.class));
  }

  @Test
  public void allowsWhenWarehouseAlreadyServesStoreEvenAtStoreLimit() {
    when(fulfillmentRepository.isWarehouseServingStore(store, warehouse)).thenReturn(true);
    when(fulfillmentRepository.countDistinctWarehousesForStore(store)).thenReturn(5L);

    Fulfillment result = service.create(1L, 2L, "MWH.001");

    verify(fulfillmentRepository).persist(result);
  }

  @Test
  public void rejectsMoreThanFiveProductsPerWarehouse() {
    when(fulfillmentRepository.isProductInWarehouse(product, warehouse)).thenReturn(false);
    when(fulfillmentRepository.countDistinctProductsForWarehouse(warehouse)).thenReturn(5L);
    assertThrows(FulfillmentLimitExceededException.class, () -> service.create(1L, 2L, "MWH.001"));
    verify(fulfillmentRepository, never()).persist(any(Fulfillment.class));
  }

  @Test
  public void allowsWhenProductAlreadyInWarehouseEvenAtProductLimit() {
    when(fulfillmentRepository.isProductInWarehouse(product, warehouse)).thenReturn(true);
    when(fulfillmentRepository.countDistinctProductsForWarehouse(warehouse)).thenReturn(10L);

    Fulfillment result = service.create(1L, 2L, "MWH.001");

    verify(fulfillmentRepository).persist(result);
  }

  @Test
  public void getByIdThrowsWhenMissing() {
    when(fulfillmentRepository.findById(99L)).thenReturn(null);
    assertThrows(FulfillmentNotFoundException.class, () -> service.getById(99L));
  }

  @Test
  public void deleteThrowsWhenMissing() {
    when(fulfillmentRepository.findById(99L)).thenReturn(null);
    assertThrows(FulfillmentNotFoundException.class, () -> service.delete(99L));
    verify(fulfillmentRepository, never()).delete(any(Fulfillment.class));
  }
}
