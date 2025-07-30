-- liquibase formatted sql

-- changeset denis:1
-- comment: Create users

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- rollback DROP TABLE users;