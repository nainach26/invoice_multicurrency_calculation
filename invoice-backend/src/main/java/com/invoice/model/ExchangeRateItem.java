package com.invoice.model;

import java.math.BigDecimal;

public class ExchangeRateItem {
    private String date;
    private String base;
    private String quote;
    private BigDecimal rate;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }

    public String getQuote() { return quote; }
    public void setQuote(String quote) { this.quote = quote; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
}
