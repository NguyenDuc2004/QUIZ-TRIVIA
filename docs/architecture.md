# Kiến trúc hệ thống

## 1. Sơ đồ tổng thể

```
┌─────────────────────────────────────────────────────────────┐
│                     Client (Browser)                         │
│     React SPA + TypeScript + Vite + Ant Design + Tailwind    │
└──────────────┬───────────────────────────┬──────────────────┘
              │ HTTPS (REST + SSE)         │ WebSocket (STOMP)
              ▼                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  Backend — Spring Boot                       │
│                                                              │
│  ┌────────────┐  ┌───────────┐  ┌──────────┐  ┌───────────┐ │
│  │ Controller │→ │  Service  │→ │Repository│→ │   Domain  │ │
│  │ REST + WS  │  │  (logic)  │  │ JPA/Neo4j│  │ (Entity)  │ │
│  └────────────┘  └─────┬─────┘  └──────────┘  └───────────┘ │
│   ┌────────────────────┼────────────────────┐               │
│   ▼                    ▼                    ▼               │
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐      │
│ │ RAG Pipeline │ │AI Orchestrator│ │ Realtime Game    │      │
│ │ ingest +     │ │ Gemini→Grok  │ │ Engine (WebSocket│      │
│ │ retrieval    │ │ +CircuitBrkr │ │ + Redis Pub/Sub) │      │
│ └──────────────┘ └──────────────┘ └──────────────────┘      │
│   Security (JWT) · Async Jobs · Caching · Validation         │
└──────┬──────────┬──────────────┬─────────────┬───────────────┘
      │          │              │             │
      ▼          ▼              ▼             ▼
┌───────────┐ ┌────────┐ ┌────────────┐ ┌──────────────────────┐
│PostgreSQL │ │ Neo4j  │ │   Redis    │ │ External AI Providers│
│+ pgvector │ │ đồ thị │ │ cache,     │ │ Gemini API (chính)   │
│dữ liệu +  │ │ hành vi│ │ session,   │ │ Grok API (fallback)  │
│vector     │ │ gợi ý  │ │ realtime   │ └──────────────────────┘
└───────────┘ └────────┘ └────────────┘
```

## 2. Nguyên tắc thiết kế

- **Layered architecture**: Controller → Service → Repository → Domain. Không để logic nghiệp vụ ở controller.
- **Hướng module theo tính năng** (feature-based package), mỗi module gần như một bounded context.
- **API stateless** (JWT) → sẵn sàng scale ngang; trạng thái phiên real-time đặt ở Redis.
- **Lớp AI cô lập** sau interface `AiProvider` → thay/đổi provider không ảnh hưởng nghiệp vụ.
- **Polyglot persistence**: mỗi loại dữ liệu dùng CSDL phù hợp (quan hệ / đồ thị / key-value).
- **Tác vụ AI nặng chạy bất đồng bộ** (job nền) + thông báo khi hoàn tất.

## 3. Cấu trúc package backend

```
com.datn.quizai
├── config          # SecurityConfig, WebSocketConfig, CorsConfig, OpenApiConfig, AsyncConfig, CacheConfig
├── auth            # Xác thực, JWT, RBAC
├── user            # Quản lý người dùng, hồ sơ
├── quiz            # Quiz, Question, QuestionOption, Category
├── attempt         # Làm bài, chấm điểm, lịch sử (attempt, attempt_answer)
├── realtime        # Multiplayer: WebSocket (STOMP), Room, GameEngine, Redis Pub/Sub
├── ai              # Lớp AI
│   ├── provider    #   AiProvider (interface), GeminiProvider, GrokProvider, AiOrchestrator
│   ├── rag         #   Ingestion, chunking, embedding, vector retrieval (pgvector)
│   ├── generation  #   Sinh đề từ học liệu (RAG)
│   ├── grading     #   Chấm & giải thích câu tự luận
│   └── chat        #   Trợ lý học tập RAG chatbot (SSE)
├── recommend       # Gợi ý cá nhân hóa dựa trên Neo4j (graph repository, Cypher)
├── analytics       # Thống kê, báo cáo
├── common          # Exception handler, DTO chung, tiện ích, base entity
└── QuizAiApplication.java
```

## 4. Các luồng dữ liệu quan trọng

### 4.1. Sinh đề bằng AI (RAG, bất đồng bộ)
```
Creator upload học liệu → Tika trích text → chunk → embedding → pgvector
Creator yêu cầu sinh đề → retrieval k đoạn liên quan → prompt + context
   → AiOrchestrator (Gemini→Grok) → validate JSON → câu hỏi nháp → Creator duyệt
```

### 4.2. Phòng đấu real-time
```
Host tạo phòng → room lưu Redis (TTL)
Người chơi join qua WebSocket (STOMP) → subscribe /topic/room/{code}
Host start → server phát câu hỏi đồng thời → người chơi gửi đáp án
   → server chấm + cập nhật điểm → phát live leaderboard qua Redis Pub/Sub
```

### 4.3. Gợi ý dựa trên Neo4j
```
Sau mỗi attempt (PostgreSQL) → event/job đồng bộ sang Neo4j
   → cập nhật quan hệ (User)-[:ATTEMPTED]->(Quiz), WEAK_IN, SIMILAR_TO
Learner mở trang gợi ý → truy vấn Cypher → danh sách quiz + lộ trình
   → LLM tóm tắt lý do gợi ý
```

## 5. Fallback AI (Gemini → Grok)

```
Request AI → AiOrchestrator
  1) Gemini (primary)  ── thành công → trả kết quả
                       └─ lỗi 429/5xx/timeout ↓
  2) Grok (fallback)   ── thành công → trả kết quả (source=grok)
                       └─ lỗi ↓
  3) Trả lỗi thân thiện + (tùy chọn) cache/template dự phòng
```

- Interface chung `AiProvider { generate(prompt, options); stream(...) }`.
- **Circuit breaker** (Resilience4j) tránh gọi lặp provider đang lỗi.
- Cấu hình thứ tự provider trong `application.yml`, Admin override được.
