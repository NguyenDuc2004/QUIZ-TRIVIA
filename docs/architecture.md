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

**Nguyên tắc:** nhóm theo **tính năng**, bên trong mỗi tính năng chia tiếp theo **tầng** (`controller` / `service` / `repository` / `domain` / `dto`). Nhờ vậy sửa một tính năng chỉ mở một thư mục, mà vẫn thấy rõ ranh giới các tầng.

```
com.datn.quizai
├── config          # SecurityConfig, OpenApiConfig, DotenvEnvironmentPostProcessor,
│                   # (sau) WebSocketConfig, AsyncConfig, CacheConfig
├── auth            # Xác thực, JWT, RBAC
│   ├── controller  #   AuthController
│   ├── service     #   AuthService, JwtService, RefreshTokenService
│   ├── security    #   JwtAuthenticationFilter
│   └── dto         #   RegisterRequest, LoginRequest, AuthResponse...
├── user            # Người dùng, hồ sơ
│   ├── controller · service · repository · domain (User, Role) · dto
├── quiz            # Quiz, Question, QuestionOption, QuizQuestion, Category
│   ├── controller  #   QuizController, QuestionController, CategoryController
│   ├── service     #   QuizService, QuestionService, CategoryService
│   ├── repository  #   QuizRepository, QuestionRepository, CategoryRepository
│   ├── domain      #   entity + enum (Difficulty, Visibility, QuestionType, QuestionSource)
│   └── dto
├── attempt         # Làm bài, chấm điểm, lịch sử, bảng xếp hạng
│   ├── controller  #   AttemptController
│   ├── service     #   AttemptService, AnswerGrader (logic chấm, thuần Java)
│   ├── repository  #   QuizAttemptRepository
│   ├── domain      #   QuizAttempt, AttemptAnswer, AnswerPayload (jsonb)
│   │               #   + enum AttemptMode, AttemptStatus, GradedBy
│   └── dto
├── file            # Tải ảnh lên & phục vụ tĩnh (dùng chung cho quiz và câu hỏi)
│   ├── controller  #   FileController
│   ├── service     #   FileStorageService, ImageType (dò chữ ký byte)
│   └── dto
├── realtime        # Multiplayer: WebSocket (STOMP), Room, GameEngine, Redis Pub/Sub
├── ai              # Lớp AI
│   ├── provider    #   AiProvider (interface), GeminiProvider, GrokProvider, AiOrchestrator
│   ├── rag         #   Ingestion, chunking, embedding, vector retrieval (pgvector)
│   ├── generation  #   Sinh đề từ học liệu (RAG)
│   ├── grading     #   Chấm & giải thích câu tự luận
│   └── chat        #   Trợ lý học tập RAG chatbot (SSE)
├── recommend       # Gợi ý cá nhân hóa dựa trên Neo4j (graph repository, Cypher)
├── analytics       # Thống kê, báo cáo
├── common          # BaseEntity, OwnershipGuard, dto/ (ApiError, PageResponse), exception/
└── QuizAiApplication.java
```

> `common` và `config` **không** chia theo tầng vì không phải tính năng nghiệp vụ.
> Thư mục test trong `src/test/java` phản chiếu đúng cấu trúc này.

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
