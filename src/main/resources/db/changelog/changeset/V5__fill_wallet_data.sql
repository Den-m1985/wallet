-- liquibase formatted sql

-- changeset denis:5
-- comment: Insert test data into wallet table

INSERT INTO wallet (id, user_id, balance, version, created_at, updated_at)
VALUES
    (
        '11111111-1111-1111-1111-111111111112',
        '11111111-1111-1111-1111-111111111111',
        10000.00,
        0,
        now(),
        now()
    ),
    (
        '11111111-1111-1111-1111-111111111113',
        '11111111-1111-1111-1111-111111111111',
        5000.00,
        0,
        now(),
        now()
    );

-- rollback DELETE FROM wallet WHERE id IN (
--    '11111111-1111-1111-1111-111111111111',
--    '22222222-2222-2222-2222-222222222222'
-- );