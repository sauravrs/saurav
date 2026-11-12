package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class WarehouseExceptionMapper implements ExceptionMapper<Exception> {

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(Exception exception) {
    int status;

    if (exception instanceof WarehouseNotFoundException) {
      status = 404;
    } else if (exception instanceof IllegalArgumentException) {
      status = 400;
    } else {
      status = 500;
    }

    ObjectNode body = objectMapper.createObjectNode();
    body.put("code", status);
    body.put("error", exception.getMessage() == null ? "Unexpected error" : exception.getMessage());

    return Response.status(status).entity(body).build();
  }
}
