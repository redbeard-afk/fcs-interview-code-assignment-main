package com.fulfilment.application.monolith.fulfillment.exceptions;

/** Raised when the same (store, product, warehouse) association already exists (HTTP 409). */
public class DuplicateFulfillmentException extends FulfillmentException {

  public DuplicateFulfillmentException(String message) {
    super(message, 409);
  }
}
