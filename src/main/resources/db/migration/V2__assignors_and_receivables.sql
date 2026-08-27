CREATE TABLE assignors (
    id UUID PRIMARY KEY,
    document VARCHAR(14) NOT NULL,
    legal_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_assignors_document UNIQUE (document),
    CONSTRAINT ck_assignors_document_digits CHECK (document ~ '^[0-9]{14}$'),
    CONSTRAINT ck_assignors_legal_name_not_blank CHECK (btrim(legal_name) <> '')
);

CREATE TABLE receivables (
    id UUID PRIMARY KEY,
    assignor_id UUID NOT NULL REFERENCES assignors (id),
    type VARCHAR(40) NOT NULL,
    face_value NUMERIC(19,2) NOT NULL,
    due_date DATE NOT NULL,
    registration_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    settlement_id UUID,
    settled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_receivables_type CHECK (type IN ('DUPLICATA_MERCANTIL', 'CHEQUE_PRE_DATADO')),
    CONSTRAINT ck_receivables_face_positive CHECK (face_value > 0),
    CONSTRAINT ck_receivables_due_after_registration CHECK (due_date > registration_date),
    CONSTRAINT ck_receivables_due_within_360_months CHECK (due_date <= registration_date + INTERVAL '360 months'),
    CONSTRAINT ck_receivables_status CHECK (status IN ('AVAILABLE', 'SETTLED')),
    CONSTRAINT ck_receivables_settlement_state CHECK (
        (status = 'AVAILABLE' AND settlement_id IS NULL AND settled_at IS NULL)
        OR (status = 'SETTLED' AND settlement_id IS NOT NULL AND settled_at IS NOT NULL)
    )
);

CREATE INDEX idx_assignors_legal_name ON assignors (lower(legal_name));
CREATE INDEX idx_receivables_assignor_status ON receivables (assignor_id, status);
CREATE INDEX idx_receivables_status_due_date ON receivables (status, due_date);
