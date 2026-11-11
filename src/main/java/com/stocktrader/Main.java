package com.stocktrader;

import com.stocktrader.api.PortfolioRouter;
import com.stocktrader.db.AppConfig;
import com.stocktrader.repository.PortfolioRepository;
import com.stocktrader.service.PriceFetchService;
import com.stocktrader.service.PriceScheduler;
import io.javalin.Javalin;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public class Main {
    public static void main(String[] args) {
        DataSource ds = AppConfig.dataSource();

        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        PortfolioRepository repo = new PortfolioRepository(ds);
        PriceFetchService priceFetchService = new PriceFetchService();
        new PriceScheduler(repo, priceFetchService).start(60, 1);

        Javalin app = Javalin.create().start(8080);
        new PortfolioRouter(repo, priceFetchService).register(app);

        System.out.println("StockTrader running on http://localhost:8080");
    }
}
