---
name: flyway-migration
description: Dùng khi cần thay đổi schema PostgreSQL (thêm/sửa bảng, cột, index, extension pgvector). Tạo file Flyway migration đúng quy ước, không sửa migration đã merge.
---

# Tạo Flyway migration

## Quy ước
- Thư mục: `src/main/resources/db/migration/`.
- Đặt tên: `V{n}__mo_ta_ngan.sql` (ví dụ `V5__add_game_rooms.sql`). `n` tăng dần, không trùng.
- **KHÔNG sửa** migration đã merge/đã chạy — luôn tạo migration mới.
- Mỗi migration là một thay đổi logic, có thể chạy lại an toàn khi hợp lý (`IF NOT EXISTS`).

## Các mẫu thường dùng
```sql
-- Bảng mới
CREATE TABLE game_rooms (
    id UUID PRIMARY KEY,
    room_code VARCHAR(12) UNIQUE NOT NULL,
    host_id UUID NOT NULL REFERENCES users(id),
    quiz_id UUID NOT NULL REFERENCES quizzes(id),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_game_rooms_status ON game_rooms(status);
```

```sql
-- Bật pgvector cho RAG
CREATE EXTENSION IF NOT EXISTS vector;
ALTER TABLE material_chunks ADD COLUMN embedding vector(768);
CREATE INDEX ON material_chunks USING ivfflat (embedding vector_cosine_ops);
```

## Checklist
- [ ] Tên file đúng `V{n}__...sql`, số thứ tự chưa dùng.
- [ ] Có khóa ngoại & index cần thiết.
- [ ] Không phá dữ liệu hiện có (thêm cột NOT NULL phải có DEFAULT hoặc backfill).
- [ ] Khớp Entity JPA tương ứng.

## Tham chiếu
`docs/database.md` mục 1 (schema PostgreSQL).
