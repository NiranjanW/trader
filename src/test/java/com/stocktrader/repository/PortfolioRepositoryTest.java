package com.stocktrader.repository;

import com.stocktrader.model.PortfolioEntry;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests against an in-memory H2 database.
 * No Postgres required; runs fully offline.
 */
class PortfolioRepositoryTest {

    private PortfolioRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        // Fresh in-memory DB per test
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:stocktrader_test_" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        ds.setUser("sa");
        ds.setPassword("");

        // Create schema (mirrors V1 migration)
        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE portfolio_entries (
                        id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                        ticker     VARCHAR(20)    NOT NULL,
                        price      DECIMAL(12, 4) NOT NULL,
                        entry_date DATE           NOT NULL
                    )
                    """);
        }

        repo = new PortfolioRepository(ds);
    }

    @Test
    void save_persistsEntryAndReturnsItWithId() {
        PortfolioEntry entry = repo.save("AAPL", new BigDecimal("213.49"), LocalDate.of(2026, 10, 17));
        assertNotNull(entry);
        assertNotNull(entry.id());
        assertEquals("AAPL", entry.ticker());
        assertEquals(0, new BigDecimal("213.49").compareTo(entry.price()));
        assertEquals(LocalDate.of(2026, 10, 17), entry.entryDate());
    }

    @Test
    void findByPeriod_week_returnsEntriesWithinLastSevenDays() {
        LocalDate today = LocalDate.now();
        repo.save("AAPL", new BigDecimal("200.00"), today.minusDays(3));  // in range
        repo.save("TSLA", new BigDecimal("150.00"), today.minusDays(10)); // out of range

        List<PortfolioEntry> results = repo.findByPeriod("week");
        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).ticker());
    }

    @Test
    void findByPeriod_day_returnsOnlyTodaysEntries() {
        LocalDate today = LocalDate.now();
        repo.save("AAPL", new BigDecimal("200.00"), today);
        repo.save("MSFT", new BigDecimal("300.00"), today.minusDays(1));

        List<PortfolioEntry> results = repo.findByPeriod("day");
        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).ticker());
    }

    @Test
    void findByPeriod_month_returnsEntriesWithinLastThirtyDays() {
        LocalDate today = LocalDate.now();
        repo.save("AAPL", new BigDecimal("200.00"), today.minusDays(20));  // in range
        repo.save("TSLA", new BigDecimal("150.00"), today.minusDays(45));  // out of range

        List<PortfolioEntry> results = repo.findByPeriod("month");
        assertEquals(1, results.size());
    }

    @Test
    void findDistinctTickers_returnsUniqueTickersOnly() {
        LocalDate today = LocalDate.now();
        repo.save("AAPL", new BigDecimal("200.00"), today);
        repo.save("AAPL", new BigDecimal("205.00"), today.minusDays(1));
        repo.save("TSLA", new BigDecimal("150.00"), today);

        List<String> tickers = repo.findDistinctTickers();
        assertEquals(2, tickers.size());
        assertTrue(tickers.containsAll(List.of("AAPL", "TSLA")));
    }

    @Test
    void findDistinctTickers_emptyTableReturnsEmptyList() {
        assertTrue(repo.findDistinctTickers().isEmpty());
    }
}
