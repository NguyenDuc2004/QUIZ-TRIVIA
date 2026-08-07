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

## Hai thứ bản thiết kế đầu bỏ đi, và vì sao

Bản mô hình đồ thị ban đầu có `(Quiz).rating` và `(Topic)-[:PREREQUISITE_OF]->(Topic)`. **Cả hai đều
không có nguồn dữ liệu thật trong hệ thống**, nên đưa vào là bịa số:

| Bỏ | Lý do | Thay bằng |
|---|---|---|
| `q.rating` để sắp xếp gợi ý | Chưa có tính năng đánh giá quiz. Sắp theo một con số không tồn tại thì thứ tự gợi ý là ngẫu nhiên nhưng *trông có vẻ có căn cứ* — tệ hơn là không sắp | Số lượt người thật đã làm, và số câu của quiz khớp chủ đề đang yếu |
| `(Topic)-[:PREREQUISITE_OF]->(Topic)` | Không ai khai báo "Vòng lặp phải học trước Mảng". Tự sinh quan hệ tiên quyết là hệ thống tự nghĩ ra kiến thức sư phạm nó không có | Xếp chủ đề theo **mức độ yếu đo được**: học cái đang sai nhiều nhất trước |

Đây là ràng buộc chung của dự án: *không bịa dữ liệu cho đẹp giao diện* (CLAUDE.md §5). Một lộ trình
xếp theo tỷ lệ đúng thật của người học vẫn là lộ trình cá nhân hoá — chỉ là nó thành thật về việc nó
dựa trên cái gì.

## Mô hình đồ thị

```
(User {id})
(Topic {name})
(Quiz {id, title, visibility})

(User)-[:ATTEMPTED {score, maxScore, accuracy, at}]->(Quiz)
(User)-[:PRACTICED {correct, total, accuracy}]->(Topic)
(Quiz)-[:COVERS {questionCount}]->(Topic)
```

**Chỉ ba loại quan hệ, đều mang số liệu đo được.** Bản đầu có thêm `WEAK_IN`, `INTERESTED_IN`,
`STRONG_IN`, `SIMILAR_TO` — gộp lại được vì:

- `WEAK_IN` / `STRONG_IN` / `INTERESTED_IN` chỉ là `PRACTICED` nhìn qua một ngưỡng. Nếu nướng ngưỡng
  vào **cạnh** thì đổi ngưỡng phải chạy lại toàn bộ đồ thị; để ngưỡng trong **truy vấn** thì đổi lúc
  nào cũng được. Cạnh giữ *sự thật* (đúng 4/10 câu), truy vấn giữ *cách diễn giải* (dưới 60% là yếu).
- `SIMILAR_TO` không cần lưu: "người giống tôi" tính được ngay trong truy vấn từ những quiz cùng làm.
  Lưu sẵn thì phải có job cập nhật, và nó lỗi thời ngay sau mỗi bài nộp.

## Luồng xử lý

1. **Sau mỗi bài nộp** (PostgreSQL) → sự kiện `AttemptFinishedEvent` → job nền đồng bộ sang Neo4j.
   Chạy sau `AFTER_COMMIT`, và **đồng bộ lại lần nữa** sau khi AI chấm xong câu tự luận, vì lúc nộp
   những câu đó còn 0 điểm.
2. Learner mở trang gợi ý → truy vấn Cypher → danh sách quiz / lộ trình.
3. *(FR-36, chưa làm)* LLM tóm tắt kết quả đồ thị thành lý do gợi ý.

Đồng bộ **idempotent**: toàn bộ dùng `MERGE` + `SET`, chạy lại bao nhiêu lần cũng cho cùng một đồ
thị. Cần vậy vì bước 1 chạy hai lần cho cùng một bài, và vì job nền có thể chạy lại sau lỗi.

## Truy vấn Cypher

```cypher
// FR-34a — Quiz thuộc chủ đề đang yếu mà chưa làm.
// Sắp theo số câu khớp chủ đề yếu (quiz càng trúng chỗ yếu càng lên trước),
// rồi tới số người thật đã làm. Không dùng rating vì không có rating.
MATCH (u:User {id: $userId})-[p:PRACTICED]->(t:Topic)<-[c:COVERS]-(q:Quiz)
WHERE p.total >= $minAnswers AND p.accuracy < $weakThreshold
  AND NOT (u)-[:ATTEMPTED]->(q)
  AND q.visibility = 'PUBLIC'
WITH q, collect(DISTINCT t.name) AS weakTopics, sum(c.questionCount) AS matchingQuestions
OPTIONAL MATCH (:User)-[a:ATTEMPTED]->(q)
RETURN q.id AS quizId, q.title AS title, weakTopics,
       matchingQuestions, count(a) AS attemptCount
ORDER BY matchingQuestions DESC, attemptCount DESC
LIMIT $limit
```

```cypher
// FR-34b — Lọc cộng tác: người từng làm cùng quiz với tôi thì còn làm gì nữa.
// `shared` = số quiz cùng làm, dùng làm độ tương đồng — tính tại chỗ, không lưu SIMILAR_TO.
MATCH (me:User {id: $userId})-[:ATTEMPTED]->(shared:Quiz)<-[:ATTEMPTED]-(peer:User)
WHERE peer.id <> $userId
WITH peer, count(DISTINCT shared) AS similarity
ORDER BY similarity DESC LIMIT $peerLimit
MATCH (peer)-[:ATTEMPTED]->(q:Quiz)
WHERE NOT (:User {id: $userId})-[:ATTEMPTED]->(q) AND q.visibility = 'PUBLIC'
RETURN q.id AS quizId, q.title AS title,
       sum(similarity) AS score, count(DISTINCT peer) AS peerCount
ORDER BY score DESC LIMIT $limit
```

```cypher
// FR-35 — Lộ trình: chủ đề xếp theo mức độ yếu đo được, yếu nhất học trước.
MATCH (u:User {id: $userId})-[p:PRACTICED]->(t:Topic)
OPTIONAL MATCH (t)<-[:COVERS]-(q:Quiz)
WHERE q.visibility = 'PUBLIC' AND NOT (u)-[:ATTEMPTED]->(q)
RETURN t.name AS topic, p.correct AS correct, p.total AS total,
       p.accuracy AS accuracy, count(DISTINCT q) AS availableQuizzes
ORDER BY p.accuracy ASC, p.total DESC
```

## API liên quan
[api.md](../api.md) mục 7 — `GET /recommendations`, `GET /recommendations/path`.

## Dữ liệu liên quan
Neo4j (đồ thị) — [database.md](../database.md) mục 2. Đồng bộ từ `quiz_attempts` + `attempt_answers`
+ `questions.topic`. **Chủ đề lấy từ `questions.topic`**, không phải từ `quizzes.category_id`: một
quiz trộn câu nhiều chủ đề thì nó "phủ" tất cả các chủ đề đó, và đó mới là thứ dùng để gợi ý.

## Ghi chú kỹ thuật
- Nguồn sự thật là PostgreSQL; Neo4j là view phân tích → job đồng bộ **idempotent**.
- Truy cập qua `Neo4jClient` với Cypher viết tay, không map `@Node`: đây là truy vấn phân tích chứ
  không phải CRUD thực thể, và Cypher viết rõ ra thì đọc lại/giải thích trong báo cáo dễ hơn nhiều.
- **Neo4j hỏng không được làm hỏng việc nộp bài.** Đồng bộ chạy nền và nuốt lỗi; API gợi ý trả danh
  sách rỗng kèm ghi chú thay vì 500. Gợi ý là tính năng phụ trợ, không đáng kéo sập luồng chính.
- Đánh giá: gợi ý có hợp lý theo năng lực không — [roadmap.md](../roadmap.md).

## Đồng bộ phải phản chiếu cả những gì đã biến mất

Bản đầu chỉ biết *thêm*: quiz hay tài khoản bị xoá ở PostgreSQL thì nút của nó nằm lại trong Neo4j
vĩnh viễn. Hậu quả không chỉ là rác — hệ thống sẽ **gợi ý một quiz đã bị xoá**, người dùng bấm vào
nhận 404. Đây là mặt còn thiếu của câu "Neo4j là view".

`syncPublicCatalog()` giờ làm hai việc: đưa danh mục quiz công khai vào đồ thị, rồi **gỡ** mọi nút
Quiz/User mà PostgreSQL không còn, cùng những Topic không còn cạnh nào trỏ tới.

Cũng ở đây sửa một lỗi thiết kế nặng hơn: lúc đầu đồ thị **chỉ được dựng khi có người làm bài**, nên
quiz chưa ai đụng tới thì không có trong đồ thị và không bao giờ được gợi ý. Mà gợi ý đúng là để giới
thiệu quiz người ta *chưa* làm — hệ thống tự loại mất đúng thứ nó cần đề xuất. Việc một quiz phủ chủ
đề nào là thuộc tính của chính quiz đó, không phải hành vi của ai, nên phải vào đồ thị độc lập.

## Ba nguồn gợi ý, không phải hai

Bản đầu chỉ có hai nguồn. Chạy thật mới thấy **cả hai cạn cùng lúc**: người học yếu Spring Boot
nhưng đã làm hết quiz Spring Boot thì nguồn "chủ đề yếu" rỗng; hệ thống mới có vài người dùng thì
nguồn "người giống bạn" cũng rỗng. Kết quả là khu Gợi ý trống trơn trong khi kho quiz vẫn còn
nguyên chủ đề người ta chưa đụng tới.

| Nguồn | Khi nào có tác dụng | Nhãn |
|---|---|---|
| `WEAK_TOPIC` | Đang yếu một chủ đề và còn quiz chưa làm thuộc chủ đề đó | Ôn chỗ đang yếu |
| `SIMILAR_LEARNERS` | Có người khác cùng làm những quiz mình đã làm | Người giống bạn đã làm |
| `NEW_TOPIC` | Còn chủ đề chưa từng luyện — **luôn có tác dụng khi kho còn quiz** | Chủ đề mới |

Nguồn thứ ba cũng giải luôn *cold start*: người vừa đăng ký chưa có hành vi nào, nhưng mọi chủ đề
đều là mới với họ, nên có gợi ý ngay từ lần đăng nhập đầu tiên.

## Ghi đồng thời: ba lớp phải sửa, không phải một

Chạy test đầy đủ moi ra một chuỗi lỗi đồng thời — hai luồng cùng đồng bộ hai bài *khác nhau* nhưng
*cùng một quiz* thì giành nhau nút Quiz.

| Triệu chứng | Nguyên nhân | Sửa |
|---|---|---|
| Quiz mất bớt chủ đề | `replaceQuizTopics` xoá rồi ghi bằng **hai lần gọi**; lệnh xoá của luồng sau chen vào giữa loạt ghi của luồng trước | Gộp xoá + ghi vào **một câu Cypher** với `UNWIND` |
| `Cannot run more queries in this transaction` | Vòng lặp thử lại nằm *trong* phương thức `@Transactional`, mà Neo4j đã huỷ cả transaction khi deadlock | Tách phần đọc sang bean riêng (`GraphSyncReader`); phần ghi + thử lại nằm **ngoài** transaction |
| `Node already exists with label Quiz` | Hai luồng cùng `MERGE` một nút chưa tồn tại, cả hai cùng quyết định tạo, ràng buộc duy nhất chặn kẻ tới sau | Bắt luôn `DataIntegrityViolationException` và thử lại |

Điểm dễ tin nhầm nhất: **`MERGE` nghe như "tạo nếu chưa có" nhưng không nguyên tử với luồng khác.**
Có ràng buộc duy nhất thì thay vì tạo hai nút, nó ném lỗi — và lỗi đó là lỗi *đụng độ*, tức là thử
lại được.

Thử lại an toàn **chính vì đồng bộ idempotent**. Đây là lần thứ hai tính chất đó trả công: lần đầu
là để chạy hai lượt cho mỗi bài (lúc nộp và sau khi AI chấm).

## Chưa làm
- **FR-36** LLM giải thích lý do gợi ý — sẽ tốn thêm hạn mức AI cho mỗi lần mở trang; cần cân nhắc
  cache trước khi bật.
- **FR-32** Adaptive difficulty trong phiên làm bài.
- Gỡ nút dùng `WHERE NOT id IN $ids` — với vài trăm bản ghi thì không sao, nhưng đây là phép so
  danh sách nên sẽ chậm dần; ngân hàng quiz lớn thì phải đổi sang đánh dấu theo lô.
