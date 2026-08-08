package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LegacyStoreSyncListener}: it must route the event to the correct gateway method,
 * and — as a transactional {@code AFTER_SUCCESS} observer — be invoked only after a successful
 * commit, never on rollback.
 */
@QuarkusTest
public class LegacyStoreSyncListenerTest {

  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Inject LegacyStoreSyncListener listener;

  @Inject TxFixture txFixture;

  @Inject Event<StoreSyncEvent> storeSyncEvent;

  @Test
  public void routesNewEventToCreate() {
    listener.onStoreSynced(new StoreSyncEvent(new Store("DIRECT-CREATE"), true));

    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(any(Store.class));
    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void routesExistingEventToUpdate() {
    listener.onStoreSynced(new StoreSyncEvent(new Store("DIRECT-UPDATE"), false));

    verify(legacyStoreManagerGateway).updateStoreOnLegacySystem(any(Store.class));
    verify(legacyStoreManagerGateway, never()).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void deliversAfterSuccessfulCommit() {
    txFixture.fireAndCommit();

    verify(legacyStoreManagerGateway, timeout(1000)).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void deliversImmediatelyWithoutActiveTransaction() {
    // With no transaction in progress, an AFTER_SUCCESS observer is delivered synchronously on fire.
    storeSyncEvent.fire(new StoreSyncEvent(new Store("NO-TX-STORE"), true));

    verify(legacyStoreManagerGateway).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void doesNotDeliverOnRollback() {
    assertThrows(RuntimeException.class, () -> txFixture.fireThenRollback());

    verify(legacyStoreManagerGateway, never()).createStoreOnLegacySystem(any(Store.class));
    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any(Store.class));
  }

  /** Fires the event inside a real transaction that either commits or rolls back. */
  @ApplicationScoped
  public static class TxFixture {

    @Inject Event<StoreSyncEvent> storeSyncEvent;

    @Transactional
    public void fireAndCommit() {
      storeSyncEvent.fire(new StoreSyncEvent(new Store("COMMIT-STORE"), true));
    }

    @Transactional
    public void fireThenRollback() {
      storeSyncEvent.fire(new StoreSyncEvent(new Store("ROLLBACK-STORE"), true));
      throw new RuntimeException("forced rollback");
    }
  }
}
