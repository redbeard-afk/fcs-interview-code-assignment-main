package com.fulfilment.application.monolith.fulfillment;

/** Request body to register a fulfilment association. */
public record FulfillmentRequest(Long storeId, Long productId, String warehouseBusinessUnitCode) {}
