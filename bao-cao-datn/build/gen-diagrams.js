/* Sinh các sơ đồ Mermaid -> PNG (render qua Chrome), tiếng Việt có dấu.
 * Đề tài: Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo.
 *
 * Phân công công cụ:
 *   Mermaid (file này):  1.1 kiến trúc · 1.2 pipeline RAG · 2.28 ERD · 2.29 phân lớp & mô-đun
 *   PlantUML (gen-plantuml.js): 2.1-2.27 use case, sequence, VOPC
 *   HTML  (gen-mockup.js):      2.30-2.37 wireframe giao diện
 */
const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");

const BUILD = __dirname;
const OUT = path.join(BUILD, "..", "assets");
const DG = path.join(BUILD, "diagrams");
fs.mkdirSync(OUT, { recursive: true });
fs.mkdirSync(DG, { recursive: true });
const mmdcCli = path.join(BUILD, "node_modules", "@mermaid-js", "mermaid-cli", "src", "cli.js");
const cfg = path.join(BUILD, "puppeteer-config.json");
const mcfg = path.join(BUILD, "mermaid-config.json");

const D = {};

/* ===== CHƯƠNG 1 ===== */

/* 1.1 — Kiến trúc tổng thể: ba kênh giao tiếp, khối đơn phân lớp, ba cơ sở dữ liệu, hai nhà cung cấp AI */
D["1.1"] = `flowchart TB
  classDef fe fill:#DBEAFE,stroke:#1D4ED8,color:#1E3A8A;
  classDef be fill:#FEF3C7,stroke:#B45309,color:#7C2D12;
  classDef data fill:#EDE9FE,stroke:#6D28D9,color:#4C1D95;
  classDef ext fill:#E5E7EB,stroke:#4B5563,color:#1F2937;

  subgraph CLIENT["Trình duyệt (Client)"]
    UI["React 19 + Vite 8 (TypeScript)<br/>Ant Design v6 + Tailwind v4"]:::fe
    WSC["STOMP.js — WebSocket Client<br/>(phòng đấu)"]:::fe
    SSEC["Fetch stream — SSE Client<br/>(trợ lý học tập)"]:::fe
  end

  subgraph BACKEND["Backend — Spring Boot 3.x + Java 21"]
    SEC["Security Filter — JWT + RBAC"]:::be
    API["REST Controllers (/api/v1)"]:::be
    WS["WebSocket Endpoint (/ws) — STOMP"]:::be
    SVC["Service Layer<br/>auth · quiz · attempt · realtime · ai · chat · recommend · analytics"]:::be
    RAG["RAG Pipeline<br/>Tika → chunk → embedding → truy hồi"]:::be
    ORCH["AiOrchestrator<br/>Gemini → Groq + Circuit Breaker"]:::be
    GAME["Realtime Game Engine<br/>SpeedScorer + Redis Pub/Sub"]:::be
  end

  subgraph STORE["Docker Compose — tầng dữ liệu"]
    PG[("PostgreSQL 16 + pgvector<br/>dữ liệu nghiệp vụ + kho vector")]:::data
    NEO[("Neo4j 5<br/>đồ thị hành vi, gợi ý")]:::data
    RD[("Redis 7<br/>phiên · trạng thái phòng · Pub/Sub · hạn mức")]:::data
  end

  GEM["Google Gemini API<br/>(nhà cung cấp chính)"]:::ext
  GRK["Groq API<br/>(dự phòng)"]:::ext

  UI -->|"REST / HTTPS"| API
  SSEC -->|"SSE (text/event-stream)"| API
  WSC -->|"WebSocket / STOMP"| WS
  API --> SEC
  WS --> SEC
  SEC --> SVC
  SVC --> RAG
  SVC --> ORCH
  SVC --> GAME
  SVC --> PG
  RAG --> PG
  SVC --> NEO
  GAME --> RD
  SVC --> RD
  ORCH -->|"HTTPS"| GEM
  ORCH -.->|"khi Gemini lỗi tạm thời"| GRK`;

/* 1.2 — Pipeline RAG hai pha, dùng chung cho sinh đề và trợ lý học tập */
D["1.2"] = `flowchart TB
  classDef ing fill:#DCFCE7,stroke:#15803D,color:#14532D;
  classDef ret fill:#DBEAFE,stroke:#1D4ED8,color:#1E3A8A;
  classDef store fill:#EDE9FE,stroke:#6D28D9,color:#4C1D95;
  classDef llm fill:#FEE2E2,stroke:#B91C1C,color:#7F1D1D;
  classDef guard fill:#FEF9C3,stroke:#A16207,color:#713F12;

  subgraph P1["Pha 1 — Nạp học liệu (chạy nền, một lần cho mỗi tài liệu)"]
    F["Tệp PDF / DOCX / TXT<br/>hoặc văn bản dán tay"]:::ing
    TK["Apache Tika<br/>bóc tách văn bản"]:::ing
    CH["TextChunker<br/>chia đoạn, có chồng lấp,<br/>cắt theo ranh giới câu"]:::ing
    EMB["Gemini embedding<br/>vector 768 chiều"]:::ing
  end

  VDB[("material_chunks (pgvector)<br/>content + embedding vector(768)")]:::store

  subgraph P2["Pha 2 — Truy hồi và sinh (mỗi lần có yêu cầu)"]
    Q["Câu hỏi của người dùng<br/>hoặc yêu cầu sinh đề"]:::ret
    QE["Sinh vector cho truy vấn"]:::ret
    FILT["Lọc quyền đọc TRƯỚC<br/>tài liệu của tôi HOẶC đã chia sẻ"]:::guard
    SIM["Xếp theo khoảng cách cosine &lt;=&gt;<br/>lấy top-K = 5"]:::ret
    THR["Loại đoạn vượt ngưỡng 0,75"]:::guard
    PB["Dựng prompt: chỉ dẫn hệ thống<br/>+ ngữ cảnh rào trong khối dữ liệu"]:::ret
  end

  LLM["AiOrchestrator → Gemini (dự phòng Groq)"]:::llm
  OUT1["Câu hỏi nháp + đoạn nguồn<br/>→ người tạo nội dung duyệt"]:::ing
  OUT2["Câu trả lời theo luồng (SSE)<br/>+ danh sách tài liệu đã dựa vào"]:::ret
  NONE["Không còn đoạn nào đủ liên quan<br/>→ trả lời 'không biết', KHÔNG suy đoán"]:::guard

  F --> TK --> CH --> EMB --> VDB
  Q --> QE --> FILT --> SIM --> THR
  THR -->|"còn đoạn hợp lệ"| PB
  THR -->|"rỗng"| NONE
  VDB --> FILT
  PB --> LLM
  LLM --> OUT1
  LLM --> OUT2`;

/* ===== CHƯƠNG 2 ===== */

/* 2.28 — ERD tổng quan cơ sở dữ liệu PostgreSQL (16 bảng đã hiện thực) */
D["2.28"] = `erDiagram
  users ||--o{ quizzes : "sở hữu"
  users ||--o{ questions : "soạn"
  users ||--o{ quiz_attempts : "làm bài"
  users ||--o{ learning_materials : "nạp"
  users ||--o{ game_rooms : "mở phòng"
  users ||--o{ chat_sessions : "hội thoại"
  users ||--o{ ai_jobs : "yêu cầu"
  users ||--o{ ai_request_logs : "phát sinh"
  categories ||--o{ quizzes : "phân loại"
  quizzes ||--o{ quiz_questions : "gồm"
  questions ||--o{ quiz_questions : "thuộc"
  questions ||--o{ question_options : "có phương án"
  quizzes ||--o{ quiz_attempts : "được làm"
  quizzes ||--o{ game_rooms : "dùng cho"
  quiz_attempts ||--o{ attempt_answers : "gồm"
  questions ||--o{ attempt_answers : "được trả lời"
  learning_materials ||--o{ material_chunks : "chia đoạn"
  game_rooms ||--o{ game_room_players : "có người chơi"
  chat_sessions ||--o{ chat_messages : "gồm"

  users {
    uuid id PK
    varchar email UK
    varchar password_hash "NULL nếu chỉ dùng Google"
    varchar google_id UK
    varchar display_name
    varchar role "LEARNER|CREATOR|ADMIN"
  }
  categories {
    uuid id PK
    varchar name
    varchar slug UK
  }
  quizzes {
    uuid id PK
    uuid owner_id FK
    uuid category_id FK
    varchar title
    varchar difficulty
    varchar visibility "public|private"
    int time_limit_sec
    boolean is_ai_generated
  }
  questions {
    uuid id PK
    uuid owner_id FK
    varchar type "5 loại câu hỏi"
    text content
    text explanation
    text rubric "tiêu chí chấm tự luận"
    varchar topic
    int points
    varchar source "manual|ai_generated"
  }
  question_options {
    uuid id PK
    uuid question_id FK
    text content
    boolean is_correct
    int order_index
  }
  quiz_questions {
    uuid quiz_id PK_FK
    uuid question_id PK_FK
    int order_index
  }
  quiz_attempts {
    uuid id PK
    uuid user_id FK "NOT NULL"
    uuid quiz_id FK
    varchar mode "PRACTICE|EXAM"
    varchar status
    timestamptz expires_at
    int total_score
    int max_score "chốt lúc bắt đầu"
  }
  attempt_answers {
    uuid id PK
    uuid attempt_id FK
    uuid question_id FK
    jsonb user_answer
    int score
    text ai_feedback
    text ai_suggestions
    varchar graded_by "AUTO|AI|AI_FAILED|HUMAN"
  }
  learning_materials {
    uuid id PK
    uuid owner_id FK
    varchar title
    varchar source_type
    varchar status "PROCESSING|READY|FAILED"
    boolean shared "mặc định false"
  }
  material_chunks {
    uuid id PK
    uuid material_id FK
    int chunk_index
    text content
    vector embedding "768 chiều"
  }
  game_rooms {
    uuid id PK
    varchar room_code UK "PIN 6 ký tự"
    uuid host_id FK
    uuid quiz_id FK
    varchar status
    boolean allow_guests "mặc định false"
  }
  game_room_players {
    uuid id PK
    uuid room_id FK
    uuid user_id FK "NULL nếu là khách"
    varchar display_name
    boolean is_guest
    int final_score
  }
  chat_sessions {
    uuid id PK
    uuid user_id FK
    varchar title
  }
  chat_messages {
    uuid id PK
    uuid session_id FK
    varchar role "USER|ASSISTANT"
    text content
  }
  ai_jobs {
    uuid id PK
    uuid user_id FK
    varchar type
    varchar status
    jsonb request
    jsonb result
  }
  ai_request_logs {
    uuid id PK
    uuid user_id FK
    varchar feature
    varchar provider "gemini|grok"
    int tokens_in
    int tokens_out
    int latency_ms
  }`;

/* 2.29 — Sơ đồ phân lớp và cấu trúc mô-đun backend */
D["2.29"] = `flowchart TB
  classDef layer fill:#FEF3C7,stroke:#B45309,color:#7C2D12;
  classDef mod fill:#DBEAFE,stroke:#1D4ED8,color:#1E3A8A;
  classDef shared fill:#E5E7EB,stroke:#4B5563,color:#1F2937;
  classDef data fill:#EDE9FE,stroke:#6D28D9,color:#4C1D95;

  subgraph L["Phân lớp — phụ thuộc một chiều"]
    direction TB
    C["Controller<br/>nhận yêu cầu, kiểm tra dữ liệu vào, trả DTO"]:::layer
    S["Service<br/>toàn bộ logic nghiệp vụ"]:::layer
    R["Repository<br/>JPA · JdbcTemplate (pgvector) · Neo4jClient"]:::layer
    E["Domain<br/>Entity và Enum"]:::layer
    C --> S --> R --> E
  end

  subgraph M["Mô-đun theo tính năng — com.datn.quizai (17 mô-đun)"]
    direction TB
    subgraph MA["Lõi"]
      direction LR
      M1["auth<br/>JWT, RBAC, OTP"]:::mod
      M2["user<br/>hồ sơ"]:::mod
      M3["quiz<br/>quiz, câu hỏi, danh mục"]:::mod
      M4["attempt<br/>làm bài, chấm điểm"]:::mod
      M10["file<br/>tải ảnh lên"]:::mod
    end
    subgraph MB["Thời gian thực và AI"]
      direction LR
      M5["realtime<br/>phòng đấu STOMP"]:::mod
      M6["ai<br/>RAG, sinh đề, chấm tự luận"]:::mod
      M7["chat<br/>trợ lý học tập (SSE)"]:::mod
      M8["recommend<br/>gợi ý Neo4j"]:::mod
    end
    subgraph MC["Mở rộng"]
      direction LR
      M9["analytics<br/>thống kê"]:::mod
      M11["admin<br/>quản trị, hạn mức AI"]:::mod
      M12["flashcard<br/>thẻ ghi nhớ, SRS"]:::mod
      M13["gamification<br/>XP, huy hiệu, streak"]:::mod
      M14["season<br/>xếp hạng theo mùa"]:::mod
      M15["integrity<br/>chống gian lận"]:::mod
      M16["classroom<br/>lớp học, giao bài"]:::mod
      M17["notification<br/>thông báo, nhắc ôn"]:::mod
    end
  end

  subgraph X["Không chia theo tầng — không phải tính năng nghiệp vụ"]
    direction LR
    CM["common<br/>BaseEntity · OwnershipGuard · ApiError · exception"]:::shared
    CF["config<br/>SecurityConfig · WebSocketConfig · RedisPubSubConfig"]:::shared
  end

  DB[("PostgreSQL + pgvector")]:::data
  NE[("Neo4j")]:::data
  RE[("Redis")]:::data

  M -->|"mỗi mô-đun chứa đủ 5 tầng bên trong"| L
  L --> DB
  L --> NE
  L --> RE
  X --- M`;

let ok = 0;
for (const [num, src] of Object.entries(D)) {
  const mmd = path.join(DG, `hinh-${num}.mmd`);
  const png = path.join(OUT, `hinh-${num}.png`);
  fs.writeFileSync(mmd, src, "utf8");
  try {
    execFileSync(process.execPath, [mmdcCli, "-i", mmd, "-o", png, "-p", cfg, "-c", mcfg, "-b", "white", "-s", "2"], { stdio: "pipe" });
    console.log(`OK hinh-${num}.png (${fs.statSync(png).size} bytes)`);
    ok++;
  } catch (e) {
    console.error(`FAIL hinh-${num}:`, (e.stderr || e.message || "").toString().slice(0, 400));
  }
}
console.log(`\nDone: ${ok}/${Object.keys(D).length} diagrams.`);
