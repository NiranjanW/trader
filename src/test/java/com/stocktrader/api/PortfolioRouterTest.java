package com.stocktrader.api;

import com.stocktrader.model.PortfolioEntry;
import com.stocktrader.repository.PortfolioRepository;
import com.stocktrader.service.PriceFetchService;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PortfolioRouterTest {

    private final PortfolioRepository repo = mock(PortfolioRepository.class);
    private final PriceFetchService priceFetchService = mock(PriceFetchService.class);

    private Javalin buildApp() {
        Javalin app = Javalin.create();
        new PortfolioRouter(repo, priceFetchService).register(app);
        return app;
    }

    private static PortfolioEntry entry(String ticker, String price) {
        return new PortfolioEntry(1L, ticker, new BigDecimal(price), LocalDate.now());
    }

    // --- POST /entries ---

    @Test
    void postEntries_returns201WithSavedEntry() {
        when(repo.save(eq("AAPL"), any(), any())).thenReturn(entry("AAPL", "213.49"));

        JavalinTest.test(buildApp(), (server, client) -> {
            var resp = client.post("/entries",
                    "{\"ticker\":\"AAPL\",\"price\":213.49,\"entryDate\":\"2026-10-17\"}");
            assertEquals(201, resp.code());
            assertTrue(resp.body().string().contains("AAPL"));
        });
    }

    // --- GET /entries ---

    @Test
    void getEntries_defaultsToWeekPeriod() {
        when(repo.findByPeriod("week")).thenReturn(List.of(entry("AAPL", "200.00")));

        JavalinTest.test(buildApp(), (server, client) -> {
            var resp = client.get("/entries");
            assertEquals(200, resp.code());
            verify(repo).findByPeriod("week");
        });
    }

    @Test
    void getEntries_respectsPeriodQueryParam() {
        when(repo.findByPeriod("month")).thenReturn(List.of());

        JavalinTest.test(buildApp(), (server, client) -> {
            client.get("/entries?period=month");
            verify(repo).findByPeriod("month");
        });
    }

    @Test
    void getEntries_returnsEmptyListWhenNoneExist() {
        when(repo.findByPeriod(any())).thenReturn(List.of());

        JavalinTest.test(buildApp(), (server, client) -> {
            var resp = client.get("/entries");
            assertEquals(200, resp.code());
            assertEquals("[]", resp.body().string().trim());
        });
    }

    // --- POST /entries/fetch ---

    @Test
    void postEntriesFetch_fetchesPriceAndReturns201() {
        when(priceFetchService.fetchPrice("AAPL")).thenReturn(new BigDecimal("213.49"));
        when(repo.save(eq("AAPL"), any(), any())).thenReturn(entry("AAPL", "213.49"));

        JavalinTest.test(buildApp(), (server, client) -> {
            var resp = client.post("/entries/fetch", "{\"ticker\":\"AAPL\"}");
            assertEquals(201, resp.code());
            assertTrue(resp.body().string().contains("213.49"));
        });
    }

    @Test
    void postEntriesFetch_returns422OnInvalidTicker() {
        when(priceFetchService.fetchPrice("FAKE"))
                .thenThrow(new PriceFetchService.PriceFetchException("Price not found for: FAKE"));

        JavalinTest.test(buildApp(), (server, client) -> {
            var resp = client.post("/entries/fetch", "{\"ticker\":\"FAKE\"}");
            assertEquals(422, resp.code());
            assertTrue(resp.body().string().contains("Price not found"));
        });
    }
}
