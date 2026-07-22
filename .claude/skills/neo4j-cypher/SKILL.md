---
name: neo4j-cypher
description: Dùng khi viết truy vấn Cypher, mô hình hóa đồ thị Neo4j, hoặc đồng bộ hành vi từ PostgreSQL sang Neo4j cho hệ gợi ý. Đảm bảo idempotent và tham số hóa.
---

# Neo4j & Cypher (hệ gợi ý)

## Mô hình đồ thị
```
(User)-[:ATTEMPTED {score,date}]->(Quiz)
(User)-[:INTERESTED_IN]->(Topic)
(User)-[:WEAK_IN {level}]->(Topic)
(Quiz)-[:BELONGS_TO]->(Topic)
(Quiz)-[:HAS]->(Question)-[:TESTS]->(Topic)
(Topic)-[:PREREQUISITE_OF]->(Topic)
(User)-[:SIMILAR_TO {score}]->(User)
```

## Quy tắc
1. **PostgreSQL là nguồn sự thật**; Neo4j là view phân tích.
2. **Đồng bộ idempotent:** dùng `MERGE` (không `CREATE`) để chạy lại không nhân đôi.
3. **Tham số hóa** (`$userId`) — không nối chuỗi (chống injection).
4. **Constraint/index:** unique trên `User.id`, `Quiz.id`, `Topic.name`.
5. Truy cập qua **Spring Data Neo4j**; tách repo đồ thị khỏi repo JPA.

## Mẫu đồng bộ (sau mỗi attempt)
```cypher
MERGE (u:User {id:$userId})
MERGE (q:Quiz {id:$quizId})
MERGE (u)-[a:ATTEMPTED]->(q)
SET a.score=$score, a.date=$date
```

## Mẫu gợi ý
```cypher
// Quiz theo chủ đề đang yếu, chưa làm
MATCH (u:User {id:$userId})-[:WEAK_IN]->(t:Topic)<-[:BELONGS_TO]-(q:Quiz)
WHERE NOT (u)-[:ATTEMPTED]->(q)
RETURN q, t ORDER BY q.rating DESC LIMIT 10
```
```cypher
// Lộ trình học: chủ đề tiếp theo theo tiên quyết
MATCH (u:User {id:$userId})-[:WEAK_IN]->(:Topic)-[:PREREQUISITE_OF]->(next:Topic)
RETURN DISTINCT next
```

## Checklist
- [ ] Dùng `MERGE` khi đồng bộ.
- [ ] Cypher tham số hóa.
- [ ] Có constraint unique cho node chính.
- [ ] Job đồng bộ chạy nền, không chặn request người dùng.

## Tham chiếu
`docs/features/07-recommendation-neo4j.md`, `docs/database.md` mục 2.
