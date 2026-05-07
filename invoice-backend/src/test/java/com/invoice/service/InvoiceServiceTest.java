package com.invoice.service;

import com.invoice.model.Invoice;
import com.invoice.model.InvoiceLine;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class InvoiceServiceTest {

    @Inject
    InvoiceService invoiceService;

    // ── Date Validation ───────────────────────────────────────────────────────

    @Test
    void futureDate_throwsIllegalArgumentException() {
        Invoice invoice = invoice("NZD", "2099-12-31", line("NZD", "100.00"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> invoiceService.calculateTotal(invoice));

        assertTrue(ex.getMessage().contains("future"));
    }

    @Test
    void invalidDateFormat_throwsIllegalArgumentException() {
        Invoice invoice = invoice("NZD", "15-01-2024", line("NZD", "100.00"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> invoiceService.calculateTotal(invoice));

        assertTrue(ex.getMessage().contains("YYYY-MM-DD"));
    }

    @Test
    void nonExistentDate_throwsIllegalArgumentException() {
        Invoice invoice = invoice("NZD", "2024-02-30", line("NZD", "100.00"));

        assertThrows(IllegalArgumentException.class,
                () -> invoiceService.calculateTotal(invoice));
    }

    @Test
    void todaysDate_isAccepted() throws Exception {
        String today = LocalDate.now().toString();
        Invoice invoice = invoice("NZD", today, line("NZD", "50.00"));

        BigDecimal result = invoiceService.calculateTotal(invoice);

        assertEquals(new BigDecimal("50.00"), result);
    }

    // ── Same-Currency Calculation (no HTTP calls) ────────────────────────────

    @Test
    void sameCurrency_singleLine_returnsLineAmount() throws Exception {
        Invoice invoice = invoice("NZD", "2024-01-15", line("NZD", "100.00"));

        BigDecimal result = invoiceService.calculateTotal(invoice);

        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void sameCurrency_multipleLines_returnsSum() throws Exception {
        Invoice invoice = invoice("NZD", "2024-01-15",
                line("NZD", "100.00"),
                line("NZD", "250.50"),
                line("NZD", "49.50"));

        BigDecimal result = invoiceService.calculateTotal(invoice);

        assertEquals(new BigDecimal("400.00"), result);
    }

    @Test
    void sameCurrency_roundingApplied() throws Exception {
        Invoice invoice = invoice("USD", "2024-06-01", line("USD", "100.005"));

        BigDecimal result = invoiceService.calculateTotal(invoice);

        assertEquals(2, result.scale());
    }

    @Test
    void sameCurrency_resultHasTwoDecimalPlaces() throws Exception {
        Invoice invoice = invoice("EUR", "2024-03-10", line("EUR", "99.9"));

        BigDecimal result = invoiceService.calculateTotal(invoice);

        assertEquals(2, result.scale());
        assertEquals(new BigDecimal("99.90"), result);
    }

    @Test
    void sameCurrency_largeAmount_calculatesCorrectly() throws Exception {
        Invoice invoice = invoice("USD", "2024-01-15",
                line("USD", "9999999.99"),
                line("USD", "0.01"));

        BigDecimal result = invoiceService.calculateTotal(invoice);

        assertEquals(new BigDecimal("10000000.00"), result);
    }

    // ── Max Amount Validation ─────────────────────────────────────────────────

    @Test
    void amountExceedsMaxAmount_throwsIllegalArgumentException() {
        Invoice invoice = invoice("NZD", "2024-01-15",
                line("NZD", "1000000001"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> invoiceService.calculateTotal(invoice));

        assertTrue(ex.getMessage().contains("cannot exceed"));
    }

    @Test
    void amountExactlyAtMaxAmount_isAccepted() throws Exception {
        Invoice invoice = invoice("NZD", "2024-01-15",
                line("NZD", "1000000000"));

        BigDecimal result = invoiceService.calculateTotal(invoice);

        assertEquals(new BigDecimal("1000000000.00"), result);
    }

    @Test
    void amountJustBelowMaxAmount_isAccepted() throws Exception {
        Invoice invoice = invoice("NZD", "2024-01-15",
                line("NZD", "999999999.99"));

        BigDecimal result = invoiceService.calculateTotal(invoice);

        assertEquals(new BigDecimal("999999999.99"), result);
    }

    @Test
    void multipleLines_oneExceedsMaxAmount_throwsIllegalArgumentException() {
        Invoice invoice = invoice("NZD", "2024-01-15",
                line("NZD", "100.00"),
                line("NZD", "1000000001.00"));

        assertThrows(IllegalArgumentException.class,
                () -> invoiceService.calculateTotal(invoice));
    }

    // ── Max Lines Validation ─────────────────────────────────────────────────

    @Test
    void exceedsMaxLines_throwsIllegalArgumentException() {
        List<InvoiceLine> lines = IntStream.range(0, 26)
                .mapToObj(i -> line("NZD", "10.00"))
                .collect(java.util.stream.Collectors.toList());
        Invoice invoice = invoiceWithLines("NZD", "2024-01-15", lines);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> invoiceService.calculateTotal(invoice));

        assertTrue(ex.getMessage().contains("more than"));
        assertTrue(ex.getMessage().contains("25"));
    }

    @Test
    void exactlyMaxLines_isAccepted() throws Exception {
        List<InvoiceLine> lines = IntStream.range(0, 25)
                .mapToObj(i -> line("NZD", "10.00"))
                .collect(java.util.stream.Collectors.toList());
        Invoice invoice = invoiceWithLines("NZD", "2024-01-15", lines);

        BigDecimal result = invoiceService.calculateTotal(invoice);

        assertEquals(new BigDecimal("250.00"), result);
    }

    @Test
    void oneLessThanMaxLines_isAccepted() throws Exception {
        List<InvoiceLine> lines = IntStream.range(0, 24)
                .mapToObj(i -> line("NZD", "10.00"))
                .collect(java.util.stream.Collectors.toList());
        Invoice invoice = invoiceWithLines("NZD", "2024-01-15", lines);

        BigDecimal result = invoiceService.calculateTotal(invoice);

        assertEquals(new BigDecimal("240.00"), result);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Invoice invoice(String currency, String date, InvoiceLine... lines) {
        return invoiceWithLines(currency, date, Arrays.asList(lines));
    }

    private Invoice invoiceWithLines(String currency, String date, List<InvoiceLine> lines) {
        Invoice inv = new Invoice();
        inv.setCurrency(currency);
        inv.setDate(date);
        inv.setLines(lines);
        return inv;
    }

    private InvoiceLine line(String currency, String amount) {
        InvoiceLine line = new InvoiceLine();
        line.setCurrency(currency);
        line.setAmount(new BigDecimal(amount));
        return line;
    }
}
