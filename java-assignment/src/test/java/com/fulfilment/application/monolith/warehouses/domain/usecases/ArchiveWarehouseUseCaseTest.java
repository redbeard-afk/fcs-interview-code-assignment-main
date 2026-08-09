package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseAlreadyArchivedException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ArchiveWarehouseUseCaseTest {

  private WarehouseStore store;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    store = mock(WarehouseStore.class);
    useCase = new ArchiveWarehouseUseCase(store);
  }

  private Warehouse warehouse(String buCode) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = buCode;
    w.location = "AMSTERDAM-001";
    w.capacity = 10;
    w.stock = 1;
    return w;
  }

  @Test
  public void archivesExistingWarehouse() {
    when(store.findByBusinessUnitCode("MWH.001")).thenReturn(warehouse("MWH.001"));

    useCase.archive(warehouse("MWH.001"));

    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(store).update(captor.capture());
    assertNotNull(captor.getValue().archivedAt);
  }

  @Test
  public void throwsWhenWarehouseNotFound() {
    when(store.findByBusinessUnitCode("MWH.404")).thenReturn(null);
    when(store.getAll()).thenReturn(List.of());

    assertThrows(WarehouseNotFoundException.class, () -> useCase.archive(warehouse("MWH.404")));
    verify(store, never()).update(any());
  }

  @Test
  public void rejectArchivingArchivedWarehouse() {
    // No active record, but an archived row with the same code exists.
    Warehouse archived = warehouse("MWH.001");
    archived.archivedAt = LocalDateTime.now();
    when(store.findByBusinessUnitCode("MWH.001")).thenReturn(null);
    when(store.getAll()).thenReturn(List.of(archived));

    assertThrows(
        WarehouseAlreadyArchivedException.class, () -> useCase.archive(warehouse("MWH.001")));
    verify(store, never()).update(any());
  }

  @Test
  public void rejectsMissingBusinessUnitCode() {
    assertThrows(WarehouseValidationException.class, () -> useCase.archive(warehouse(null)));
    verify(store, never()).update(any());
  }
}
