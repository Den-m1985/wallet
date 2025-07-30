-- liquibase formatted sql

-- changeset denis:2
-- comment: Create wallet table with all required columns including version for optimistic locking

CREATE TABLE IF NOT EXISTS wallet (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- rollback DROP TABLE wallet;