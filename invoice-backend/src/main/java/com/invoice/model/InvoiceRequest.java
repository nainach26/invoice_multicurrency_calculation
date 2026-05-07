package com.invoice.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class InvoiceRequest {

    @NotNull(message = "Invalid or missing invoice data")
    @Valid
    private Invoice invoice;

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
}
