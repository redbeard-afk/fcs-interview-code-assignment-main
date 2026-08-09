package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/** Translates warehouse domain exceptions into their HTTP responses. */
@Provider
public class WarehouseExceptionMapper implements ExceptionMapper<WarehouseException> {

  @Override
  public Response toResponse(WarehouseException exception) {
    return Response.status(exception.getStatusCode())
        .entity(Map.of("error", exception.getMessage()))
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
