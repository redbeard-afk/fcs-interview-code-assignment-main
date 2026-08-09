package com.fulfilment.application.monolith.fulfillment.exceptions;

/** Raised when a referenced store, product, warehouse or fulfilment does not exist (HTTP 404). */
public class FulfillmentNotFoundException extends FulfillmentException {

  public FulfillmentNotFoundException(String message) {
    super(message, 404);
  }
}
