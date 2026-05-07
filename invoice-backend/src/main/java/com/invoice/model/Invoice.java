package com.invoice.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class Invoice {

    @NotBlank(message = "Invoice currency is required")
    private String currency;

    @NotBlank(message = "Invoice date is required")
    private String date;

    @NotNull(message = "Invoice must have at least one line item")
    @Size(min = 1, message = "Invoice must have at least one line item")
    @Valid
    private List<InvoiceLine> lines;

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<InvoiceLine> getLines() { return lines; }
    public void setLines(List<InvoiceLine> lines) { this.lines = lines; }
}
