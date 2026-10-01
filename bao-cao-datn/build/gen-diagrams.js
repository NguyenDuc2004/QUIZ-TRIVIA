/* Sinh các sơ đồ Mermaid -> PNG (render qua Chrome), tiếng Việt có dấu.
 * Đề tài: Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo.
 *
 * Phân công công cụ:
 *   Mermaid (file này):  1.1 kiến trúc · 1.2 pipeline RAG · 2.28 ERD · 2.29 phân lớp & mô-đun
 *   PlantUML (gen-plantuml.js): 2.1-2.27 use case, sequence, VOPC
 *   HTML  (gen-mockup.js):      2.30-2.37 wireframe giao diện
 *   Mermaid (file này):  3.1 sơ đồ triển khai
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
/* Sơ đồ 2.29 (phân lớp và mô-đun) đã bỏ khỏi báo cáo: chữ trong hình chỉ cao 1,07 mm khi in.
 * Bỏ luôn định nghĩa ở đây, vì để lại thì lần chạy sau sinh ra một tệp ảnh mang số đã được
 * cấp cho sơ đồ khác. */

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

D["1.2"] = `flowchart TB
  classDef ing fill:#DCFCE7,stroke:#15803D,color:#14532D;
  classDef ret fill:#DBEAFE,stroke:#1D4ED8,color:#1E3A8A;
  classDef store fill:#EDE9FE,stroke:#6D28D9,color:#4C1D95;
  classDef llm fill:#FEE2E2,stroke:#B91C1C,color:#7F1D1D;
  classDef guard fill:#FEF9C3,stroke:#A16207,color:#713F12;

  subgraph P2["Pha 2 — Truy hồi và sinh"]
    direction TB
    Q["Câu hỏi hoặc yêu cầu sinh đề<br/>→ sinh vector cho truy vấn"]:::ret
    W2[("material_chunks<br/>— cùng kho Pha 1 —")]:::store
    FILT["Lọc quyền đọc TRƯỚC — tài liệu của tôi<br/>hoặc đã chia sẻ — rồi mới xếp theo<br/>khoảng cách cosine &lt;=&gt;, lấy top-K = 5"]:::guard
    THR{"Loại đoạn vượt<br/>ngưỡng 0,75"}:::guard
    NONE["Trả lời 'không biết',<br/>KHÔNG suy đoán"]:::guard
    LLM["Dựng prompt — chỉ dẫn hệ thống + ngữ cảnh<br/>rào trong khối dữ liệu — rồi gọi<br/>AiOrchestrator → Gemini (dự phòng Groq)"]:::llm
    OUT1["Câu hỏi nháp + đoạn nguồn<br/>→ người tạo nội dung duyệt"]:::ing
    OUT2["Trả lời theo luồng (SSE)<br/>+ danh sách tài liệu đã dựa vào"]:::ret
    Q --> FILT
    W2 --> FILT
    FILT --> THR
    THR -->|"còn đoạn"| LLM
    THR -->|"rỗng"| NONE
    LLM --> OUT1
    LLM --> OUT2
  end

  subgraph P1["Pha 1 — Nạp học liệu (chạy nền)"]
    direction TB
    F["Tệp PDF / DOCX / TXT<br/>hoặc văn bản dán tay"]:::ing
    TK["Apache Tika<br/>bóc tách văn bản"]:::ing
    CH["TextChunker — chia đoạn<br/>có chồng lấp"]:::ing
    EMB["Gemini embedding<br/>vector 768 chiều"]:::ing
    W1[("material_chunks<br/>(pgvector)")]:::store
    F --> TK --> CH --> EMB --> W1
  end
`;

D["2.18"] = `erDiagram
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

D["3.1"] = `flowchart TB
  classDef fe fill:#DBEAFE,stroke:#1D4ED8,color:#1E3A8A;
  classDef be fill:#FEF3C7,stroke:#B45309,color:#7C2D12;
  classDef data fill:#EDE9FE,stroke:#6D28D9,color:#4C1D95;
  classDef ext fill:#E5E7EB,stroke:#4B5563,color:#1F2937;
  classDef cfg fill:#FCE7F3,stroke:#BE185D,color:#831843;

  subgraph MAY["Máy đơn — Windows 11"]
    direction TB

    BROWSER["Trình duyệt<br/>Chrome"]:::fe

    subgraph APP["Tiến trình chạy trực tiếp trên máy"]
      direction LR
      VITE["Giao diện — Vite 8 dev server<br/>React 19 + TypeScript<br/><b>cổng 5173</b>"]:::fe
      BOOT["Máy chủ ứng dụng — Spring Boot 3.5<br/>Java 21 Temurin, Maven Wrapper<br/><b>cổng 8080</b>"]:::be
    end

    subgraph DOCKER["Docker Compose — docker compose up -d"]
      direction LR
      PG[("PostgreSQL 16 + pgvector<br/>pgvector/pgvector:pg16<br/><b>cổng 5432</b>")]:::data
      NEO[("Neo4j 5<br/>neo4j:5<br/><b>cổng 7687</b>")]:::data
      RD[("Redis 7<br/>redis:7-alpine<br/><b>cổng 6379</b>")]:::data
    end

    ENV["Tệp .env — ngoài quản lý phiên bản<br/>khoá mô hình · mật khẩu CSDL · khoá ký JWT"]:::cfg
    FLY["Flyway — 23 tệp migration<br/>dựng lược đồ lúc khởi động"]:::cfg
  end

  subgraph NGOAI["Ngoài máy — Internet"]
    NCC["<b>Nhà cung cấp mô hình</b><br/>Google Gemini · gemini-3.6-flash <i>(chính)</i><br/>Groq · openai/gpt-oss-120b <i>(dự phòng)</i>"]:::ext
  end

  BROWSER -->|"HTTP :5173"| VITE
  BROWSER -->|"REST · SSE · WebSocket :8080"| BOOT
  VITE -.->|"proxy /api"| BOOT

  BOOT -->|"JDBC"| PG
  BOOT -->|"Bolt"| NEO
  BOOT -->|"RESP · Pub/Sub"| RD

  BOOT -->|"HTTPS"| NCC

  ENV -.->|"nạp lúc khởi động"| BOOT
  FLY -.->|"áp lược đồ"| PG`;


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


