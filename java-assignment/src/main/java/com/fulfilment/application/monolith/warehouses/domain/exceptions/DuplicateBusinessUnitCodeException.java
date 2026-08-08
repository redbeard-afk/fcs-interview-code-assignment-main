package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Raised when creating a warehouse whose business unit code already belongs to an active warehouse
 * (maps to HTTP 409 Conflict).
 */
public class DuplicateBusinessUnitCodeException extends WarehouseException {

  public DuplicateBusinessUnitCodeException(String message) {
    super(message, 409);
  }
}
