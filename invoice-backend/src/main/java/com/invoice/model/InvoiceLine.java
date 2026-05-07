package com.invoice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class InvoiceLine {

    private String description;

    @NotBlank(message = "Each invoice line must have a currency")
    private String currency;

    @NotNull(message = "Invoice line amounts must be positive")
    @Positive(message = "Invoice line amounts must be positive")
    private BigDecimal amount;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
