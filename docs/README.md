# Tài liệu dự án — Ứng dụng Quiz/Trivia tích hợp AI

> **Đề tài:** Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo
> **Sinh viên:** Nguyễn Khắc Minh Đức — 2022601585 — 2022DHKTPM01 (K17)
> **GVHD:** Ths. Nguyễn Đức Lưu · **Thời gian:** 20/07/2026 → 20/09/2026

Bộ tài liệu này được tách nhỏ theo chủ đề để dễ đọc, dễ bảo trì và thuận tiện cho việc phát triển có hỗ trợ AI (vibe coding).

## Điều hướng

| Tài liệu | Nội dung |
|----------|----------|
| [overview.md](overview.md) | Giới thiệu, mục tiêu, phạm vi, tác nhân, đối chiếu phiếu giao đề tài |
| [architecture.md](architecture.md) | Kiến trúc tổng thể, phân lớp, cấu trúc package, luồng dữ liệu |
| [tech-stack.md](tech-stack.md) | Công nghệ backend/frontend/hạ tầng và lý do chọn |
| [database.md](database.md) | Thiết kế PostgreSQL + pgvector, Neo4j, Redis |
| [api.md](api.md) | Đặc tả REST API & WebSocket (STOMP) |
| [security.md](security.md) | Xác thực, phân quyền, bảo mật AI |
| [roadmap.md](roadmap.md) | Lộ trình theo tuần, kiểm thử hiệu năng & độ chính xác AI |
| [ke-hoach-tien-do.md](ke-hoach-tien-do.md) | Kế hoạch chi tiết theo từng ngày (20/07 → 20/09) |
| [conventions.md](conventions.md) | Quy ước code, cấu trúc thư mục, **quy trình lát cắt dọc & Definition of Done** |
| [features/](features/) | Đặc tả chi tiết từng tính năng (mỗi file 1 feature) |
| [bao-cao/](bao-cao/) | Nhật ký tiến độ & nội dung báo cáo ĐATN (khung ở skill `viet-bao-cao`) |
| [phieu_giao_de_tai/](phieu_giao_de_tai/) | Phiếu giao đề tài (nguồn gốc của mục tiêu & kết quả dự kiến) |

## Danh sách tính năng

Xem [features/README.md](features/README.md) để có bảng tổng hợp và mức ưu tiên.

## Bốn trụ cột theo phiếu giao đề tài

1. **Multiplayer real-time** — phòng đấu trí nhiều người, độ trễ thấp (Spring WebSocket + Redis).
2. **Generative AI + RAG** — trợ lý học tập & sinh đề từ học liệu.
3. **Neo4j** — CSDL đồ thị phân tích hành vi, gợi ý quiz & lộ trình học cá nhân hóa.
4. **Kiểm thử** — hiệu năng chịu tải real-time & độ chính xác mô hình AI.
