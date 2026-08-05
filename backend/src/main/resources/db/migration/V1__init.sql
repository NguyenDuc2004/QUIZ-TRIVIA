-- V1: khởi tạo schema — extension pgvector + bảng người dùng
-- Nguồn thiết kế: docs/database.md §1.2

-- Đặt ở migration (không chỉ dựa vào infra/postgres/init) để mọi môi trường,
-- kể cả container Testcontainers khi chạy test, đều có extension này.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    avatar_url    VARCHAR(500),
    role          VARCHAR(20)  NOT NULL DEFAULT 'LEARNER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_users_email  UNIQUE (email),
    CONSTRAINT ck_users_role   CHECK (role IN ('LEARNER', 'CREATOR', 'ADMIN'))
);

COMMENT ON TABLE  users               IS 'Người dùng hệ thống (Learner/Creator/Admin). Guest không lưu ở đây.';
COMMENT ON COLUMN users.email         IS 'Chuẩn hóa chữ thường trước khi lưu để so sánh không phân biệt hoa/thường';
COMMENT ON COLUMN users.password_hash IS 'Băm BCrypt, không bao giờ lưu plaintext';
COMMENT ON COLUMN users.role          IS 'LEARNER | CREATOR | ADMIN — một user có thể vừa học vừa tạo nội dung (CREATOR bao hàm quyền LEARNER)';
