# Quiz/Trivia tích hợp AI

Nền tảng web trắc nghiệm tích hợp trí tuệ nhân tạo — đồ án tốt nghiệp ngành Kỹ thuật phần mềm, Trường ĐH Công nghiệp Hà Nội.

**Bốn trụ cột:**

1. **Phòng đấu trí real-time** nhiều người chơi, độ trễ thấp (Spring WebSocket + Redis Pub/Sub).
2. **Generative AI qua RAG** — trợ lý học tập và tự động sinh đề từ học liệu (Gemini → Grok dự phòng).
3. **Neo4j** — phân tích hành vi, gợi ý quiz và lộ trình học cá nhân hóa.
4. **Kiểm thử** hiệu năng chịu tải real-time và độ chính xác mô hình AI.

## Công nghệ

Java 21 · Spring Boot 3.5 · React 19 + Vite + TypeScript · Ant Design · PostgreSQL 16 + pgvector · Neo4j 5 · Redis 7 · Docker Compose

## Chạy dự án

```bash
cp .env.example .env          # rồi điền GEMINI_API_KEY, GROK_API_KEY
docker compose up -d          # PostgreSQL (5434) · Neo4j (7474/7687) · Redis (6379)

cd backend && ./mvnw spring-boot:run        # API: http://localhost:8080 · Swagger: /swagger-ui.html
cd frontend && npm install && npm run dev   # Web: http://localhost:5173
```

Yêu cầu: JDK 21, Maven 3.9+, Node 20+, Docker Desktop.

## Cấu trúc

```
backend/    Spring Boot — REST + WebSocket + AI/RAG
frontend/   React SPA
docs/       Tài liệu phân tích, thiết kế, báo cáo
infra/      Script khởi tạo hạ tầng
```

## Tài liệu

Điều hướng toàn bộ tài liệu: [docs/README.md](docs/README.md) · Quy ước phát triển: [docs/conventions.md](docs/conventions.md) · Hướng dẫn cho AI agent: [CLAUDE.md](CLAUDE.md)
