-- liquibase formatted sql

-- changeset denis:3
-- comment: Create wallet transaction

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    operation_type VARCHAR(255),
    amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_wallet_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id)
);

-- rollback DROP TABLE wallet_transactions;