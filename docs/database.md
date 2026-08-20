# Thiết kế cơ sở dữ liệu

Hệ thống dùng **đa CSDL (polyglot persistence)**:

- **PostgreSQL 16 + pgvector** — dữ liệu nghiệp vụ chính + vector embedding học liệu (RAG).
- **Neo4j 5** — đồ thị hành vi/sở thích người dùng phục vụ gợi ý & lộ trình học.
- **Redis** — cache, session, quota, trạng thái phòng chơi real-time (Pub/Sub).

---

## 1. PostgreSQL

### 1.1. Sơ đồ quan hệ (tóm tắt)

```
users ──1:N── quizzes ──1:N── quiz_questions ──N:1── questions
  │                                                      │
  │                                    questions ──1:N── question_options
  │
  ├──1:N── quiz_attempts ──1:N── attempt_answers
  ├──1:N── chat_sessions ──1:N── chat_messages
  ├──1:N── learning_materials ──1:N── material_chunks (embedding)
  └──1:N── game_rooms ──1:N── game_room_players

categories ──1:N── quizzes
ai_request_logs (audit các lần gọi AI)
```

### 1.2. Bảng

**users**
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID (PK) | |
| email | varchar unique | |
| password_hash | varchar | BCrypt · **nullable** — NULL = tài khoản chỉ đăng nhập bằng Google |
| google_id | varchar(64) | nullable, unique (partial index bỏ qua NULL) · `sub` của Google |
| display_name | varchar | |
| avatar_url | varchar | nullable |
| role | enum | LEARNER / CREATOR / ADMIN |
| locked | boolean | *(V12)* true = quản trị viên đã khoá; không đăng nhập được và mọi phiên bị thu hồi. **Khoá là chặn truy cập, không phải xoá người dùng** — dữ liệu của họ (bài đã làm, quiz đã soạn, học liệu đã chia sẻ) là thứ người khác đang dùng. Chỉ mục một phần `WHERE locked = true` vì phần lớn bản ghi có giá trị false |
| created_at, updated_at | timestamptz | |

> `CHECK (password_hash IS NOT NULL OR google_id IS NOT NULL)` — mỗi tài khoản phải có ít nhất một
> cách đăng nhập; không để lọt bản ghi vào được bằng không đường nào.

**categories**
| id (PK) | name | slug | description |

**quizzes**
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID (PK) | |
| owner_id | FK users | |
| category_id | FK categories | |
| title, description | | |
| difficulty | enum | easy / medium / hard |
| visibility | enum | public / private |
| is_ai_generated | boolean | đánh dấu nguồn AI |
| time_limit_sec | int | nullable |
| created_at, updated_at | | |

> `quizzes` bổ sung cột `thumbnail_url VARCHAR(500)` ở *V4* — đường dẫn ảnh bìa do server sinh
> (`/uploads/images/<uuid>.<ext>`), NULL thì giao diện tự vẽ khối màu theo tiêu đề.

**questions**
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID (PK) | |
| owner_id | FK users | |
| type | enum | single / multiple / true_false / fill_blank / short_answer |
| content | text | |
| explanation | text | giải thích đáp án, hiện sau khi nộp |
| rubric | text | *(V9)* tiêu chí chấm câu tự luận. Không có rubric thì mô hình tự nghĩ ra thang điểm riêng và hai lần chấm cùng một bài lệch nhau |
| difficulty | enum | |
| topic | varchar | chữ tự do, **không phải bảng riêng** — gom lại khi cần qua `GET /questions/topics`. Không bắt tạo chủ đề trước mới soạn được câu |
| points | int | |
| source | enum | manual / ai_generated |
| ai_metadata | jsonb | provider, model, prompt_hash |

**question_options** (cho câu trắc nghiệm)
| id (PK) | question_id (FK) | content | is_correct | order_index |

**quiz_questions** (bảng nối, kèm thứ tự)
| quiz_id (FK) | question_id (FK) | order_index |

**quiz_attempts** *(V3)*
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID (PK) | |
| user_id, quiz_id | FK | |
| mode | varchar | PRACTICE / EXAM (FR-14) |
| status | varchar | IN_PROGRESS / SUBMITTED / EXPIRED |
| started_at | timestamptz | |
| expires_at | timestamptz | `started_at + quizzes.time_limit_sec`; NULL = không giới hạn (FR-16) |
| submitted_at | timestamptz | NULL khi chưa nộp |
| total_score, max_score | integer | `max_score` **chốt lúc bắt đầu** |
| created_at, updated_at | timestamptz | |

> Chỉ mục một phần `uk_quiz_attempts_in_progress (user_id, quiz_id) WHERE status = 'IN_PROGRESS'`:
> mỗi người tối đa một bài dở trên một quiz, gọi lại API bắt đầu là làm tiếp.

**attempt_answers** *(V3)* — mỗi dòng là **một câu trong đề của riêng bài làm đó**, sinh sẵn ngay khi
bắt đầu để chốt đề: chủ quiz thêm/bớt câu sau đó không ảnh hưởng bài đang làm hay bài đã nộp.

| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID (PK) | |
| attempt_id, question_id | FK | UNIQUE (attempt_id, question_id) |
| order_index | integer | sao từ `quiz_questions.order_index` lúc bắt đầu |
| user_answer | jsonb | `{"optionIds":[…]}` hoặc `{"text":"…"}`; NULL = chưa trả lời |
| is_correct | boolean | NULL khi chưa chấm hoặc đang chờ AI |
| score, max_score | integer | `max_score` chốt từ `questions.points` lúc bắt đầu |
| ai_feedback | text | nhận xét về bài đã làm (features/06) |
| ai_suggestions | text | *(V9)* việc cần làm để khá hơn — tách khỏi nhận xét để giao diện nhấn mạnh riêng |
| graded_by | varchar | NOT_GRADED / AUTO / PENDING_AI / AI / **AI_FAILED** *(V9)* / HUMAN |
| answered_at | timestamptz | |
| graded_at | timestamptz | *(V9)* chấm xong lúc nào — để dò câu kẹt `PENDING_AI` quá lâu |

> Index bộ phận `idx_attempt_answers_pending_ai ... WHERE graded_by = 'PENDING_AI'` *(V9)*: chỉ đánh
> trên số ít bản ghi đang chờ, không phình theo toàn bộ lịch sử làm bài.
>
> `AI_FAILED` là **trạng thái dừng** khi gọi mô hình hỏng. Không có nó thì câu nằm mãi ở
> `PENDING_AI` và người học thấy "đang chấm" vĩnh viễn mà không ai biết là đã hỏng.

**learning_materials** *(V6)* — học liệu cho RAG
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID (PK) | |
| owner_id | FK users | học liệu là dữ liệu riêng, mọi truy vấn đều lọc theo cột này |
| title, topic | varchar | |
| source_type | varchar | PDF / DOCX / TXT / TEXT (TEXT = dán tay) |
| status | varchar | PROCESSING / READY / FAILED |
| char_count, chunk_count | int | số ký tự trích được và số đoạn đã cắt |
| error_message | text | lý do xử lý hỏng, hiện thẳng lên giao diện |
| created_at, updated_at | timestamptz | |

**material_chunks** *(V6)* — đoạn học liệu + embedding (pgvector)
| id | material_id (FK) | chunk_index | content (text) | embedding **vector(768)** | metadata (jsonb) |

> 768 chiều khớp model embedding của Gemini. Similarity search bằng toán tử `<=>` (cosine distance).
> Truy vấn đi qua `MaterialChunkRepository` dùng JdbcTemplate — Hibernate không có kiểu `vector`.
>
> **Không có chỉ mục ANN trên `embedding`.** V6 từng tạo `ivfflat (lists = 100)`, V11 đã bỏ: truy vấn
> RAG phải lọc quyền đọc trước (tài liệu của tôi **hoặc** tài liệu người khác đã bật `shared`), còn
> `order by embedding <=> ? limit n` lại khiến PostgreSQL lấy `n` đoạn gần nhất **toàn kho** rồi mới
> lọc quyền — ứng viên không được phép đọc bị loại mà không có gì bù lại, thường còn rỗng. Đo trên dev:
> index trả 2 ứng viên đều thuộc tài liệu chưa chia sẻ → 0 đoạn; cùng câu truy vấn với `probes = 100`
> → 5 đoạn, khoảng cách 0.193–0.319. Sai trong im lặng, không lỗi không cảnh báo.
>
> Truy vấn hiện lọc quyền trong CTE `materialized` rồi mới tính khoảng cách, tức **tìm chính xác trên
> đúng tập được phép đọc**. Ở quy mô vài trăm tới vài nghìn đoạn, quét thẳng nhanh hơn dựng cây và
> không bỏ sót. Khi vượt cỡ vài chục nghìn đoạn mới cần ANN — dùng HNSW kèm `hnsw.iterative_scan`
> (pgvector 0.8) để index tự quét thêm khi bộ lọc quyền loại bớt ứng viên.

**ai_jobs** *(V6)* — tác vụ AI chạy nền
| id | user_id (FK) | type (INGEST_MATERIAL/GENERATE_QUESTIONS) | status (PENDING/RUNNING/SUCCEEDED/FAILED) |
| request (jsonb) | result (jsonb) | error_message | started_at | finished_at | created_at |

> `request`/`result` để JSON để thêm loại job mới không phải đổi schema.

**game_rooms** *(V5)* — metadata phòng đấu; trạng thái đang chơi nằm ở Redis `room:{code}`
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID (PK) | |
| room_code | varchar(8) unique | 6 ký tự, bỏ `0/O` và `1/I` cho dễ đọc |
| host_id, quiz_id | FK | |
| status | varchar | WAITING / PLAYING / FINISHED |
| seconds_per_question | int | NULL = theo `questions.time_limit_sec`, không có nữa thì mặc định 20s |
| allow_guests *(V7)* | boolean | host cho khách quét QR vào hay không; **mặc định FALSE** |
| started_at, finished_at | timestamptz | |
| created_at, updated_at | timestamptz | |

**game_room_players** *(V5, mở rộng ở V7)* — `final_score` chỉ ghi khi ván kết thúc, trong lúc chơi điểm ở Redis
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| id | UUID (PK) | |
| room_id | FK | |
| user_id | FK **nullable** *(V7)* | NULL khi là khách vãng lai |
| display_name *(V7)* | varchar(50) | chốt tại thời điểm chơi; với khách đây là nguồn duy nhất |
| avatar *(V7)* | varchar(40) | mã trong `PlayerAvatar` (emoji + màu, không phải file ảnh) |
| is_guest *(V7)* | boolean | |
| final_score | int | |
| joined_at | timestamptz | |

> UNIQUE `(room_id, user_id)` được thay bằng **chỉ mục một phần** `WHERE user_id IS NOT NULL`:
> nhiều khách trong cùng phòng đều có `user_id` NULL nên ràng buộc cũ không còn diễn đạt đúng ý.
> Thêm CHECK: hoặc là tài khoản thật, hoặc là khách có tên hiển thị.

> Khoá phiên khách nằm ở Redis `roomguest:{key}` (TTL 6 giờ), **không** lưu xuống PostgreSQL —
> nó chỉ có nghĩa trong đúng một ván đấu.

**chat_sessions / chat_messages**
| chat_sessions: id, user_id, title, created_at |
| chat_messages: id, session_id, role (user/assistant), content, created_at |

**ai_request_logs** *(V6)* — audit & giám sát chi phí
| id | user_id | feature (embedding/generation/grading/chat) | provider (gemini/groq) | model | tokens_in | tokens_out | latency_ms | status | error_message | created_at |

> Ghi trong transaction **riêng** (`REQUIRES_NEW`): job hỏng và rollback thì bản ghi audit vẫn phải còn.
> Đây là nguồn số liệu cho mục 3.6 báo cáo (chi phí, độ trễ, tỉ lệ fallback).

**flashcard_decks / flashcards / flashcard_reviews** (tính năng Flashcard + SRS — [features/11](features/11-flashcard-srs.md))
| flashcard_decks: id, owner_id, title, topic, created_at |
| flashcards: id, deck_id (FK), front, back, hint, tag, source (manual/ai/from_wrong_answer), created_at |
| flashcard_reviews: id, flashcard_id (FK), user_id (FK), ease_factor, interval_days, repetitions, due_date, last_reviewed_at | *(trạng thái SRS theo từng user)* |

**proctoring_events / attempt_integrity** *(V17)* — Chống gian lận ([features/12](features/12-anti-cheat.md))
| proctoring_events: id, attempt_id (FK), user_id (FK), event_type, detail (jsonb), occurred_at |
| attempt_integrity: id, attempt_id (FK, unique), risk_score, flags (jsonb), ai_note (text), review_status (PENDING/VALID/INVALID), reviewed_by, reviewed_at, review_note |

> `detail` **chỉ chứa số** — `{"length": 400}`, `{"seconds": 3}`. Không bao giờ chứa nội dung người dùng: server
> dựng lại trường này từ danh sách trường vô hại thay vì lưu nguyên gói tin của client, nên kể cả một bản client
> bị sửa cũng không đưa được nội dung vào đây.
>
> `review_status` mặc định **PENDING** và không có đường nào để hệ thống tự đổi nó — kết luận chỉ đến từ chủ quiz
> hoặc Admin. Chỉ ghi cho lượt **EXAM**; lượt PRACTICE không có dòng nào ở cả hai bảng.

**users.ai_daily_quota** *(V22)* — Hạn mức AI mỗi ngày ([features/10](features/10-admin.md))

> Cột `INTEGER` **nullable** trên `users`. `null` = chưa đặt riêng, dùng mặc định hệ thống; `0` = quản trị
> viên **cấm** người đó gọi AI. Gộp hai thứ thì hoặc không cấm được ai, hoặc mọi tài khoản mới bị cấm ngay —
> nên `DEFAULT NULL`, không phải `DEFAULT 0`.
>
> **Bộ đếm KHÔNG nằm ở đây.** Nó tăng mỗi lời gọi AI; ghi vào PostgreSQL là một UPDATE cho mỗi lời gọi trên
> đúng một dòng mà nhiều luồng tranh nhau. Đếm ở Redis (`aiquota:{userId}:{ngày}`) và **dựng lại từ
> `ai_request_logs`** khi Redis rỗng — cùng nguyên tắc "PostgreSQL là nguồn sự thật, Redis là chỉ mục" của
> bảng xếp hạng mùa. V22 thêm `idx_ai_request_logs_user_created` cho đúng phép dựng lại đó.

**quizzes.strict_exam** *(V21)* — Chế độ thi nghiêm ngặt ([features/12](features/12-anti-cheat.md))

> Cột `BOOLEAN NOT NULL DEFAULT FALSE` trên `quizzes`. Đặt ở quiz chứ không ở `quiz_attempts`: người quyết
> định mức nghiêm khắc là người ra đề, còn đặt ở lượt làm bài thì người làm tự tắt được. Mặc định FALSE để
> không đổi hành vi của quiz đang có.
>
> Cờ này chỉ có nghĩa với lượt `EXAM` — ràng buộc đó chốt ở tầng service vì `mode` nằm ở bảng khác, và API
> trả về giá trị **đã tính cho từng lượt** (`quiz.strictExam && mode == EXAM`) thay vì cờ thô.

**room_proctoring_events** *(V20)* — Cảnh báo live trong phòng đấu ([features/12](features/12-anti-cheat.md))
| room_proctoring_events: id, room_id (FK game_rooms), player_id, player_name, is_guest, event_type (TAB_HIDDEN/TAB_VISIBLE), question_index, occurred_at, created_at |

> **Bảng riêng, không dùng `proctoring_events`.** Bảng V17 có `attempt_id NOT NULL` → `quiz_attempts` và
> `user_id NOT NULL` → `users`. Phòng đấu vi phạm cả hai: nó **không tạo dòng `quiz_attempts` nào** (điểm nằm ở
> `game_room_players`), và **khách vãng lai không có dòng `users`**. Nhồi `attempt_id` giả để lách thì mọi thống
> kê theo lượt thi sẽ đếm cả dòng không thuộc lượt thi nào.
>
> **`player_id` không có khoá ngoại** — đó là danh tính *phạm vi phòng*: với thành viên là `users.id`, với khách
> là UUID sinh lúc vào phòng. Nhờ dùng chung một kiểu, phần ghi nhận không rẽ nhánh "nếu là khách thì…".
>
> **Không có `risk_score`, không có `review_status`.** Hai cột đó chỉ có nghĩa khi có người quay lại kết luận;
> ván xong là phòng tan. Thêm vào là hứa một quy trình xử lý không tồn tại.
>
> **`question_index` là cột làm nên khuôn lặp** — đếm số câu *khác nhau* có tín hiệu mới phân biệt được "một lần
> bị gián đoạn" với "lặp lại ở nhiều câu". `-1` = còn ở phòng chờ, không được tính.

**Gamification** ([features/13](features/13-gamification.md))
| user_stats: user_id (PK), total_xp, level, current_streak, longest_streak, last_active_date |
| badges: id, code, name, description, condition (jsonb), icon |
| user_badges: id, user_id, badge_id, earned_at |
| daily_challenges: id, date, description, rule (jsonb), xp_reward |
| user_daily_progress: id, user_id, challenge_id, progress, completed_at |

**Classroom** *(V19)* — Lớp học & giao bài ([features/14](features/14-classroom.md))
| classrooms: id, owner_id (FK), name, description, class_code (unique), created_at, updated_at |
| classroom_members: id, classroom_id (FK), user_id (FK), role (STUDENT/CO_TEACHER), joined_at — UNIQUE (classroom_id, user_id) |
| assignments: id, classroom_id (FK), quiz_id (FK **RESTRICT**), title, instruction, open_at, due_at, created_at, updated_at |
| *(quiz_attempts bổ sung cột `assignment_id` FK nullable, ON DELETE SET NULL)* |

> **`UNIQUE INDEX (assignment_id, user_id) WHERE assignment_id IS NOT NULL`** — mỗi học sinh một lượt cho mỗi
> bài tập. Đặc tả không nói, nhưng làm lại không giới hạn thì điểm bài tập mất hết ý nghĩa. Chốt ở CSDL vì
> kiểm trong Java thua cuộc khi học sinh mở hai tab.
>
> **`assignments.quiz_id` dùng `ON DELETE RESTRICT`**, khác mọi khoá ngoại còn lại trong lược đồ: CASCADE ở
> đây nghĩa là xoá một quiz sẽ xoá luôn bài tập và mọi điểm gắn với nó. Chặn để giáo viên nhận lỗi rõ ràng
> thay vì mất dữ liệu trong im lặng.
>
> **`class_code`** là 6 ký tự `[A-Z2-9]` — bỏ 0, O, 1, I, L vì mã này được đọc to trong lớp và chép tay lên
> bảng. Cùng lý do với mã PIN phòng đấu, nhưng dùng cả chữ vì lớp học sống lâu nên cần không gian mã lớn hơn.

**Seasonal leaderboard** ([features/15](features/15-seasonal-leaderboard.md)) — *bảng xếp hạng live nằm ở Redis ZSET*
| seasons: id, name, start_at, end_at, status (active/ended) |
| season_rankings: id, season_id (FK), user_id (FK), final_score, final_rank, reward_badge_id |

**Notifications** *(V18)* — Thông báo & nhắc ôn tập ([features/16](features/16-notifications.md))
| notifications: id, user_id (FK), type, title, body, data (jsonb), is_read, **dedupe_key**, created_at |
| notification_settings: user_id (PK), **disabled_types (jsonb)**, created_at, updated_at |

> **`dedupe_key` + `UNIQUE (user_id, dedupe_key)`** là chốt chống gửi trùng, thay cho khoá phân tán Redis mà
> đặc tả gợi ý: khoá phân tán chỉ chặn *hai instance cùng lúc*, còn ràng buộc duy nhất chặn **mọi** đường —
> deploy lại giữa trưa, gọi tay để thử, tính lại XP. Khoá là `srs:{ngày}`, `badge:{mã}`, `level:{cấp}`. NULL
> khi không cần chống trùng, và PostgreSQL coi mỗi NULL là một giá trị khác nhau nên nhiều dòng NULL cùng tồn
> tại được. Chèn bằng `ON CONFLICT DO NOTHING`, **không** bắt ngoại lệ — trùng khoá là đường chạy bình thường
> của một job hằng ngày.
>
> **`disabled_types`** là mảng jsonb tên các loại **bị tắt**, thay cho một cột boolean mỗi loại: thêm loại
> thông báo mới thì không phải đụng schema. Mặc định `[]` = bật tất cả — người chưa từng vào trang cài đặt vẫn
> nên nhận nhắc ôn, đó là lý do tính năng này tồn tại.

### 1.3. pgvector (RAG)

```sql
CREATE EXTENSION IF NOT EXISTS vector;
-- material_chunks.embedding kiểu vector(768)  -- tuỳ model embedding
-- KHÔNG tạo chỉ mục ANN: truy vấn RAG lọc quyền đọc trước rồi mới xếp theo khoảng cách,
-- mà index ANN thì xếp trước lọc sau nên bỏ sót kết quả (xem material_chunks ở §1.2 và V11).
```

> Quản lý migration bằng **Flyway** (`V1__init.sql`, `V2__rag.sql`, ...).

---

## 2. Neo4j (đồ thị gợi ý & hành vi)

### 2.1. Node & quan hệ

**Node:** `User {id}`, `Quiz {id, title, visibility}`, `Topic {name}`.

```
(User)-[:ATTEMPTED {score, maxScore, accuracy, at}]->(Quiz)
(User)-[:PRACTICED {correct, total, accuracy}]->(Topic)
(Quiz)-[:COVERS {questionCount}]->(Topic)
```

Ràng buộc duy nhất trên `User.id`, `Quiz.id`, `Topic.name` — tạo lúc ứng dụng khởi động. Neo4j không
có schema nên thiếu bước này thì `MERGE` vẫn chạy, chỉ là **quét toàn bộ nút** mỗi lần và chậm dần
theo kích thước đồ thị mà không có triệu chứng gì.

> **Bản thiết kế đầu có nhiều quan hệ hơn — đã lược đi có chủ đích.** Chi tiết lý do ở
> [features/07](features/07-recommendation-neo4j.md); tóm tắt:
>
> | Bỏ | Vì sao |
> |---|---|
> | `WEAK_IN` / `STRONG_IN` / `INTERESTED_IN` | Chỉ là `PRACTICED` nhìn qua một ngưỡng. Nướng ngưỡng vào **cạnh** thì đổi ngưỡng phải dựng lại cả đồ thị; để ở **truy vấn** thì đổi lúc nào cũng được. Cạnh giữ *sự thật đo được*, truy vấn giữ *cách diễn giải* |
> | `SIMILAR_TO` | Tính được ngay trong truy vấn từ những quiz cùng làm. Lưu sẵn thì phải có job cập nhật, mà nó lỗi thời ngay sau mỗi bài nộp |
> | `PREREQUISITE_OF` | **Không có nguồn dữ liệu.** Không ai khai báo "Vòng lặp phải học trước Mảng"; tự sinh là hệ thống bịa ra kiến thức sư phạm nó không có |
> | `Question` node, `BELONGS_TO`, `HAS`, `TESTS` | Câu hỏi chưa dùng tới trong truy vấn nào. Chủ đề của quiz suy ra được từ `COVERS` — thêm nút chỉ để đồ thị trông phong phú là thêm thứ phải giữ đồng bộ mà không dùng |

### 2.2. Đồng bộ dữ liệu

- **Nguồn sự thật là PostgreSQL; Neo4j chỉ là view phân tích.** Hệ quả thực tế: đồ thị lệch hay mất
  thì dựng lại được, nên không cần transaction hai pha, không cần rollback — chỉ cần **idempotent**.
- Sau mỗi bài nộp, sự kiện `AttemptSubmittedEvent` (pha `AFTER_COMMIT`) khởi động job nền đồng bộ.
  Đồng bộ **lần nữa** sau khi AI chấm xong câu tự luận (`AttemptRegradedEvent`) — lúc nộp những câu
  đó còn 0 điểm nên năng lực tính ra sai.
- Toàn bộ dùng `MERGE` + `SET`. Chạy lại bao nhiêu lần cũng cho cùng một đồ thị — bắt buộc, vì bước
  này *cố ý* chạy hai lần cho mỗi bài.
- Năng lực theo chủ đề **tính lại từ đầu** trên toàn bộ lịch sử, không cộng dồn: cộng dồn thì chạy
  hai lần là số liệu nhân đôi.
- `POST /recommendations/rebuild` dựng lại đồ thị của một người từ lịch sử — cần cho dữ liệu có
  trước khi tính năng ra đời, và để phục hồi nếu Neo4j mất dữ liệu.
- **Neo4j chết không kéo theo việc nộp bài:** đồng bộ chạy nền và nuốt lỗi; API gợi ý trả danh sách
  rỗng thay vì 500.
- Truy cập qua `Neo4jClient` với **Cypher viết tay**, không map `@Node` — đây là truy vấn phân tích
  chứ không phải CRUD thực thể.

### 2.3. Ví dụ truy vấn gợi ý (Cypher)

```cypher
// Gợi ý quiz theo chủ đề người dùng đang yếu, chưa từng làm
MATCH (u:User {id: $userId})-[:WEAK_IN]->(t:Topic)<-[:BELONGS_TO]-(q:Quiz)
WHERE NOT (u)-[:ATTEMPTED]->(q)
RETURN q, t ORDER BY q.rating DESC LIMIT 10
```

```cypher
// Lộ trình học: chủ đề tiếp theo dựa trên tiên quyết
MATCH (u:User {id: $userId})-[:WEAK_IN]->(t:Topic)-[:PREREQUISITE_OF]->(next:Topic)
RETURN DISTINCT next
```

---

## 3. Redis (real-time & cache)

| Key / Kênh | Vai trò | Ghi chú |
|------------|---------|---------|
| `room:{code}` | Trạng thái phòng chơi (người chơi, câu hiện tại, điểm) | TTL, hash/json |
| `room:{code}:events` (Pub/Sub) | Phát sự kiện game tới các instance backend | Đồng bộ độ trễ thấp |
| `roomguest:{key}` | Phiên khách vãng lai trong một phòng | TTL 6 giờ, chỉ dùng cho đúng phòng đó |
| `ai:cache:{hash}` | Cache kết quả AI theo hash(prompt) | Tiết kiệm chi phí |
| `session:{token}` | Session / refresh token | |
| `quota:ai:{userId}` | Đếm hạn mức gọi AI theo user | Rate limiting |
| `leaderboard:season:{seasonId}` | Bảng xếp hạng theo mùa | **Sorted Set** (ZADD/ZREVRANK) |

> Redis Pub/Sub giúp đồng bộ trạng thái phòng khi backend chạy nhiều instance (scale ngang).
