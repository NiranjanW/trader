# StockTrader

A Java 21 portfolio tracker that records stock prices by ticker over day, week, and month periods.

**Stack:** Java 21 · Javalin · jOOQ · Flyway · HikariCP · PostgreSQL · Gradle

---

## Prerequisites

- Java 21+
- PostgreSQL running locally

---

## Setup

### 1. Create the database

```bash
psql -U postgres -c "CREATE DATABASE stocktrader;"
```

### 2. Configure credentials

Copy the example env file and fill in your values:

```bash
cp .env.example .env
```

Edit `.env`:

```
DB_URL=jdbc:postgresql://localhost:5432/stocktrader
DB_USER=postgres
DB_PASSWORD=yourpassword
APP_PORT=8080
```

`.env` is gitignored and never committed.

### 3. Run

```bash
export $(cat .env | xargs) && ./gradlew run
```

Flyway runs automatically on startup and creates the `portfolio_entries` table on first boot.

The server starts at `http://localhost:8080`.

---

## API

### Add an entry manually

```
POST /entries
Content-Type: application/json

{
  "ticker": "AAPL",
  "price": 213.49,
  "entryDate": "2026-10-17"
}
```

### Fetch live price and save automatically

Looks up the current price from Yahoo Finance and saves a new entry for today:

```
POST /entries/fetch
Content-Type: application/json

{
  "ticker": "AAPL"
}
```

Optionally override the date:

```json
{ "ticker": "TSLA", "entryDate": "2026-10-17" }
```

Returns `201` with the saved entry, or `422` if the ticker is invalid.

### List entries by period

```
GET /entries?period=day      # today
GET /entries?period=week     # last 7 days (default)
GET /entries?period=month    # last 30 days
```

---

## Scheduled price updates

The app automatically fetches prices for all tickers in your portfolio every hour.
The first run happens 60 seconds after startup. No configuration needed.

---

## Generate jOOQ classes (optional)

Generates type-safe DSL classes from your live schema:

```bash
export $(cat .env | xargs) && ./gradlew generateMainJooqSchemaSource
```

Generated classes land in `build/generated/jooq/` and are compiled automatically.
They are never written to `src/` so your source files are safe.

---

## Project structure

```
src/main/
├── java/com/stocktrader/
│   ├── Main.java                          # entry point, wires everything
│   ├── api/PortfolioRouter.java           # REST routes
│   ├── db/AppConfig.java                  # HikariCP datasource
│   ├── model/PortfolioEntry.java          # domain record
│   ├── repository/PortfolioRepository.java # jOOQ queries
│   └── service/
│       ├── PriceFetchService.java         # Yahoo Finance HTTP client
│       └── PriceScheduler.java            # hourly price fetch job
└── resources/
    ├── application.properties
    └── db/migration/
        └── V1__create_portfolio_entries.sql
```
