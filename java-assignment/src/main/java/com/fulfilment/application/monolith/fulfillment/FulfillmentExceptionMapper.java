package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/** Translates fulfilment domain exceptions into their HTTP responses. */
@Provider
public class FulfillmentExceptionMapper implements ExceptionMapper<FulfillmentException> {

  @Override
  public Response toResponse(FulfillmentException exception) {
    return Response.status(exception.getStatusCode())
        .entity(Map.of("error", exception.getMessage()))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
