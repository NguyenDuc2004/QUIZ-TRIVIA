# Bộ Agents & Skills cho dự án Quiz/Trivia AI

Bộ này được **tùy biến riêng** cho dự án (bám `docs/` và 4 trụ cột: Real-time, AI/RAG, Neo4j, Kiểm thử), lấy cảm hứng và tham chiếu từ các bộ mã nguồn mở dưới đây.

## Agents (`.claude/agents/`)
Chuyên gia được gọi khi cần tư vấn/hiện thực theo lĩnh vực.

| Agent | Vai trò | Model |
|-------|---------|-------|
| `spring-boot-architect` | Kiến trúc & code backend, phân lớp, package | opus |
| `rag-ai-engineer` | LLM/RAG, fallback Gemini→Grok, sinh đề, chấm, chatbot | opus |
| `realtime-engineer` | Phòng đấu real-time WebSocket + Redis | sonnet |
| `neo4j-graph-engineer` | Đồ thị Neo4j, Cypher, hệ gợi ý | sonnet |
| `react-frontend-engineer` | Frontend React + TypeScript | sonnet |
| `test-qa-engineer` | Kiểm thử, load test, đánh giá độ chính xác AI | sonnet |
| `security-reviewer` | Rà soát bảo mật (OWASP + bảo mật AI) | sonnet |

## Skills (`.claude/skills/`)
Quy trình tái sử dụng, tự kích hoạt theo ngữ cảnh hoặc gọi `/<tên>`.

| Skill | Khi dùng |
|-------|----------|
| `spring-feature` | Tạo mới tính năng backend (Controller/Service/Repository/DTO) |
| `flyway-migration` | Thay đổi schema PostgreSQL |
| `ai-orchestrator-call` | Gọi LLM qua orchestrator + fallback |
| `rag-pipeline` | Hiện thực/chỉnh pipeline RAG |
| `websocket-room` | Phòng đấu real-time |
| `neo4j-cypher` | Truy vấn/đồng bộ đồ thị Neo4j |
| `rest-api-conventions` | Thiết kế/rà soát REST endpoint |
| `react-feature` | Tạo tính năng frontend |
| `backend-testing` | Viết test theo tầng cho từng chức năng backend (JUnit/Mockito/Testcontainers/STOMP/SSE) |
| `data-seeding` | Seed dữ liệu mẫu (OpenTDB, generator user/attempt, học liệu RAG) |
| `eval-and-load-test` | Load test real-time (P95/throughput) + đánh giá độ chính xác AI → số liệu báo cáo |
| `viet-bao-cao` | Viết báo cáo ĐATN theo khung 3 chương HaUI + xuất Word đúng định dạng |

## Nguồn mã nguồn mở tham khảo
- [rrezartprebreza/spring-boot-skills](https://github.com/rrezartprebreza/spring-boot-skills) — skills Spring Boot (layered, JWT, Flyway, Redis, Spring AI, testing). Khớp stack nhất.
- [wshobson/agents](https://github.com/wshobson/agents) — bộ agents/skills/commands đa nền tảng.
- [VoltAgent/awesome-claude-code-subagents](https://github.com/VoltAgent/awesome-claude-code-subagents) — 100+ subagent theo lĩnh vực.
- [0xfurai/claude-code-subagents](https://github.com/0xfurai/claude-code-subagents) — subagent production-ready.
- [mingrath/awesome-claude-skills](https://github.com/mingrath/awesome-claude-skills), [travisvn/awesome-claude-skills](https://github.com/travisvn/awesome-claude-skills) — danh mục skills.

> Các file trong bộ này do dự án tự viết (bám tài liệu nội bộ), không sao chép trực tiếp từ repo ngoài. Có thể tham khảo các repo trên để bổ sung ví dụ code (`examples/`, `templates/`) cho từng skill khi cần.
