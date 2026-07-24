---
name: react-feature
description: Dùng khi tạo mới một tính năng frontend React trong dự án — cấu trúc theo feature, gọi API bằng TanStack Query, form với Zod, real-time STOMP/SSE khi cần.
---

# Tạo tính năng frontend React

## Cấu trúc
```
src/features/<feature>/
├── api/          # hàm gọi API + hook query/mutation (TanStack Query)
├── components/   # component UI (PascalCase)
├── hooks/        # useXxx
├── pages/        # trang gắn route
└── types.ts      # type khớp response backend
```
Dùng chung đặt ở `src/shared` (client axios, ui, utils).

## Quy trình
1. Định nghĩa `types.ts` khớp DTO backend (xem `docs/api.md`).
2. Viết hàm API dùng axios instance tập trung (không hardcode URL, đọc từ env).
3. Bọc bằng TanStack Query: `useQuery` (đọc), `useMutation` (ghi) + invalidate cache.
4. Form: React Hook Form + **Zod** schema validate.
5. UI: Ant Design (component) + TailwindCSS (layout/spacing); trạng thái loading/error rõ ràng.
6. **Real-time (nếu là phòng đấu):** `@stomp/stompjs` + SockJS, subscribe `/topic/room/{code}`.
7. **Chatbot:** `EventSource` (SSE) nhận stream token; render dần.

## Xử lý lỗi
- Đọc response lỗi chuẩn (`message`, `status`) và hiển thị thân thiện.
- Với AI (có thể lâu): hiển thị tiến trình job, poll `/ai/jobs/{jobId}`.

## Checklist
- [ ] Type khớp backend.
- [ ] Không hardcode URL/API key.
- [ ] Dùng TanStack Query, không tự quản loading thủ công.
- [ ] Form validate bằng Zod.
- [ ] Responsive & accessible.

## Tham chiếu
`docs/tech-stack.md` mục 3, `docs/conventions.md` mục 2, `docs/api.md`.
