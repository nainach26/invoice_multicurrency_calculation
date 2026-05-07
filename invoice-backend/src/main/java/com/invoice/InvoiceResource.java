package com.invoice;

import com.invoice.model.Invoice;
import com.invoice.model.InvoiceRequest;
import com.invoice.service.ExchangeRateApiException;
import com.invoice.service.InvoiceService;
import com.invoice.service.RateNotFoundException;
import io.smallrye.common.annotation.Blocking;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

import java.math.BigDecimal;

@Path("/invoice")
public class InvoiceResource {

    private static final Logger log = Logger.getLogger(InvoiceResource.class);

    @Inject
    InvoiceService invoiceService;

    @POST
    @Path("/total")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Blocking
    public Response calculateTotal(@Valid InvoiceRequest request) {
        Invoice invoice = request.getInvoice();
        try {
            BigDecimal total = invoiceService.calculateTotal(invoice);
            return Response.ok(total.toPlainString()).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error: " + e.getMessage()).build();
        } catch (RateNotFoundException e) {
            log.warnf("Rate not found: %s", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Error: " + e.getMessage()).build();
        } catch (ExchangeRateApiException e) {
            log.errorf("Exchange rate API error (HTTP %d): %s", e.getStatusCode(), e.getMessage());
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity("Error: Failed to retrieve exchange rate data. Please try again later.").build();
        } catch (Exception e) {
            log.errorf(e, "Unexpected error calculating invoice total for currency %s on %s",
                    invoice.getCurrency(), invoice.getDate());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: An unexpected error occurred. Please try again later.").build();
        }
    }
}
