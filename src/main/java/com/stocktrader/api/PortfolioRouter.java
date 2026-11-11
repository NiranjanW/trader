package com.stocktrader.api;

import com.stocktrader.model.PortfolioEntry;
import com.stocktrader.repository.PortfolioRepository;
import com.stocktrader.service.PriceFetchService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PortfolioRouter {

    private final PortfolioRepository repo;
    private final PriceFetchService priceFetchService;

    public PortfolioRouter(PortfolioRepository repo, PriceFetchService priceFetchService) {
        this.repo = repo;
        this.priceFetchService = priceFetchService;
    }

    public void register(Javalin app) {
        app.post("/entries", this::create);
        app.get("/entries", this::list);
        app.post("/entries/fetch", this::fetchAndSave);
    }

    private void create(Context ctx) {
        var req = ctx.bodyAsClass(PortfolioEntry.CreateRequest.class);
        var entry = repo.save(req.ticker(), req.price(), req.entryDate());
        ctx.status(201).json(entry);
    }

    private void list(Context ctx) {
        String period = ctx.queryParamAsClass("period", String.class).getOrDefault("week");
        ctx.json(repo.findByPeriod(period));
    }

    private void fetchAndSave(Context ctx) {
        record FetchRequest(String ticker, LocalDate entryDate) {}
        var req = ctx.bodyAsClass(FetchRequest.class);
        try {
            BigDecimal price = priceFetchService.fetchPrice(req.ticker());
            LocalDate date = req.entryDate() != null ? req.entryDate() : LocalDate.now();
            var entry = repo.save(req.ticker(), price, date);
            ctx.status(201).json(entry);
        } catch (PriceFetchService.PriceFetchException e) {
            ctx.status(422).json(new ErrorBody(e.getMessage()));
        }
    }

    record ErrorBody(String error) {}
}
