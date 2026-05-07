package com.invoice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoice.model.ExchangeRateItem;
import com.invoice.model.Invoice;
import com.invoice.model.InvoiceLine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class InvoiceService {

    private static final Logger log = Logger.getLogger(InvoiceService.class);
    private static final String FRANKFURTER_BASE_URL = "https://api.frankfurter.dev/v2/rates";

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public BigDecimal calculateTotal(Invoice invoice) throws RateNotFoundException, IOException, InterruptedException {
        BigDecimal total = BigDecimal.ZERO;
        String baseCurrency = invoice.getCurrency();
        String date = invoice.getDate();
        Map<String, BigDecimal> rateCache = new HashMap<>();

        for (InvoiceLine line : invoice.getLines()) {
            BigDecimal lineAmount = line.getAmount();

            if (line.getCurrency().equalsIgnoreCase(baseCurrency)) {
                lineAmount = lineAmount.setScale(2, RoundingMode.HALF_UP);
            } else {
                String cacheKey = line.getCurrency() + "_" + baseCurrency;
                BigDecimal rate = rateCache.get(cacheKey);
                if (rate == null) {
                    rate = fetchExchangeRate(date, line.getCurrency(), baseCurrency);
                    rateCache.put(cacheKey, rate);
                }
                lineAmount = lineAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            }

            total = total.add(lineAmount);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal fetchExchangeRate(String date, String from, String to)
            throws RateNotFoundException, IOException, InterruptedException {

        String url = FRANKFURTER_BASE_URL + "?date=" + date + "&base=" + from;
        log.debugf("Fetching exchange rate: %s -> %s on %s", from, to, date);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            throw new RateNotFoundException("Exchange rate data not found for date: " + date);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Frankfurter API returned HTTP " + response.statusCode());
        }

        List<ExchangeRateItem> items = objectMapper.readValue(
                response.body(), new TypeReference<List<ExchangeRateItem>>() {});

        return items.stream()
                .filter(item -> item.getQuote().equalsIgnoreCase(to))
                .findFirst()
                .map(item -> item.getRate().setScale(4, RoundingMode.HALF_UP))
                .orElseThrow(() -> new RateNotFoundException(
                        "Exchange rate not found for currency pair: " + from + " -> " + to));
    }
}
