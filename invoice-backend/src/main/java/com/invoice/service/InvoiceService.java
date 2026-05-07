package com.invoice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoice.model.ExchangeRateItem;
import com.invoice.model.Invoice;
import com.invoice.model.InvoiceLine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InvoiceService {

    private static final Logger log = Logger.getLogger(InvoiceService.class);
    private static final String FRANKFURTER_BASE_URL = "https://api.frankfurter.dev/v2/rates";
    private static final long CACHE_TTL_MS = Duration.ofHours(1).toMillis();
    private static final int MAX_RETRIES = 2;

    @ConfigProperty(name = "invoice.max-lines", defaultValue = "25")
    int maxLines;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private record CacheEntry(BigDecimal rate, long expiresAt) {
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    private final ConcurrentHashMap<String, CacheEntry> rateCache = new ConcurrentHashMap<>();

    public BigDecimal calculateTotal(Invoice invoice) throws RateNotFoundException, IOException, InterruptedException {
        String baseCurrency = invoice.getCurrency();
        String date = invoice.getDate();

        validateInvoiceDate(date);

        if (invoice.getLines().size() > maxLines) {
            throw new IllegalArgumentException(
                    "Invoice cannot have more than " + maxLines + " line items");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLine line : invoice.getLines()) {
            BigDecimal lineAmount = line.getAmount();

            if (!line.getCurrency().equalsIgnoreCase(baseCurrency)) {
                BigDecimal rate = getCachedRate(date, line.getCurrency(), baseCurrency);
                lineAmount = lineAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            } else {
                lineAmount = lineAmount.setScale(2, RoundingMode.HALF_UP);
            }

            total = total.add(lineAmount);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateInvoiceDate(String date) {
        LocalDate invoiceDate;
        try {
            invoiceDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid invoice date format. Expected YYYY-MM-DD");
        }
        if (invoiceDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Invoice date cannot be in the future");
        }
    }

    private BigDecimal getCachedRate(String date, String from, String to)
            throws RateNotFoundException, IOException, InterruptedException {
        String cacheKey = date + "_" + from + "_" + to;
        CacheEntry entry = rateCache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            log.debugf("Cache hit for %s -> %s on %s", from, to, date);
            return entry.rate();
        }
        BigDecimal rate = fetchWithRetry(date, from, to);
        rateCache.put(cacheKey, new CacheEntry(rate, System.currentTimeMillis() + CACHE_TTL_MS));
        return rate;
    }

    private BigDecimal fetchWithRetry(String date, String from, String to)
            throws RateNotFoundException, IOException, InterruptedException {
        IOException lastIoException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return fetchExchangeRate(date, from, to);
            } catch (ExchangeRateApiException | RateNotFoundException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (IOException e) {
                log.warnf("Attempt %d/%d failed fetching rate %s -> %s: %s", attempt, MAX_RETRIES, from, to, e.getMessage());
                lastIoException = e;
            }
        }
        throw lastIoException;
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
            throw new ExchangeRateApiException(
                    "Frankfurter API returned HTTP " + response.statusCode(), response.statusCode());
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
