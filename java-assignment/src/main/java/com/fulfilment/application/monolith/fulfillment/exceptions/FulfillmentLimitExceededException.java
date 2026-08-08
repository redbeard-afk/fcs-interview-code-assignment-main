package com.fulfilment.application.monolith.fulfillment.exceptions;

/** Raised when registering a fulfilment would breach one of the association limits (HTTP 409). */
public class FulfillmentLimitExceededException extends FulfillmentException {

  public FulfillmentLimitExceededException(String message) {
    super(message, 409);
  }
}
