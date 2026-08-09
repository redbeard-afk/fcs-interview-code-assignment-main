package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.DuplicateBusinessUnitCodeException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private WarehouseStore store;
  private LocationResolver locationResolver;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    store = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    useCase = new CreateWarehouseUseCase(store, locationResolver);
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
  public void createsWarehouseWhenAllValidationsPass() {
    when(store.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(store.getAll()).thenReturn(List.of());

    Warehouse w = warehouse("MWH.100", "AMSTERDAM-001", 30, 10);
    useCase.create(w);

    verify(store).create(w);
    assertNotNull(w.createdAt);
  }

  @Test
  public void createsWhenExistingWarehouseCapacityIsNull() {
    // An existing warehouse at the location has a null capacity: it should count as 0 capacity used.
    when(store.findByBusinessUnitCode("MWH.100")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(store.getAll()).thenReturn(List.of(warehouse("MWH.012", "AMSTERDAM-001", null, 0)));

    Warehouse w = warehouse("MWH.100", "AMSTERDAM-001", 30, 10);
    useCase.create(w);

    verify(store).create(w);
  }

  @Test
  public void rejectsDuplicateBusinessUnitCode() {
    when(store.findByBusinessUnitCode("MWH.001"))
        .thenReturn(warehouse("MWH.001", "ZWOLLE-001", 10, 1));

    assertThrows(
        DuplicateBusinessUnitCodeException.class,
        () -> useCase.create(warehouse("MWH.001", "AMSTERDAM-001", 30, 10)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsInvalidLocation() {
    when(store.findByBusinessUnitCode(any())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("NOWHERE")).thenReturn(null);

    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.100", "NOWHERE", 10, 1)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsWhenMaxWarehousesReached() {
    when(store.findByBusinessUnitCode(any())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 100));
    when(store.getAll()).thenReturn(List.of(warehouse("MWH.001", "ZWOLLE-001", 10, 1)));

    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.100", "ZWOLLE-001", 10, 1)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsWhenTotalCapacityWouldExceedLocation() {
    when(store.findByBusinessUnitCode(any())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(store.getAll()).thenReturn(List.of(warehouse("MWH.001", "AMSTERDAM-001", 95, 1)));

    // 95 already used + 10 new = 105 > 100
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.100", "AMSTERDAM-001", 10, 1)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsWhenStockExceedsCapacity() {
    when(store.findByBusinessUnitCode(any())).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(store.getAll()).thenReturn(List.of());

    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.100", "AMSTERDAM-001", 10, 20)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsMissingBusinessUnitCode() {
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse(null, "AMSTERDAM-001", 10, 1)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsMissingLocation() {
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.100", null, 10, 1)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsInvalidCapacity() {
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.100", "AMSTERDAM-001", 0, 1)));
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.100", "AMSTERDAM-001", null, 1)));
    verify(store, never()).create(any());
  }

  @Test
  public void rejectsInvalidStock() {
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.100", "AMSTERDAM-001", 10, -1)));
    assertThrows(
        WarehouseValidationException.class,
        () -> useCase.create(warehouse("MWH.100", "AMSTERDAM-001", 10, null)));
    verify(store, never()).create(any());
  }
}
