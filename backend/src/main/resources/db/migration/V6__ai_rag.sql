-- V6: học liệu + vector embedding (RAG) + job AI nền + audit chi phí
-- Nguồn thiết kế: docs/database.md §1.2, §1.3 · đặc tả: docs/features/05-ai-rag-generation.md

CREATE TABLE learning_materials (
    id           UUID         PRIMARY KEY,
    owner_id     UUID         NOT NULL,
    title        VARCHAR(300) NOT NULL,
    source_type  VARCHAR(10)  NOT NULL,
    topic        VARCHAR(100),
    -- Đường dẫn file gốc do server sinh (/uploads/materials/<uuid>.pdf)
    file_url     VARCHAR(500),
    status       VARCHAR(12)  NOT NULL DEFAULT 'PROCESSING',
    -- Số ký tự trích được và số đoạn đã cắt — hiện trên giao diện để người dùng biết tài liệu "dày" cỡ nào
    char_count   INTEGER      NOT NULL DEFAULT 0,
    chunk_count  INTEGER      NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_learning_materials_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_learning_materials_type   CHECK (source_type IN ('PDF', 'DOCX', 'TXT', 'TEXT')),
    CONSTRAINT ck_learning_materials_status CHECK (status IN ('PROCESSING', 'READY', 'FAILED'))
);

CREATE INDEX idx_learning_materials_owner ON learning_materials (owner_id, created_at DESC);

-- Một đoạn học liệu kèm vector embedding.
-- 768 chiều khớp model `text-embedding-004` của Gemini (docs/tech-stack.md).
CREATE TABLE material_chunks (
    id          UUID        PRIMARY KEY,
    material_id UUID        NOT NULL,
    chunk_index INTEGER     NOT NULL,
    content     TEXT        NOT NULL,
    embedding   vector(768),
    metadata    JSONB,

    CONSTRAINT fk_material_chunks_material FOREIGN KEY (material_id)
        REFERENCES learning_materials (id) ON DELETE CASCADE,
    CONSTRAINT uk_material_chunks UNIQUE (material_id, chunk_index)
);

CREATE INDEX idx_material_chunks_material ON material_chunks (material_id, chunk_index);

-- Chỉ mục ivfflat cho similarity search bằng cosine distance (toán tử <=>).
-- `lists` nhỏ vì dữ liệu đồ án ít; tăng lên khi số đoạn lớn hơn vài chục nghìn.
CREATE INDEX idx_material_chunks_embedding ON material_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Job AI chạy nền. Tác vụ sinh đề mất hàng chục giây nên không giữ HTTP request,
-- client nhận jobId rồi hỏi lại trạng thái (docs/conventions.md §1 — Async).
CREATE TABLE ai_jobs (
    id           UUID        PRIMARY KEY,
    user_id      UUID        NOT NULL,
    type         VARCHAR(20) NOT NULL,
    status       VARCHAR(12) NOT NULL DEFAULT 'PENDING',
    -- Tham số đầu vào và kết quả đều là JSON để thêm loại job mới không phải đổi schema
    request      JSONB,
    result       JSONB,
    error_message TEXT,
    started_at   TIMESTAMPTZ,
    finished_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_ai_jobs_user   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_jobs_type   CHECK (type IN ('INGEST_MATERIAL', 'GENERATE_QUESTIONS')),
    CONSTRAINT ck_ai_jobs_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX idx_ai_jobs_user ON ai_jobs (user_id, created_at DESC);

-- Audit mọi lời gọi LLM: biết chi phí, độ trễ và provider nào đã phục vụ.
-- Đây cũng là nguồn số liệu cho mục 3.6 báo cáo (đánh giá AI).
CREATE TABLE ai_request_logs (
    id         UUID        PRIMARY KEY,
    user_id    UUID,
    feature    VARCHAR(20) NOT NULL,
    provider   VARCHAR(20) NOT NULL,
    model      VARCHAR(60),
    tokens_in  INTEGER,
    tokens_out INTEGER,
    latency_ms INTEGER,
    status     VARCHAR(12) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_ai_request_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_ai_request_logs_status CHECK (status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX idx_ai_request_logs_created ON ai_request_logs (created_at DESC);
