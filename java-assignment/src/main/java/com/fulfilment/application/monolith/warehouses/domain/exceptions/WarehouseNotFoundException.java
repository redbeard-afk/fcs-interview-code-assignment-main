package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Raised when a targeted warehouse does not exist (maps to HTTP 404). */
public class WarehouseNotFoundException extends RuntimeException {

  public WarehouseNotFoundException(String message) {
    super(message);
  }
}
