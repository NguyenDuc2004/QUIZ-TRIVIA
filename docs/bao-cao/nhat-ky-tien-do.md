# Nhật ký tiến độ hàng ngày

> **Cách dùng:** Mỗi ngày làm việc tạo 1 mục theo [mẫu ở cuối file](#-mẫu-ngày-copy-để-dùng).
> Đổi `[ ]` → `[x]` khi làm xong. Việc chưa xong ghi vào **Nợ / chuyển ngày** để hôm sau kéo tiếp.
> Cột "Ghi chú báo cáo" là chỗ ghi nhanh số liệu / quyết định kỹ thuật để sau chép vào báo cáo đồ án.

**Ký hiệu:** `[ ]` chưa làm · `[x]` xong · `[~]` đang làm dở · `[!]` bị chặn/vướng

---

## Bảng theo dõi nhanh

| Ngày | Trọng tâm | Xong / Tổng | Trạng thái |
|------|-----------|-------------|------------|
| 27/07 | Khởi tạo repo + hạ tầng Docker | 0/4 | 🟡 đang làm |
| 05/08 | Dựng & kiểm tra hạ tầng 3 CSDL | 1/3 | 🟡 đang làm |

> 🔴 chưa bắt đầu · 🟡 đang làm · 🟢 xong · 🔵 nghỉ/đệm

---

## 📅 T2 — 27/07/2026 — Khởi tạo dự án & hạ tầng

**Mục tiêu hôm nay:** Dựng khung BE + FE và chạy được 3 CSDL bằng Docker.

### Nhiệm vụ
- [ ] Khởi tạo project Spring Boot (Maven, Java 21, cấu trúc package chuẩn)
- [ ] Khởi tạo project FE React + Vite + TypeScript
- [ ] Docker Compose: PostgreSQL (+pgvector), Neo4j, Redis — chạy `docker compose up -d` OK
- [ ] BE kết nối được 3 CSDL (health check khởi động không lỗi)

### Đã làm được
_(ghi lại khi kết thúc ngày)_

### Nợ / chuyển sang ngày sau
- _(...)_

### Vướng mắc
- _(...)_

### Ghi chú báo cáo
- _(quyết định kỹ thuật, version, cấu hình quan trọng cần đưa vào báo cáo)_

---

## 📅 T4 — 05/08/2026 — Dựng & kiểm tra hạ tầng 3 CSDL

**Mục tiêu hôm nay:** Chạy được PostgreSQL + Neo4j + Redis bằng Docker Compose và xác nhận kết nối thật.

### Nhiệm vụ
- [x] `docker compose up -d` — 3 container lên, kiểm tra kết nối từng CSDL
- [x] Khởi tạo project Spring Boot trong `backend/` (Maven, Java 21, package `com.datn.quizai`)
- [x] Khởi tạo project React + Vite + TypeScript trong `frontend/`
- [x] BE khởi động được, FE gọi được BE qua proxy, test `contextLoads` pass

### Đã làm được
- Tạo `.env` từ `.env.example` (đã gitignore).
- `docker compose up -d`: `quiz_postgres`, `quiz_neo4j`, `quiz_redis` đều Up; volume `datn_pgdata`, `datn_neo4jdata`, `datn_redisdata` được tạo mới.
- Kiểm tra thật từng CSDL:
  - PostgreSQL **16.14**, extension `vector` **0.8.6** đã bật sẵn qua `infra/postgres/init/01-extensions.sql`; thử toán tử cosine `'[1,2,3]'::vector <=> '[1,2,4]'::vector` = 0.00854 → pgvector hoạt động.
  - Neo4j 5: `cypher-shell` trả kết quả OK (bolt 7687, browser 7474).
  - Redis 7: `PING` → `PONG`.
- Chốt quy ước phát triển: monorepo `backend/` + `frontend/`, lát cắt dọc từng tính năng — `docs/conventions.md §6–§7`.

- Dựng khung `backend/`: Spring Boot 3.5.16, Java 21, Maven wrapper, 14 package theo `architecture.md §3`, `application.yml` đọc cấu hình từ biến môi trường.
- Dựng khung `frontend/`: React 19 + Vite 8 + TS, Ant Design v6, Tailwind v4 (không nạp preflight), TanStack Query, React Router 7, Zustand, RHF + Zod, axios client tập trung, proxy `/api` `/ws` `/actuator` sang :8080.
- `SecurityConfig` nền: chốt luật Guest — `/api/v1/auth/**` và `GET /api/v1/quizzes*` mở, còn lại `authenticated()`.
- **Kiểm chứng chạy thật:** `mvn test` pass (`contextLoads`), `npm run build` pass, BE trả `{"status":"UP"}`, FE dev server gọi BE qua proxy OK.
- Ma trận quyền đo bằng curl: `/actuator/health` 200 · `/api/v1/quizzes` 404 (qua được security, chưa có controller) · `/api/v1/users/me` **401** · `/v3/api-docs` 200.

### Nợ / chuyển sang ngày sau
- Migration Flyway `V1__init.sql` (bảng `users`) — schema hiện vẫn rỗng.
- Slice Auth: JWT filter, `AuthService`, endpoint `/auth/*`, trang đăng nhập/đăng ký FE, test.

### Vướng mắc
- **Xung đột cổng 5432 (đã xử lý xong):** máy đã cài sẵn PostgreSQL 17 (5432) và 18 (5433) chạy dạng service Windows → JDBC nối nhầm vào Postgres của máy, báo `password authentication failed for user "quiz"`. Ban đầu tạm chuyển container sang cổng 5434; sau đó **đã gỡ hẳn 2 bản PostgreSQL cài trên máy** nên trả container về **cổng chuẩn 5432**, test lại pass.
- Tiến độ chậm so với `ke-hoach-tien-do.md` (kế hoạch tuần 1 là 20–26/07, nay đã 05/08) → cần dồn Auth + Quiz trong các ngày tới.

### Ghi chú báo cáo
- **Mục 3.1 (Môi trường triển khai):** Docker Desktop 28.0.1 trên Windows 11; image `pgvector/pgvector:pg16` (PostgreSQL 16.14 + pgvector 0.8.6), `neo4j:5`, `redis:7-alpine`; cổng 5432 / 7474+7687 / 6379. Backend Spring Boot 3.5.16 + JDK 21.0.3, Maven 3.9.11; Frontend Node 22.17 + Vite 8.
- **Mục 1.6:** pgvector bật bằng `CREATE EXTENSION vector` lúc container khởi tạo lần đầu, dùng toán tử `<=>` (cosine distance) cho retrieval RAG.
- **Mục 2.3 / 3.4 (bảo mật):** có số liệu thực nghiệm đầu tiên — bảng mã trạng thái chứng minh Guest bị chặn đúng thiết kế (401 với `/users/me`, đi qua với `/quizzes`).

---

## 🧩 Mẫu ngày (copy để dùng)

```markdown
## 📅 <Thứ> — <dd/mm/yyyy> — <Tên trọng tâm>

**Mục tiêu hôm nay:** <mô tả ngắn>

### Nhiệm vụ
- [ ] <nhiệm vụ 1>
- [ ] <nhiệm vụ 2>
- [ ] <nhiệm vụ 3>

### Đã làm được
-

### Nợ / chuyển sang ngày sau
-

### Vướng mắc
-

### Ghi chú báo cáo
-
```

> Nhớ thêm 1 dòng vào **Bảng theo dõi nhanh** cho mỗi ngày mới.
