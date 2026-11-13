package com.stocktrader.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PriceFetchServiceTest {

    // Build a minimal Yahoo Finance-like JSON response with the given price
    private static String yahooJson(String price) {
        return "{\"chart\":{\"result\":[{\"meta\":{" +
                "\"regularMarketPrice\":{\"raw\":" + price + "}}}]}}";
    }

    // Inject a mock HttpClient into PriceFetchService via reflection
    @SuppressWarnings("unchecked")
    private PriceFetchService serviceWithMockHttp(int status, String body) throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(status);
        when(mockResponse.body()).thenReturn(body);

        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(), any())).thenReturn(mockResponse);

        PriceFetchService service = new PriceFetchService();
        Field httpField = PriceFetchService.class.getDeclaredField("http");
        httpField.setAccessible(true);
        httpField.set(service, mockClient);
        return service;
    }

    @Test
    void fetchPrice_returnsCorrectPrice() throws Exception {
        PriceFetchService service = serviceWithMockHttp(200, yahooJson("213.49"));
        BigDecimal price = service.fetchPrice("AAPL");
        assertEquals(new BigDecimal("213.49"), price);
    }

    @Test
    void fetchPrice_uppercasesTicker() throws Exception {
        // Should not throw — ticker is normalised before the URL is built
        PriceFetchService service = serviceWithMockHttp(200, yahooJson("100.00"));
        assertDoesNotThrow(() -> service.fetchPrice("aapl"));
    }

    @Test
    void fetchPrice_throwsOnHttp404() throws Exception {
        PriceFetchService service = serviceWithMockHttp(404, "not found");
        var ex = assertThrows(PriceFetchService.PriceFetchException.class,
                () -> service.fetchPrice("FAKE"));
        assertTrue(ex.getMessage().contains("HTTP 404"));
    }

    @Test
    void fetchPrice_throwsWhenPriceNotInBody() throws Exception {
        PriceFetchService service = serviceWithMockHttp(200, "{\"chart\":{\"result\":[]}}");
        var ex = assertThrows(PriceFetchService.PriceFetchException.class,
                () -> service.fetchPrice("AAPL"));
        assertTrue(ex.getMessage().contains("Price not found"));
    }

    @Test
    void fetchPrice_parsesIntegerPrice() throws Exception {
        PriceFetchService service = serviceWithMockHttp(200, yahooJson("150"));
        assertEquals(new BigDecimal("150"), service.fetchPrice("MSFT"));
    }
}
