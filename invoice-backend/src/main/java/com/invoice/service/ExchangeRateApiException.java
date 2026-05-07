package com.invoice.service;

public class ExchangeRateApiException extends RuntimeException {
    private final int statusCode;

    public ExchangeRateApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() { return statusCode; }
}
