package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LegacyStoreManagerGateway}. The gateway emulates a legacy integration by
 * writing (and cleaning up) a temp file, so we assert it completes without error.
 */
public class LegacyStoreManagerGatewayTest {

  private final LegacyStoreManagerGateway gateway = new LegacyStoreManagerGateway();

  @Test
  public void createStoreOnLegacySystemDoesNotThrow() {
    Store store = new Store("LEGACY-CREATE");
    store.quantityProductsInStock = 3;

    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }

  @Test
  public void updateStoreOnLegacySystemDoesNotThrow() {
    Store store = new Store("LEGACY-UPDATE");
    store.quantityProductsInStock = 8;

    assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
  }

  @Test
  public void writeErrorIsSwallowedNotPropagated() {
    // A name with a path separator makes it an invalid temp-file prefix, so createTempFile throws.
    // The gateway must catch it internally (it only logs) and never surface the error to the caller.
    Store store = new Store("invalid/name");
    store.quantityProductsInStock = 1;

    assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
  }
}
