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

  private Warehouse warehouse(Long id) {
    Warehouse w = new Warehouse();
    w.id = id;
    w.businessUnitCode = "MWH.001";
    w.location = "AMSTERDAM-001";
    w.capacity = 10;
    w.stock = 1;
    return w;
  }

  private Warehouse request(Long id) {
    Warehouse w = new Warehouse();
    w.id = id;
    return w;
  }

  @Test
  public void archivesExistingWarehouse() {
    when(store.findByDbId(1L)).thenReturn(warehouse(1L));

    useCase.archive(request(1L));

    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(store).update(captor.capture());
    assertNotNull(captor.getValue().archivedAt);
  }

  @Test
  public void throwsWhenWarehouseNotFound() {
    when(store.findByDbId(404L)).thenReturn(null);

    assertThrows(WarehouseNotFoundException.class, () -> useCase.archive(request(404L)));
    verify(store, never()).update(any());
  }

  @Test
  public void rejectArchivingArchivedWarehouse() {
    Warehouse archived = warehouse(1L);
    archived.archivedAt = LocalDateTime.now();
    when(store.findByDbId(1L)).thenReturn(archived);

    assertThrows(WarehouseAlreadyArchivedException.class, () -> useCase.archive(request(1L)));
    verify(store, never()).update(any());
  }

  @Test
  public void rejectsMissingId() {
    assertThrows(WarehouseValidationException.class, () -> useCase.archive(request(null)));
    verify(store, never()).update(any());
  }
}
