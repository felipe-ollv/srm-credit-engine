CREATE TABLE exchange_rates (
    id UUID PRIMARY KEY,
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    rate NUMERIC(19,10) NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_exchange_rates_currencies CHECK (
        base_currency IN ('BRL', 'USD')
        AND quote_currency IN ('BRL', 'USD')
        AND base_currency <> quote_currency
    ),
    CONSTRAINT ck_exchange_rates_rate_positive CHECK (rate > 0),
    CONSTRAINT ck_exchange_rates_capture_order CHECK (captured_at >= effective_at)
);

CREATE INDEX idx_exchange_rates_current
    ON exchange_rates (base_currency, quote_currency, effective_at DESC, captured_at DESC);
