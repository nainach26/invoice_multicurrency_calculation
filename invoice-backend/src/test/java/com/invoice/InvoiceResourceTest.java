package com.invoice;

import com.invoice.service.ExchangeRateApiException;
import com.invoice.service.InvoiceService;
import com.invoice.service.RateNotFoundException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class InvoiceResourceTest {

    @InjectMock
    InvoiceService invoiceService;

    private static final String API_KEY = "test-api-key";

    private static final String VALID_PAYLOAD = """
            {
              "invoice": {
                "currency": "NZD",
                "date": "2024-01-15",
                "lines": [
                  { "description": "Consulting", "currency": "NZD", "amount": 100.00 }
                ]
              }
            }
            """;

    // ── Authentication ────────────────────────────────────────────────────────

    @Test
    void missingApiKey_returns401() {
        given()
            .contentType(ContentType.JSON)
            .body(VALID_PAYLOAD)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(401);
    }

    @Test
    void invalidApiKey_returns401() {
        given()
            .header("X-API-Key", "wrong-key")
            .contentType(ContentType.JSON)
            .body(VALID_PAYLOAD)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(401);
    }

    // ── Input Validation (400) ────────────────────────────────────────────────

    @Test
    void missingInvoiceBody_returns400() {
        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400);
    }

    @Test
    void missingDate_returns400() {
        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "invoice": {
                    "currency": "NZD",
                    "lines": [{ "currency": "NZD", "amount": 100.00 }]
                  }
                }
                """)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400)
            .body(containsString("Invoice date is required"));
    }

    @Test
    void invalidDateFormat_returns400WithMessage() throws Exception {
        Mockito.when(invoiceService.calculateTotal(Mockito.any()))
                .thenThrow(new IllegalArgumentException("Invalid invoice date format. Expected YYYY-MM-DD"));

        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "invoice": {
                    "currency": "NZD",
                    "date": "15-01-2024",
                    "lines": [{ "currency": "NZD", "amount": 100.00 }]
                  }
                }
                """)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400)
            .body(containsString("YYYY-MM-DD"));
    }

    @Test
    void missingCurrency_returns400() {
        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "invoice": {
                    "date": "2024-01-15",
                    "lines": [{ "currency": "NZD", "amount": 100.00 }]
                  }
                }
                """)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400)
            .body(containsString("currency"));
    }

    @Test
    void emptyLines_returns400() {
        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "invoice": {
                    "currency": "NZD",
                    "date": "2024-01-15",
                    "lines": []
                  }
                }
                """)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400)
            .body(containsString("line item"));
    }

    @Test
    void nullLines_returns400() {
        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "invoice": {
                    "currency": "NZD",
                    "date": "2024-01-15"
                  }
                }
                """)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400)
            .body(containsString("line item"));
    }

    @Test
    void negativeAmount_returns400WithMessage() {
        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "invoice": {
                    "currency": "NZD",
                    "date": "2024-01-15",
                    "lines": [{ "currency": "NZD", "amount": -50.00 }]
                  }
                }
                """)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400)
            .body(containsString("Invoice line amounts must be positive"));
    }

    @Test
    void zeroAmount_returns400WithMessage() {
        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "invoice": {
                    "currency": "NZD",
                    "date": "2024-01-15",
                    "lines": [{ "currency": "NZD", "amount": 0 }]
                  }
                }
                """)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400)
            .body(containsString("Invoice line amounts must be positive"));
    }

    @Test
    void nullAmount_returns400WithMessage() {
        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "invoice": {
                    "currency": "NZD",
                    "date": "2024-01-15",
                    "lines": [{ "currency": "NZD" }]
                  }
                }
                """)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400)
            .body(containsString("Invoice line amounts must be positive"));
    }

    @Test
    void missingLineCurrency_returns400() {
        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "invoice": {
                    "currency": "NZD",
                    "date": "2024-01-15",
                    "lines": [{ "amount": 100.00 }]
                  }
                }
                """)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400);
    }

    // ── Service Error Mapping ────────────────────────────────────────────────

    @Test
    void tooManyLines_returns400WithMessage() throws Exception {
        Mockito.when(invoiceService.calculateTotal(Mockito.any()))
                .thenThrow(new IllegalArgumentException("Invoice cannot have more than 25 line items"));

        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body(VALID_PAYLOAD)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400)
            .body(containsString("more than 25"));
    }

    @Test
    void futureDate_returns400WithMessage() throws Exception {
        Mockito.when(invoiceService.calculateTotal(Mockito.any()))
                .thenThrow(new IllegalArgumentException("Invoice date cannot be in the future"));

        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body(VALID_PAYLOAD)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(400)
            .body(containsString("future"));
    }

    @Test
    void rateNotFound_returns404() throws Exception {
        Mockito.when(invoiceService.calculateTotal(Mockito.any()))
                .thenThrow(new RateNotFoundException("No rate for date"));

        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body(VALID_PAYLOAD)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(404)
            .body(startsWith("Error:"));
    }

    @Test
    void exchangeRateApiFailure_returns502() throws Exception {
        Mockito.when(invoiceService.calculateTotal(Mockito.any()))
                .thenThrow(new ExchangeRateApiException("Upstream error", 503));

        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body(VALID_PAYLOAD)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(502)
            .body(containsString("exchange rate"));
    }

    @Test
    void networkFailure_returns500() throws Exception {
        Mockito.when(invoiceService.calculateTotal(Mockito.any()))
                .thenThrow(new IOException("Connection refused"));

        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body(VALID_PAYLOAD)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(500)
            .body(containsString("unexpected error"));
    }

    @Test
    void unexpectedError_returns500WithGenericMessage() throws Exception {
        Mockito.when(invoiceService.calculateTotal(Mockito.any()))
                .thenThrow(new RuntimeException("Something broke"));

        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body(VALID_PAYLOAD)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(500)
            .body(not(containsString("Something broke")));
    }

    // ── Happy Path ───────────────────────────────────────────────────────────

    @Test
    void validSingleLine_returns200WithTotal() throws Exception {
        Mockito.when(invoiceService.calculateTotal(Mockito.any()))
                .thenReturn(new BigDecimal("100.00"));

        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body(VALID_PAYLOAD)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(200)
            .body(equalTo("100.00"));
    }

    @Test
    void validMultipleLines_returns200WithSum() throws Exception {
        Mockito.when(invoiceService.calculateTotal(Mockito.any()))
                .thenReturn(new BigDecimal("375.50"));

        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body("""
                {
                  "invoice": {
                    "currency": "NZD",
                    "date": "2024-01-15",
                    "lines": [
                      { "description": "Item A", "currency": "NZD", "amount": 100.00 },
                      { "description": "Item B", "currency": "USD", "amount": 275.50 }
                    ]
                  }
                }
                """)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(200)
            .body(equalTo("375.50"));
    }

    @Test
    void responseDoesNotLeakStackTrace_on500() throws Exception {
        Mockito.when(invoiceService.calculateTotal(Mockito.any()))
                .thenThrow(new RuntimeException("internal detail"));

        given()
            .header("X-API-Key", API_KEY)
            .contentType(ContentType.JSON)
            .body(VALID_PAYLOAD)
        .when()
            .post("/invoice/total")
        .then()
            .statusCode(500)
            .body(not(containsString("internal detail")))
            .body(not(containsString("at com.invoice")));
    }
}
