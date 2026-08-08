package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
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
    Warehouse existing = warehouse("MWH.001");
    when(store.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    useCase.archive(warehouse("MWH.001"));

    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(store).update(captor.capture());
    assertNotNull(captor.getValue().archivedAt);
  }

  @Test
  public void throwsWhenWarehouseNotFound() {
    when(store.findByBusinessUnitCode("MWH.404")).thenReturn(null);

    assertThrows(
        WarehouseNotFoundException.class, () -> useCase.archive(warehouse("MWH.404")));
    verify(store, never()).update(any());
  }
}
