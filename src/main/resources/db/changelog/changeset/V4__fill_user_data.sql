-- liquibase formatted sql

-- changeset denis:4
-- comment: Fill data to user

INSERT INTO users (id, password, email, created_at, updated_at)
SELECT '11111111-1111-1111-1111-111111111111', '123456', 'user@example.com', now(), now()
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE id = '11111111-1111-1111-1111-111111111111'
);

-- rollback DELETE FROM users WHERE id = '11111111-1111-1111-1111-111111111111';