package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for the {@link StoreSyncEvent} record. */
public class StoreSyncEventTest {

  @Test
  public void exposesStoreAndNewFlagForCreation() {
    Store store = new Store("EVT-STORE");

    StoreSyncEvent event = new StoreSyncEvent(store, true);

    assertSame(store, event.store());
    assertTrue(event.isNew());
  }

  @Test
  public void exposesStoreAndNewFlagForUpdate() {
    Store store = new Store("EVT-STORE");

    StoreSyncEvent event = new StoreSyncEvent(store, false);

    assertSame(store, event.store());
    assertFalse(event.isNew());
  }
}
