package com.fulfilment.application.monolith.fulfillment;

import java.util.List;

import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentValidationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("fulfillment")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FulfillmentResource {

  @Inject FulfillmentService fulfillmentService;

  @GET
  public List<FulfillmentResponse> list() {
    return fulfillmentService.listAll().stream().map(this::toResponse).toList();
  }

  @GET
  @Path("{id}")
  public FulfillmentResponse getById(Long id) {
    return toResponse(fulfillmentService.getById(id));
  }

  @POST
  @Transactional
  public Response create(FulfillmentRequest request) {
    if (request == null) {
      throw new FulfillmentValidationException("Request body is required.");
    }
    Fulfillment fulfillment =
        fulfillmentService.create(
            request.storeId(), request.productId(), request.warehouseBusinessUnitCode());
    return Response.status(201).entity(toResponse(fulfillment)).build();
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    fulfillmentService.delete(id);
    return Response.status(204).build();
  }

  private FulfillmentResponse toResponse(Fulfillment fulfillment) {
    return new FulfillmentResponse(
        fulfillment.id,
        fulfillment.store.id,
        fulfillment.product.id,
        fulfillment.warehouse.businessUnitCode);
  }
}
