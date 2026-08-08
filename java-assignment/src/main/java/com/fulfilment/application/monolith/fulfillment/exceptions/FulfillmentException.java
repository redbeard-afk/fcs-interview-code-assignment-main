package com.fulfilment.application.monolith.fulfillment.exceptions;

/** Base type for fulfilment errors, carrying the HTTP status they map to. */
public abstract class FulfillmentException extends RuntimeException {

  private final int statusCode;

  protected FulfillmentException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
