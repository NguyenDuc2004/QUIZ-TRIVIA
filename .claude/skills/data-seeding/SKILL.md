---
name: data-seeding
description: Dùng khi cần tạo dữ liệu mẫu cho dev/demo — import câu hỏi từ Open Trivia DB, sinh user/lượt làm bài giả lập để populate leaderboard & đồ thị Neo4j, nạp học liệu mẫu cho RAG. Dữ liệu sạch, tái lập được, không scrape web tùy tiện.
---

# Tạo dữ liệu mẫu (seed) cho dev & demo

Mục tiêu: đủ data để **demo mượt + có số liệu cho báo cáo**, không phải big data. Ưu tiên nguồn có cấu trúc + tự sinh; **không scrape web bừa bãi** (bản quyền, data bẩn).

## 4 loại data — nguồn tương ứng

| Loại | Dùng cho | Nguồn |
|---|---|---|
| Câu hỏi trắc nghiệm | quiz, chơi đơn, phòng đấu | **Open Trivia DB** API (`opentdb.com/api.php`) — miễn phí, có category/độ khó/loại câu |
| Học liệu (3–5 tài liệu) | RAG: sinh đề + chatbot | File công khai (Wikipedia/giáo trình có quyền); ingest qua pipeline RAG |
| Câu hỏi "chất AI" | demo sinh đề + số liệu 3.6 | **Tự sinh bằng module AI của dự án** (dogfooding) |
| User / attempt / room | leaderboard, thống kê, **gợi ý Neo4j** | Generator giả lập (script) |

## Quy trình
1. **Chọn cơ chế nạp:**
   - Dữ liệu tĩnh, cố định → **Flyway migration** `V<n>__seed_*.sql` (chỉ cho data ổn định như category, roles).
   - Dữ liệu khối lượng/ngẫu nhiên → **CommandLineRunner/`@Profile("seed")`** hoặc script riêng (không nhét vào migration đã merge).
2. **Import OpenTDB:** gọi API → map field sang `question` (loại: `multiple`→single/multiple, `boolean`→true-false) + `answer_option` (đánh dấu `is_correct`). Chèn category tương ứng. Tiếng Anh → có thể dịch 1 phần bằng LLM cho demo tiếng Việt.
3. **Học liệu RAG:** đặt file mẫu vào thư mục seed → chạy ingest (Tika→chunk→embedding→`material_chunks`). Xác nhận retrieval trả đúng đoạn.
4. **Generator hành vi:** tạo ~20–50 user + vài trăm `quiz_attempts` phân bố theo chủ đề (có người "giỏi/yếu" chủ đề khác nhau) → để đồ thị Neo4j có `ATTEMPTED`/`WEAK_IN`/`SIMILAR_TO` ra gợi ý hợp lý.
5. **Đồng bộ Neo4j** sau seed (chạy job đồng bộ, idempotent `MERGE`) → kiểm truy vấn gợi ý trả kết quả.
6. **Idempotent & reset:** seed chạy lại không nhân đôi; có lệnh/script dọn data demo.

## Checklist
- [ ] Không scrape web tùy tiện; nguồn có cấu trúc/được phép.
- [ ] Seed tách khỏi migration schema (trừ data cố định nhỏ).
- [ ] Chạy lại không nhân đôi (idempotent).
- [ ] Attempt giả lập đủ đa dạng để Neo4j gợi ý có nghĩa.
- [ ] Không commit file học liệu vi phạm bản quyền / API key.
- [ ] Ghi lại nguồn data để đưa vào báo cáo (Tài liệu tham khảo).

## Chống mẫu (tránh)
- Nhét hàng nghìn bản ghi random vào Flyway migration. Crawl web bằng parser tự chế cho demo. Seed để mật khẩu plaintext. Data giả lập đồng nhất khiến gợi ý Neo4j vô nghĩa.
