CREATE TABLE portfolio_entries (
    id          BIGSERIAL PRIMARY KEY,
    ticker      VARCHAR(10)    NOT NULL,
    price       NUMERIC(12, 4) NOT NULL,
    entry_date  DATE           NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_portfolio_ticker      ON portfolio_entries (ticker);
CREATE INDEX idx_portfolio_entry_date  ON portfolio_entries (entry_date DESC);
