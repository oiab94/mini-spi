CREATE OR REPLACE FUNCTION spi_apply_transfer_result(
    p_source_account_number TEXT,
    p_target_account_number TEXT,
    p_amount NUMERIC(19,4),
    p_idempotency_key TEXT,
    p_final_status TEXT
)
RETURNS TABLE (
    transfer_id BIGINT,
    status TEXT,
    message TEXT,
    source_account TEXT,
    target_account TEXT,
    amount NUMERIC(19,4)
)
LANGUAGE plpgsql
AS $$
DECLARE
v_now TIMESTAMPTZ := NOW();
    v_existing RECORD;
    v_source RECORD;
    v_target RECORD;
    v_transfer_id BIGINT;
BEGIN
    -- Validaciones mínimas
    IF p_source_account_number IS NULL OR p_source_account_number = '' THEN
        RETURN QUERY
SELECT NULL::BIGINT, 'RECHAZADA', 'Source account is required', p_source_account_number, p_target_account_number, p_amount;
RETURN;
END IF;

    IF p_target_account_number IS NULL OR p_target_account_number = '' THEN
        RETURN QUERY
SELECT NULL::BIGINT, 'RECHAZADA', 'Target account is required', p_source_account_number, p_target_account_number, p_amount;
RETURN;
END IF;

    IF p_amount IS NULL OR p_amount <= 0 THEN
        RETURN QUERY
SELECT NULL::BIGINT, 'RECHAZADA', 'Amount must be greater than zero', p_source_account_number, p_target_account_number, p_amount;
RETURN;
END IF;

    IF p_idempotency_key IS NULL OR p_idempotency_key = '' THEN
        RETURN QUERY
SELECT NULL::BIGINT, 'RECHAZADA', 'Idempotency key is required', p_source_account_number, p_target_account_number, p_amount;
RETURN;
END IF;

    IF p_final_status IS NULL OR p_final_status NOT IN ('EXITOSA', 'RECHAZADA') THEN
        RETURN QUERY
SELECT NULL::BIGINT, 'RECHAZADA', 'Final status must be EXITOSA or RECHAZADA', p_source_account_number, p_target_account_number, p_amount;
RETURN;
END IF;

    -- 1) Idempotencia
SELECT *
INTO v_existing
FROM transferencias
WHERE idempotency_key = p_idempotency_key
    LIMIT 1;

IF FOUND THEN
        -- Si ya existe, devolvemos el estado vigente
        RETURN QUERY
SELECT
    v_existing.id,
    v_existing.status::TEXT,
    'The transfer already exists with the current idempotency key.',
    v_existing.account_source_number,
    v_existing.account_target_number,
    v_existing.amount;
RETURN;
END IF;

    -- 2) Validar que las cuentas existan y bloquearlas
SELECT *
INTO v_source
FROM cuentas
WHERE account_number = p_source_account_number
    FOR UPDATE;

IF NOT FOUND THEN
        RETURN QUERY
SELECT NULL::BIGINT, 'RECHAZADA', 'Source account not found: ' || p_source_account_number, p_source_account_number, p_target_account_number, p_amount;
RETURN;
END IF;

SELECT *
INTO v_target
FROM cuentas
WHERE account_number = p_target_account_number
    FOR UPDATE;

IF NOT FOUND THEN
        RETURN QUERY
SELECT NULL::BIGINT, 'RECHAZADA', 'Target account not found: ' || p_target_account_number, p_source_account_number, p_target_account_number, p_amount;
RETURN;
END IF;

    IF p_source_account_number = p_target_account_number THEN
        RETURN QUERY
SELECT NULL::BIGINT, 'RECHAZADA', 'Source and target account must be different', p_source_account_number, p_target_account_number, p_amount;
RETURN;
END IF;

    -- 3) Insertar registro inicial como PENDIENTE
INSERT INTO transferencias (
    account_source_number,
    account_target_number,
    idempotency_key,
    status,
    amount,
    created_timestamp,
    updated_timestamp
)
VALUES (
           p_source_account_number,
           p_target_account_number,
           p_idempotency_key,
           'PENDIENTE',
           p_amount,
           v_now,
           v_now
       )
    RETURNING id INTO v_transfer_id;

-- 4) Aplicar el resultado que ya decidió el back-end
IF p_final_status = 'RECHAZADA' THEN
UPDATE transferencias
SET status = 'RECHAZADA',
    updated_timestamp = v_now
WHERE id = v_transfer_id;

RETURN QUERY
SELECT v_transfer_id, 'RECHAZADA', 'The external network rejected the transfer.', p_source_account_number, p_target_account_number, p_amount;
RETURN;
END IF;

    -- p_final_status = 'EXITOSA'
    IF v_source.saldo < p_amount THEN
UPDATE transferencias
SET status = 'RECHAZADA',
    updated_timestamp = v_now
WHERE id = v_transfer_id;

RETURN QUERY
SELECT v_transfer_id, 'RECHAZADA', 'Insufficient funds. The transfer was rejected.', p_source_account_number, p_target_account_number, p_amount;
RETURN;
END IF;

UPDATE cuentas
SET saldo = saldo - p_amount,
    updated_timestamp = v_now
WHERE account_number = p_source_account_number;

UPDATE cuentas
SET saldo = saldo + p_amount,
    updated_timestamp = v_now
WHERE account_number = p_target_account_number;

UPDATE transferencias
SET status = 'EXITOSA',
    updated_timestamp = v_now
WHERE id = v_transfer_id;

RETURN QUERY
SELECT v_transfer_id, 'EXITOSA', 'Transfer completed successfully.', p_source_account_number, p_target_account_number, p_amount;
END;
$$;