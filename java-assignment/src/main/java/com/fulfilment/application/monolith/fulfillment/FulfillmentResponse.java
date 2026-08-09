package com.fulfilment.application.monolith.fulfillment;

/** Response view of a fulfilment association. */
public record FulfillmentResponse(
    Long id, Long storeId, Long productId, String warehouseBusinessUnitCode) {}
