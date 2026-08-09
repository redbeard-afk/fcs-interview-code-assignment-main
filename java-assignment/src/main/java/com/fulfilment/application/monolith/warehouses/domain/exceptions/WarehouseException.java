package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Base type for warehouse domain errors, carrying the HTTP status they map to. */
public abstract class WarehouseException extends RuntimeException {

  private final int statusCode;

  protected WarehouseException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
