package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link Store} Panache entity. */
@QuarkusTest
public class StoreTest {

  @Test
  public void noArgConstructorLeavesFieldsUnset() {
    Store store = new Store();
    assertNull(store.name);
    assertEquals(0, store.quantityProductsInStock);
  }

  @Test
  public void nameConstructorSetsName() {
    Store store = new Store("ACME");
    assertEquals("ACME", store.name);
  }

  @Test
  @Transactional
  public void persistAssignsIdAndIsRetrievable() {
    Store store = new Store("PERSIST-ME");
    store.quantityProductsInStock = 42;
    store.persist();

    assertNotNull(store.id, "persist should assign a generated id");

    Store found = Store.findById(store.id);
    assertNotNull(found);
    assertEquals("PERSIST-ME", found.name);
    assertEquals(42, found.quantityProductsInStock);
  }
}
