package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseRepositoryTest {

  @Inject WarehouseRepository repository;

  private Warehouse newWarehouse(String buCode, String location, int capacity, int stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = buCode;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    w.createdAt = LocalDateTime.now();
    w.archivedAt = null;
    return w;
  }

  @Test
  @Transactional
  public void findByBusinessUnitCodeReturnsSeededActiveWarehouse() {
    Warehouse found = repository.findByBusinessUnitCode("MWH.001");

    assertNotNull(found);
    assertEquals("ZWOLLE-001", found.location);
    assertEquals(100, found.capacity);
  }

  @Test
  @Transactional
  public void findByBusinessUnitCodeReturnsNullForUnknown() {
    assertNull(repository.findByBusinessUnitCode("DOES.NOT.EXIST"));
  }

  @Test
  @Transactional
  public void createThenFindReturnsWarehouse() {
    repository.create(newWarehouse("TEST.CREATE", "AMSTERDAM-001", 10, 5));

    Warehouse found = repository.findByBusinessUnitCode("TEST.CREATE");
    assertNotNull(found);
    assertEquals("AMSTERDAM-001", found.location);
    assertEquals(10, found.capacity);
    assertEquals(5, found.stock);
  }

  @Test
  @Transactional
  public void updateMutatesActiveWarehouse() {
    repository.create(newWarehouse("TEST.UPDATE", "AMSTERDAM-001", 20, 5));

    Warehouse toUpdate = repository.findByBusinessUnitCode("TEST.UPDATE");
    toUpdate.stock = 9;
    repository.update(toUpdate);

    assertEquals(9, repository.findByBusinessUnitCode("TEST.UPDATE").stock);
  }

  @Test
  @Transactional
  public void updateSettingArchivedAtHidesFromActiveLookup() {
    repository.create(newWarehouse("TEST.ARCHIVE", "AMSTERDAM-001", 20, 5));

    Warehouse toArchive = repository.findByBusinessUnitCode("TEST.ARCHIVE");
    toArchive.archivedAt = LocalDateTime.now();
    repository.update(toArchive);

    assertNull(repository.findByBusinessUnitCode("TEST.ARCHIVE"));
  }

  @Test
  @Transactional
  public void removeDeletesActiveWarehouse() {
    repository.create(newWarehouse("TEST.REMOVE", "AMSTERDAM-001", 20, 5));
    assertNotNull(repository.findByBusinessUnitCode("TEST.REMOVE"));

    repository.remove(newWarehouse("TEST.REMOVE", "AMSTERDAM-001", 20, 5));

    assertNull(repository.findByBusinessUnitCode("TEST.REMOVE"));
  }
}
