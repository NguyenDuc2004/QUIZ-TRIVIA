# 07 — Gợi ý cá nhân hóa dựa trên đồ thị (Neo4j)

**Ưu tiên:** [M] Must · **Trụ cột phiếu:** Neo4j — phân tích hành vi & gợi ý

## Mục tiêu
Phân tích hành vi/sở thích người dùng bằng **cơ sở dữ liệu đồ thị Neo4j** để đề xuất quiz phù hợp và lộ trình học tập cá nhân hóa theo năng lực.

## Use case
- Learner nhận danh sách quiz gợi ý theo điểm yếu/sở thích.
- Learner xem lộ trình học tập đề xuất (chủ đề nên học tiếp).

## Yêu cầu chức năng
- **FR-33** [M] Xây dựng đồ thị người dùng–chủ đề–quiz–câu hỏi từ hành vi.
- **FR-34** [M] Gợi ý quiz dựa trên thuật toán đồ thị (tương đồng/collaborative).
- **FR-35** [M] Đề xuất lộ trình học cá nhân hóa theo năng lực & điểm yếu.
- **FR-36** [S] LLM giải thích lý do gợi ý bằng ngôn ngữ tự nhiên.
- **FR-32** [S] Adaptive difficulty trong phiên làm bài (chọn câu theo chuỗi đúng/sai).

## Mô hình đồ thị
```
(User)-[:ATTEMPTED {score, date}]->(Quiz)
(User)-[:INTERESTED_IN]->(Topic)
(User)-[:WEAK_IN {level}]->(Topic)
(Quiz)-[:BELONGS_TO]->(Topic)
(Quiz)-[:HAS]->(Question)-[:TESTS]->(Topic)
(Topic)-[:PREREQUISITE_OF]->(Topic)
(User)-[:SIMILAR_TO {score}]->(User)
```

## Luồng xử lý
1. Sau mỗi attempt (Postgres) → event/job nền đồng bộ sang Neo4j (cập nhật ATTEMPTED, WEAK_IN, SIMILAR_TO).
2. Learner mở trang gợi ý → truy vấn Cypher → danh sách quiz / lộ trình.
3. (Tùy chọn) LLM tóm tắt kết quả đồ thị thành lý do gợi ý.

## Ví dụ Cypher
```cypher
// Gợi ý quiz theo chủ đề đang yếu, chưa làm
MATCH (u:User {id:$userId})-[:WEAK_IN]->(t:Topic)<-[:BELONGS_TO]-(q:Quiz)
WHERE NOT (u)-[:ATTEMPTED]->(q)
RETURN q, t ORDER BY q.rating DESC LIMIT 10
```
```cypher
// Lộ trình: chủ đề tiếp theo dựa trên tiên quyết
MATCH (u:User {id:$userId})-[:WEAK_IN]->(t:Topic)-[:PREREQUISITE_OF]->(next:Topic)
RETURN DISTINCT next
```

## API liên quan
[api.md](../api.md) mục 7 (`/recommendations`, `/recommendations/path`).

## Dữ liệu liên quan
Neo4j (đồ thị) — [database.md](../database.md) mục 2. Đồng bộ từ Postgres `quiz_attempts`.

## Ghi chú kỹ thuật
- Nguồn sự thật là PostgreSQL; Neo4j là view phân tích → job đồng bộ **idempotent**.
- Truy cập qua Spring Data Neo4j.
- Đánh giá: gợi ý có hợp lý theo năng lực không — [roadmap.md](../roadmap.md).
