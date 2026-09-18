CREATE TABLE IF NOT EXISTS cuentas (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    saldo NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transferencias (
    id BIGSERIAL PRIMARY KEY,
    account_source_number VARCHAR(50) NOT NULL,
    account_target_number VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO cuentas (account_number, saldo, created_timestamp, updated_timestamp)
SELECT
    'ACC-' || LPAD(CAST(n AS TEXT), 6, '0') AS account_number,
    CAST((1000 + (RANDOM() * 50000)) AS NUMERIC(19, 2)) AS saldo,
    CURRENT_TIMESTAMP AS created_timestamp,
    CURRENT_TIMESTAMP AS updated_timestamp
FROM generate_series(1, 100) AS t(n)
ON CONFLICT (account_number) DO NOTHING;
