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
| password_hash | varchar | BCrypt |
| display_name | varchar | |
| avatar_url | varchar | nullable |
| role | enum | LEARNER / CREATOR / ADMIN |
| created_at, updated_at | timestamptz | |

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
| explanation | text | |
| difficulty | enum | |
| topic | varchar | |
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
| ai_feedback | text | nhận xét của AI (features/06) |
| graded_by | varchar | NOT_GRADED / AUTO / PENDING_AI / AI / HUMAN |
| answered_at | timestamptz | |

**learning_materials** (học liệu cho RAG)
| id | owner_id (FK) | title | source_type (pdf/docx/txt) | topic | status (processing/ready) | created_at |

**material_chunks** (đoạn học liệu + embedding — pgvector)
| id | material_id (FK) | chunk_index | content (text) | embedding (vector) | metadata (jsonb) |

**game_rooms** (metadata phòng đấu — trạng thái live nằm ở Redis)
| id | room_code (unique) | host_id (FK) | quiz_id (FK) | status (waiting/playing/finished) | created_at |

**game_room_players**
| id | room_id (FK) | user_id (FK) | final_score | joined_at |

**chat_sessions / chat_messages**
| chat_sessions: id, user_id, title, created_at |
| chat_messages: id, session_id, role (user/assistant), content, created_at |

**ai_request_logs** (audit & giám sát chi phí)
| id | user_id | feature (generation/grading/chat/recommend) | provider (gemini/grok) | model | tokens_in | tokens_out | latency_ms | status | created_at |

**flashcard_decks / flashcards / flashcard_reviews** (tính năng Flashcard + SRS — [features/11](features/11-flashcard-srs.md))
| flashcard_decks: id, owner_id, title, topic, created_at |
| flashcards: id, deck_id (FK), front, back, hint, tag, source (manual/ai/from_wrong_answer), created_at |
| flashcard_reviews: id, flashcard_id (FK), user_id (FK), ease_factor, interval_days, repetitions, due_date, last_reviewed_at | *(trạng thái SRS theo từng user)* |

**proctoring_events / attempt_integrity** (Chống gian lận — [features/12](features/12-anti-cheat.md))
| proctoring_events: id, attempt_id (FK), user_id (FK), event_type, detail (jsonb), occurred_at |
| attempt_integrity: id, attempt_id (FK), risk_score, flags (jsonb), ai_note (text), review_status (pending/valid/invalid), reviewed_by |

**Gamification** ([features/13](features/13-gamification.md))
| user_stats: user_id (PK), total_xp, level, current_streak, longest_streak, last_active_date |
| badges: id, code, name, description, condition (jsonb), icon |
| user_badges: id, user_id, badge_id, earned_at |
| daily_challenges: id, date, description, rule (jsonb), xp_reward |
| user_daily_progress: id, user_id, challenge_id, progress, completed_at |

**Classroom** ([features/14](features/14-classroom.md))
| classrooms: id, owner_id, name, class_code (unique), description, created_at |
| classroom_members: id, classroom_id (FK), user_id (FK), role (student/co_teacher), joined_at |
| assignments: id, classroom_id (FK), quiz_id (FK), title, open_at, due_at, created_at |
| *(quiz_attempts bổ sung cột `assignment_id` FK nullable)* |

**Seasonal leaderboard** ([features/15](features/15-seasonal-leaderboard.md)) — *bảng xếp hạng live nằm ở Redis ZSET*
| seasons: id, name, start_at, end_at, status (active/ended) |
| season_rankings: id, season_id (FK), user_id (FK), final_score, final_rank, reward_badge_id |

**Notifications** ([features/16](features/16-notifications.md))
| notifications: id, user_id (FK), type, title, body, data (jsonb), is_read, created_at |
| notification_settings: user_id (PK), srs_reminder, assignment_due, achievement, email_enabled, quiet_hours (jsonb) |

### 1.3. pgvector (RAG)

```sql
CREATE EXTENSION IF NOT EXISTS vector;
-- material_chunks.embedding kiểu vector(768)  -- tuỳ model embedding
CREATE INDEX ON material_chunks USING ivfflat (embedding vector_cosine_ops);
```

> Quản lý migration bằng **Flyway** (`V1__init.sql`, `V2__rag.sql`, ...).

---

## 2. Neo4j (đồ thị gợi ý & hành vi)

### 2.1. Node & quan hệ

**Node:** `User`, `Quiz`, `Question`, `Topic`.

```
(User)-[:ATTEMPTED {score, date}]->(Quiz)
(User)-[:INTERESTED_IN]->(Topic)
(User)-[:WEAK_IN {level}]->(Topic)
(Quiz)-[:BELONGS_TO]->(Topic)
(Quiz)-[:HAS]->(Question)-[:TESTS]->(Topic)
(Topic)-[:PREREQUISITE_OF]->(Topic)     // dựng lộ trình học
(User)-[:SIMILAR_TO {score}]->(User)    // collaborative filtering
```

### 2.2. Đồng bộ dữ liệu

- Nguồn sự thật (source of truth) là PostgreSQL.
- Sau mỗi `attempt`, phát sự kiện / job nền cập nhật đồ thị Neo4j (ATTEMPTED, WEAK_IN, SIMILAR_TO).
- Truy cập qua **Spring Data Neo4j**, truy vấn bằng **Cypher**.

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
| `ai:cache:{hash}` | Cache kết quả AI theo hash(prompt) | Tiết kiệm chi phí |
| `session:{token}` | Session / refresh token | |
| `quota:ai:{userId}` | Đếm hạn mức gọi AI theo user | Rate limiting |
| `leaderboard:season:{seasonId}` | Bảng xếp hạng theo mùa | **Sorted Set** (ZADD/ZREVRANK) |

> Redis Pub/Sub giúp đồng bộ trạng thái phòng khi backend chạy nhiều instance (scale ngang).
