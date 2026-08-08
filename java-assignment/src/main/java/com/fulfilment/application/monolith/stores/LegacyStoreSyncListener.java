package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Bridges committed {@link Store} changes to the legacy system.
 *
 * <p>The observer method reacts to {@link StoreSyncEvent} only during {@link
 * TransactionPhase#AFTER_SUCCESS}. This guarantees the {@link LegacyStoreManagerGateway} is invoked
 * exclusively after the transaction that produced the change has been committed to our database. If
 * the transaction rolls back, an {@code AFTER_SUCCESS} observer is never delivered, so the legacy
 * system is never told about data we did not persist.
 */
@ApplicationScoped
public class LegacyStoreSyncListener {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreSynced(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreSyncEvent event) {
    if (event.isNew()) {
      legacyStoreManagerGateway.createStoreOnLegacySystem(event.store());
    } else {
      legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store());
    }
  }
}
