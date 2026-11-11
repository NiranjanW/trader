package com.stocktrader.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioEntry(
        Long id,
        String ticker,
        BigDecimal price,
        LocalDate entryDate
) {
    public record CreateRequest(String ticker, BigDecimal price, LocalDate entryDate) {}
}
