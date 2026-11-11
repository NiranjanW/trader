package com.stocktrader.repository;

import com.stocktrader.model.PortfolioEntry;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.jooq.impl.DSL.*;

public class PortfolioRepository {

    private final DataSource ds;

    public PortfolioRepository(DataSource ds) {
        this.ds = ds;
    }

    private DSLContext ctx() {
        return DSL.using(ds, SQLDialect.POSTGRES);
    }

    public PortfolioEntry save(String ticker, BigDecimal price, LocalDate entryDate) {
        return ctx()
                .insertInto(table("portfolio_entries"))
                .columns(field("ticker"), field("price"), field("entry_date"))
                .values(ticker, price, entryDate)
                .returning(field("id"), field("ticker"), field("price"), field("entry_date"))
                .fetchOne(r -> new PortfolioEntry(
                        r.get(field("id"), Long.class),
                        r.get(field("ticker"), String.class),
                        r.get(field("price"), BigDecimal.class),
                        r.get(field("entry_date"), LocalDate.class)
                ));
    }

    public List<PortfolioEntry> findByPeriod(String period) {
        LocalDate from = switch (period) {
            case "day"   -> LocalDate.now();
            case "month" -> LocalDate.now().minusMonths(1);
            default      -> LocalDate.now().minusWeeks(1); // "week" and fallback
        };
        return ctx()
                .select(field("id"), field("ticker"), field("price"), field("entry_date"))
                .from(table("portfolio_entries"))
                .where(field("entry_date").greaterOrEqual(from))
                .orderBy(field("entry_date").desc(), field("ticker").asc())
                .fetch(r -> new PortfolioEntry(
                        r.get(field("id"), Long.class),
                        r.get(field("ticker"), String.class),
                        r.get(field("price"), BigDecimal.class),
                        r.get(field("entry_date"), LocalDate.class)
                ));
    }

    public List<String> findDistinctTickers() {
        return ctx()
                .selectDistinct(field("ticker", String.class))
                .from(table("portfolio_entries"))
                .fetch(field("ticker", String.class));
    }
}
