package com.stocktrader.service;

import com.stocktrader.repository.PortfolioRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PriceScheduler {

    private final PortfolioRepository repo;
    private final PriceFetchService priceFetchService;
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "price-scheduler");
                t.setDaemon(true);
                return t;
            });

    public PriceScheduler(PortfolioRepository repo, PriceFetchService priceFetchService) {
        this.repo = repo;
        this.priceFetchService = priceFetchService;
    }

    public void start(long initialDelaySeconds, long intervalHours) {
        executor.scheduleAtFixedRate(
                this::fetchAll,
                initialDelaySeconds,
                intervalHours * 3600,
                TimeUnit.SECONDS
        );
        System.out.println("PriceScheduler started: runs every " + intervalHours + "h");
    }

    private void fetchAll() {
        List<String> tickers = repo.findDistinctTickers();
        if (tickers.isEmpty()) {
            System.out.println("PriceScheduler: no tickers in portfolio, skipping");
            return;
        }
        System.out.println("PriceScheduler: fetching prices for " + tickers);
        for (String ticker : tickers) {
            try {
                var price = priceFetchService.fetchPrice(ticker);
                repo.save(ticker, price, LocalDate.now());
                System.out.println("PriceScheduler: saved " + ticker + " @ " + price);
            } catch (Exception e) {
                System.err.println("PriceScheduler: failed for " + ticker + ": " + e.getMessage());
            }
        }
    }
}
