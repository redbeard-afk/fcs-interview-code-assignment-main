package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

public class ReplaceWarehouseUseCaseTest {

  private WarehouseStore store;
  private LocationResolver locationResolver;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    store = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    useCase = new ReplaceWarehouseUseCase(store, locationResolver);
  }

  private Warehouse warehouse(String buCode, String location, Integer capacity, Integer stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = buCode;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }

  @Test
  public void replacesArchivingOldThenCreatingNew() {
    Warehouse old = warehouse("MWH.001", "AMSTERDAM-001", 50, 5);
    when(store.findByBusinessUnitCode("MWH.001")).thenReturn(old);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(store.getAll()).thenReturn(List.of(old));

    Warehouse replacement = warehouse("MWH.001", "AMSTERDAM-001", 60, 5);
    useCase.replace(replacement);

    InOrder inOrder = inOrder(store);
    inOrder.verify(store).update(old); // archive old first
    inOrder.verify(store).create(replacement); // then create new
    assertNotNull(old.archivedAt);
    assertNotNull(replacement.createdAt);
  }

  @Test
  public void handlesNullStockAndCapacityOnReplacedWarehouseWithoutNpe() {
    Warehouse old = warehouse("MWH.001", "AMSTERDAM-001", 50, 5);
    old.stock = null; // legacy/corrupt persisted data
    old.capacity = null;
    when(store.findByBusinessUnitCode("MWH.001")).thenReturn(old);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(store.getAll()).thenReturn(List.of(old));

    // null old stock/capacity are treated as 0; new stock 0 matches, capacity 10 accommodates 0
    Warehouse replacement = warehouse("MWH.001", "AMSTERDAM-001", 10, 0);
    useCase.replace(replacement);

    verify(store).create(replacement);
  }

  @Test
  public void throwsWhenTargetNotFound() {
    when(store.findByBusinessUnitCode("MWH.404")).thenReturn(null);

    assertThrows(
        WarehouseNotFoundException.class,
        () -> useCase.replace(warehouse("MWH.404", "AMSTERDAM-001", 50, 5)));
    verify(store, never()).update(any());
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsInvalidLocation() {
    when(store.findByBusinessUnitCode("MWH.001"))
        .thenReturn(warehouse("MWH.001", "AMSTERDAM-001", 50, 5));
    when(locationResolver.resolveByIdentifier("NOWHERE")).thenReturn(null);

    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.replace(warehouse("MWH.001", "NOWHERE", 50, 5)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsWhenNewCapacityCannotAccommodateOldStock() {
    when(store.findByBusinessUnitCode("MWH.001"))
        .thenReturn(warehouse("MWH.001", "AMSTERDAM-001", 50, 40));
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    // new capacity 30 < old stock 40
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.replace(warehouse("MWH.001", "AMSTERDAM-001", 30, 40)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsWhenStockDoesNotMatch() {
    when(store.findByBusinessUnitCode("MWH.001"))
        .thenReturn(warehouse("MWH.001", "AMSTERDAM-001", 50, 5));
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    // new stock 6 != old stock 5
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.replace(warehouse("MWH.001", "AMSTERDAM-001", 50, 6)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsWhenCapacityExceedsLocationMaxAfterFreeingOld() {
    Warehouse old = warehouse("MWH.001", "AMSTERDAM-001", 50, 5);
    Warehouse other = warehouse("MWH.012", "AMSTERDAM-001", 40, 5);
    when(store.findByBusinessUnitCode("MWH.001")).thenReturn(old);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(store.getAll()).thenReturn(List.of(old, other));

    // used excluding old = 40; 40 + new 90 = 130 > 100
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.replace(warehouse("MWH.001", "AMSTERDAM-001", 90, 5)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsMissingBusinessUnitCode() {
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.replace(warehouse(null, "AMSTERDAM-001", 50, 5)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsMissingCapacityOrStock() {
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.replace(warehouse("MWH.001", "AMSTERDAM-001", null, 5)));
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.replace(warehouse("MWH.001", "AMSTERDAM-001", 50, null)));
    verify(store, never()).create(any());
  }
}
