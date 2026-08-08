package com.fulfilment.application.monolith.stores;

/**
 * Event fired when a {@link Store} has been created or updated within a transaction.
 *
 * <p>It is consumed only after the surrounding transaction commits successfully, so the legacy
 * system is synced exclusively with data confirmed in our own database.
 *
 * @param store the confirmed store state to propagate to the legacy system
 * @param isNew {@code true} when the store was just created, {@code false} when it was updated
 */
public record StoreSyncEvent(Store store, boolean isNew) {}
