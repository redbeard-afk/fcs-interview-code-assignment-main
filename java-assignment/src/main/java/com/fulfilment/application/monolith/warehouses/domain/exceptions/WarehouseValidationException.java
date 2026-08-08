package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Raised when a warehouse operation violates a business validation rule (maps to HTTP 400). */
public class WarehouseValidationException extends RuntimeException {

  public WarehouseValidationException(String message) {
    super(message);
  }
}
