---
name: viet-bao-cao
description: "Soạn thảo báo cáo Đồ án tốt nghiệp (ĐATN) cho đề tài \"Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo\" theo mẫu trường ĐH Công nghiệp Hà Nội (HaUI). Dùng khi user yêu cầu viết/soạn/sinh báo cáo đồ án, phần Mở đầu, Chương 1 tổng quan công nghệ, Chương 2 khảo sát–phân tích–thiết kế (usecase, ERD, wireframe), Chương 3 thực nghiệm & đánh giá (kiểm thử, load test real-time, độ chính xác AI), Kết luận, hoặc xuất báo cáo ra file Word .docx. Skill nắm khung 3 chương chuẩn HaUI, văn phong học thuật tiếng Việt, định dạng trình bày (Times New Roman 13, lề 3-2-2-2cm, line 1.5) và biết lấy nội dung từ docs/ của dự án (overview.md, architecture.md, tech-stack.md, database.md, security.md, roadmap.md, features/*.md). KHÔNG dùng cho code, test, hay tài liệu kỹ thuật README."
---

# Viết báo cáo ĐATN — Quiz/Trivia tích hợp AI

Soạn báo cáo Đồ án tốt nghiệp cho đề tài **"Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo"** (SV Nguyễn Khắc Minh Đức — 2022601585) theo chuẩn trình bày **Trường ĐH Công nghiệp Hà Nội**.

## Nguyên tắc cốt lõi

1. **Nội dung lấy từ `docs/` và mã nguồn thực tế, KHÔNG bịa.** Mọi số liệu, tên module, tên bảng, yêu cầu chức năng phải truy về tài liệu nguồn (bảng map bên dưới). Spec không nói rõ → hỏi user, không suy diễn.
2. **Số liệu 3.5 (load test) và 3.6 (độ chính xác AI) chỉ được ghi khi đã đo thật** (skill `eval-and-load-test`). Chưa đo → để `«...»`, tuyệt đối không điền số ước lượng.
3. **Bám khung 3 chương HaUI** — xem `references/cau-truc-bao-cao.md`. Không tự ý đổi sang 4–5 chương.
4. **Văn phong học thuật**: ngôi "em" trong Lời cảm ơn/Mở đầu; thân bài dùng "hệ thống", "đồ án", câu khẳng định. KHÔNG văn nói, KHÔNG "tôi/mình". Mỗi chương có dẫn nhập đầu chương + chuyển tiếp cuối chương.
5. **Hình/bảng để placeholder rõ ràng** khi chưa có ảnh thật: `[HÌNH x.y: mô tả — cần chèn]` kèm caption đúng quy cách, để user bổ sung sau.
6. **Xuất Word bằng preset trong `references/dinh-dang-haui.md`** — không tự chế lại cấu hình docx từ đầu.

## Tài liệu tham khảo của skill

- `references/cau-truc-bao-cao.md` — **ĐỌC TRƯỚC khi viết bất kỳ phần nào.** Khung đầy đủ: thông tin bìa, front matter, Mở đầu, 3 chương, Kết luận + các **mẫu bảng** (đặc tả UC, mô tả bảng CSDL, kịch bản kiểm thử, bảng load test, bảng độ chính xác AI) + map chức năng → mục báo cáo.
- `references/dinh-dang-haui.md` — định dạng HaUI (font, lề, line, caption, đánh số trang) + preset script tạo `.docx`.

## Nơi lưu nội dung báo cáo

- Nội dung viết ra: `docs/bao-cao/noi-dung/` — mỗi phần một file: `00-mo-dau.md`, `01-tong-quan.md`, `02-phan-tich-thiet-ke.md`, `03-thuc-nghiem-danh-gia.md`, `04-ket-luan.md`, `05-tai-lieu-tham-khao.md`.
- Bản Word xuất ra: `docs/bao-cao/bao-cao-datn.docx` (hoặc từng chương rồi ghép).
- Nhật ký & số liệu phát sinh: `docs/bao-cao/nhat-ky-tien-do.md` (mục "Ghi chú báo cáo" của mỗi ngày) — luôn quét file này trước khi viết Chương 3.

## Map nội dung báo cáo → tài liệu nguồn

| Phần báo cáo | Lấy từ |
|---|---|
| Bìa, thông tin SV/CBHD, mục tiêu, kết quả dự kiến | `docs/phieu_giao_de_tai/phieu_giao_de_tai.md` |
| Mở đầu (lý do, phạm vi, từ viết tắt, tác nhân) | `docs/overview.md` |
| Chương 1 — công nghệ & kiến trúc | `docs/tech-stack.md`, `docs/architecture.md` |
| Chương 2 — yêu cầu chức năng, UC, luồng nghiệp vụ | `docs/features/README.md`, `docs/features/01..16-*.md`, `docs/api.md` |
| Chương 2.3 — yêu cầu phi chức năng, bảo mật | `docs/security.md` |
| Chương 2.8 — ERD & mô tả bảng | `docs/database.md` + migration Flyway |
| Chương 3.1 — môi trường triển khai | `docs/tech-stack.md §5`, `docker-compose.yml`, `.env.example` |
| Chương 3.2–3.3 — giao diện | screenshot thực tế từ `frontend/` |
| Chương 3.4 — kiểm thử | `docs/roadmap.md §2.1`, `src/test/`, skill `backend-testing` |
| Chương 3.5–3.6 — load test & độ chính xác AI | skill `eval-and-load-test`, `docs/roadmap.md §2.2–2.3` |
| Kết luận, hướng phát triển | `docs/roadmap.md`, `docs/overview.md §3` |

## Quy trình làm việc

Báo cáo dài → **viết theo từng phần, xác nhận xong mới sang phần kế**, KHÔNG sinh cả báo cáo một lần (khớp lối làm lát cắt dọc của dự án).

1. **Chốt phạm vi**: user muốn viết phần nào (Mở đầu / một chương / một mục / cả cuốn). Nếu là "cả cuốn" → vẫn xuất theo thứ tự từng phần và dừng lại cho user review.
2. **Đọc `references/cau-truc-bao-cao.md`** để lấy đúng dàn ý + mẫu bảng của phần đó.
3. **Đọc tài liệu nguồn** tương ứng (bảng map trên) + kiểm tra mã nguồn thực tế nếu phần đó mô tả cái đã code.
4. **Soạn Markdown trước** vào `docs/bao-cao/noi-dung/` cho user review (sửa nhanh hơn Word); chỉ soạn thẳng `.docx` khi user yêu cầu bản Word ngay.
5. **Xuất `.docx`** theo preset trong `references/dinh-dang-haui.md` khi user yêu cầu.
6. **Chạy checklist** dưới đây trước khi báo xong.

### Thời điểm viết báo cáo

**Báo cáo viết SAU CÙNG, không viết dần từng chương trong lúc code.** Trong lúc code, bằng chứng được tích lũy ở `docs/bao-cao/nhat-ky-tien-do.md` (số liệu, quyết định kỹ thuật, kết quả test, lỗi đã gặp) và `docs/bao-cao/hinh-anh/`.

Cuối kỳ, sản phẩm giao là **một bộ báo cáo** gồm:
1. File báo cáo (Markdown → `.docx`) theo khung 3 chương.
2. Thư mục ảnh `docs/bao-cao/hinh-anh/` (sơ đồ + ảnh chụp giao diện).
3. Tài liệu **test plan / test case** cho mục 3.4 — tổng hợp từ nhật ký + `backend/src/test/`.

Khi dựng báo cáo, tra bảng **"Map chức năng → mục báo cáo"** ở cuối `references/cau-truc-bao-cao.md` để biết mỗi chức năng đã code cần đưa vào mục nào.

## Checklist trước khi giao bản báo cáo

- [ ] Trang bìa đúng format HaUI, thông tin SV/CBHD khớp phiếu giao đề tài
- [ ] Có đủ: Lời cảm ơn, Mục lục, Danh mục hình, Danh mục bảng, Danh mục từ viết tắt, Mở đầu (6 mục)
- [ ] Đúng khung 3 chương; Mở đầu §2 bám nguyên 4 mục tiêu của phiếu
- [ ] Mỗi chương có dẫn nhập đầu chương + tóm tắt/chuyển tiếp cuối chương
- [ ] Mọi hình có caption "Hình x.y", mọi bảng có caption "Bảng x.y", đều được tham chiếu trong văn bản
- [ ] Font Times New Roman 13, line 1.5, lề trái 3cm / phải 2cm / trên 2cm / dưới 2cm, A4
- [ ] Số trang: front matter i, ii, iii; thân bài 1, 2, 3
- [ ] Mục lục + danh mục hình/bảng đã update field trong Word (số trang khớp)
- [ ] **Mục 3.5 và 3.6 có số liệu thật đã đo** (hoặc ghi rõ chưa đo, không bỏ trống lặng lẽ)
- [ ] Tài liệu tham khảo đánh số `[1], [2]…` và đều được trích dẫn trong bài
- [ ] Không bịa số liệu; nội dung khớp `docs/` và mã nguồn thực tế
- [ ] Không lẫn nội dung báo cáo mẫu (Next.js / RabbitMQ / microservices) — xem bảng khác biệt cuối `references/cau-truc-bao-cao.md`

## Sự thật cốt lõi của đề tài (dùng để tự kiểm)

- **4 trụ cột theo phiếu giao đề tài**: (1) web Quiz/Trivia hoàn chỉnh + **phòng đấu multiplayer real-time** độ trễ thấp; (2) **Generative AI qua RAG** — trợ lý học tập + tự sinh cấu trúc đề thi từ học liệu; (3) **Neo4j** phân tích hành vi → gợi ý quiz & lộ trình học cá nhân hóa; (4) **kiểm thử hiệu năng chịu tải real-time + độ chính xác AI**. Bốn cái này phải nổi bật trong báo cáo.
- **Stack**: Java 21 + Spring Boot 3.5, React 19 + Vite 8 + TypeScript + Ant Design v6 + Tailwind v4, PostgreSQL 16 + pgvector, Neo4j 5, Redis (cache/quota/trạng thái phòng/Pub-Sub), Spring WebSocket (STOMP), SSE cho chatbot, Apache Tika, Flyway, Resilience4j, Docker Compose.
- **LLM**: Google Gemini (`gemini-2.5-flash` / `gemini-2.5-pro`) là chính → **xAI Grok dự phòng**, đi qua `AiOrchestrator` tự viết bằng `WebClient` (KHÔNG dùng Spring AI, KHÔNG LangChain4j).
- **4 tác nhân**: Guest, Learner, Creator, Admin (một user có thể vừa Learner vừa Creator).
- **Kiến trúc**: monolith phân lớp Controller→Service→Repository, feature-based package `com.datn.quizai`.
- **Thời gian thực hiện**: 20/07/2026 – 20/09/2026; CBHD ThS. Nguyễn Đức Lưu.
