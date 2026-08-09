package com.fulfilment.application.monolith.fulfillment.exceptions;

/** Raised when a fulfilment request is missing required fields (maps to HTTP 400). */
public class FulfillmentValidationException extends FulfillmentException {

  public FulfillmentValidationException(String message) {
    super(message, 400);
  }
}
