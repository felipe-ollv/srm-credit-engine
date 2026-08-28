CREATE TABLE settlement_batches (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_settlement_batches_status CHECK (status IN ('PROCESSING', 'COMPLETED')),
    CONSTRAINT ck_settlement_batches_completion CHECK (
        (status = 'PROCESSING' AND completed_at IS NULL)
        OR (status = 'COMPLETED' AND completed_at IS NOT NULL)
    )
);

CREATE TABLE settlements (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES settlement_batches (id),
    item_index INTEGER NOT NULL,
    receivable_id UUID NOT NULL REFERENCES receivables (id),
    assignor_id UUID NOT NULL REFERENCES assignors (id),
    assignor_document VARCHAR(14) NOT NULL,
    assignor_legal_name VARCHAR(160) NOT NULL,
    receivable_type VARCHAR(40) NOT NULL,
    due_date DATE NOT NULL,
    face_value NUMERIC(19,2) NOT NULL,
    present_value NUMERIC(19,2) NOT NULL,
    discount NUMERIC(19,2) NOT NULL,
    payment_amount NUMERIC(19,2) NOT NULL,
    payment_currency VARCHAR(3) NOT NULL,
    term_months INTEGER NOT NULL,
    base_rate NUMERIC(19,10) NOT NULL,
    spread NUMERIC(19,10) NOT NULL,
    exchange_base_currency VARCHAR(3),
    exchange_quote_currency VARCHAR(3),
    exchange_rate NUMERIC(19,10),
    exchange_effective_at TIMESTAMPTZ,
    exchange_captured_at TIMESTAMPTZ,
    pricing_date DATE NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL,
    settled_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_settlements_receivable UNIQUE (receivable_id),
    CONSTRAINT uk_settlements_batch_item UNIQUE (batch_id, item_index),
    CONSTRAINT ck_settlements_item_index CHECK (item_index >= 0 AND item_index < 100),
    CONSTRAINT ck_settlements_receivable_type CHECK (
        receivable_type IN ('DUPLICATA_MERCANTIL', 'CHEQUE_PRE_DATADO')
    ),
    CONSTRAINT ck_settlements_assignor_snapshot CHECK (
        assignor_document ~ '^[0-9]{14}$' AND btrim(assignor_legal_name) <> ''
    ),
    CONSTRAINT ck_settlements_money CHECK (
        face_value > 0 AND present_value >= 0 AND discount >= 0
        AND face_value - present_value = discount AND payment_amount >= 0
    ),
    CONSTRAINT ck_settlements_term CHECK (term_months BETWEEN 1 AND 360),
    CONSTRAINT ck_settlements_rates CHECK (base_rate >= 0 AND spread >= 0),
    CONSTRAINT ck_settlements_payment_currency CHECK (payment_currency IN ('BRL', 'USD')),
    CONSTRAINT ck_settlements_exchange_snapshot CHECK (
        (payment_currency = 'BRL'
            AND exchange_base_currency IS NULL
            AND exchange_quote_currency IS NULL
            AND exchange_rate IS NULL
            AND exchange_effective_at IS NULL
            AND exchange_captured_at IS NULL)
        OR
        (payment_currency = 'USD'
            AND exchange_base_currency = 'USD'
            AND exchange_quote_currency = 'BRL'
            AND exchange_rate > 0
            AND exchange_effective_at IS NOT NULL
            AND exchange_captured_at IS NOT NULL)
    ),
    CONSTRAINT ck_settlements_timestamp_order CHECK (
        settled_at >= calculated_at
        AND (exchange_captured_at IS NULL OR exchange_captured_at >= exchange_effective_at)
    )
);

ALTER TABLE receivables
    ADD CONSTRAINT fk_receivables_settlement
    FOREIGN KEY (settlement_id) REFERENCES settlements (id)
    DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE settlement_idempotency (
    idempotency_key VARCHAR(64) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    batch_id UUID NOT NULL UNIQUE REFERENCES settlement_batches (id),
    status VARCHAR(20) NOT NULL,
    response_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT ck_settlement_idempotency_hash CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_settlement_idempotency_status CHECK (status IN ('PROCESSING', 'COMPLETED')),
    CONSTRAINT ck_settlement_idempotency_completion CHECK (
        (status = 'PROCESSING' AND response_payload IS NULL AND completed_at IS NULL)
        OR (status = 'COMPLETED' AND response_payload IS NOT NULL AND completed_at IS NOT NULL)
    )
);

CREATE INDEX idx_settlements_batch ON settlements (batch_id, item_index);
CREATE INDEX idx_settlements_period ON settlements (settled_at DESC);
CREATE INDEX idx_settlements_assignor_period ON settlements (assignor_id, settled_at DESC);
CREATE INDEX idx_settlements_currency_period ON settlements (payment_currency, settled_at DESC);
