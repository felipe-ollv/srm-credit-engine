CREATE INDEX idx_settlements_period_id
    ON settlements (settled_at DESC, id DESC);

CREATE INDEX idx_settlements_assignor_currency_period_id
    ON settlements (assignor_id, payment_currency, settled_at DESC, id DESC);
