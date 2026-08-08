package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Raised when archiving a warehouse whose business unit code exists but is already archived (maps to
 * HTTP 409 Conflict).
 */
public class WarehouseAlreadyArchivedException extends WarehouseException {

  public WarehouseAlreadyArchivedException(String message) {
    super(message, 409);
  }
}
