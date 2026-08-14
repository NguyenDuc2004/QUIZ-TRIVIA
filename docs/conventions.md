# Quy ước phát triển (Conventions)

> Tài liệu này giữ code nhất quán khi phát triển có hỗ trợ AI (vibe coding). Đọc trước khi sinh code.

## 1. Backend (Java / Spring Boot)

- **Cấu trúc:** feature-based package (xem [architecture.md](architecture.md)), phân lớp Controller → Service → Repository.
- **Controller** chỉ nhận/trả DTO, không chứa logic nghiệp vụ.
- **DTO** tách khỏi Entity; dùng record cho DTO khi hợp lý. Không expose Entity ra API.
- **Đặt tên:** `XxxController`, `XxxService`, `XxxRepository`, `XxxDto`, `XxxRequest`, `XxxResponse`.
- **Validation:** annotate DTO request bằng Jakarta Bean Validation; validate ở controller.
- **Exception:** ném exception nghiệp vụ, xử lý tập trung ở `@RestControllerAdvice` → response lỗi chuẩn (xem [api.md](api.md) mục 10).
- **Entity:** kế thừa `BaseEntity` (id UUID, createdAt, updatedAt). Không dùng logic nghiệp vụ trong entity.
- **AI:** mọi lời gọi LLM đi qua `AiProvider`/`AiOrchestrator`, không gọi trực tiếp API trong service nghiệp vụ.
- **Async:** tác vụ > vài giây (sinh đề, xử lý học liệu) chạy nền, trả `jobId`.
- **Migration:** mọi thay đổi schema PostgreSQL qua Flyway (`V{n}__mô_tả.sql`), không sửa file migration đã merge.

## 2. Frontend (React / TypeScript)

- **Cấu trúc:** theo feature (`src/features/<feature>/{api,components,hooks,pages}`), `src/shared` cho dùng chung.
- **Data:** dùng TanStack Query cho gọi API; không tự quản lý loading/error thủ công.
- **Type:** định nghĩa type khớp response backend; validate form bằng Zod.
- **Đặt tên:** component `PascalCase`, hook `useXxx`, file component trùng tên component.
- **Không hardcode URL API:** dùng client tập trung (axios instance) + biến môi trường.
- **Giao diện: bắt buộc theo [ui-design-system.md](ui-design-system.md).** Tóm tắt luật cứng:
  - Không hardcode mã màu, `borderRadius`, `boxShadow` trong component — dùng token antd (`shared/theme/antdTheme.ts`) hoặc class Tailwind sinh từ `@theme` (`text-ink`, `border-line`…).
  - Nút hành động chính là **nút đen** (`type="primary"`); tím chỉ dùng cho link và trạng thái active.
  - Trang cho người học → **lưới card**; trang quản lý → **bảng**. Chọn sai kiểu là lỗi giao diện.
  - Mọi trang mới dùng `PageHeader`; danh sách rỗng dùng `EmptyState`. Không tự dựng lại.
  - **Không bịa dữ liệu để cho đẹp** (điểm đánh giá, số lượt học…) — chỉ hiển thị dữ liệu backend thật sự trả về.
  - Cú pháp Tailwind v4: viết `mb-1!` chứ không phải `!mb-1`.
- **Test:** vitest + Testing Library, cấu hình ở `vitest.config.ts` (riêng, không gộp vào `vite.config.ts`
  — xem ghi chú trong file). Đặt file test cạnh thứ nó kiểm: `useAuthMutations.test.tsx` nằm cùng thư mục
  với `useAuthMutations.ts`. Chạy `npm test`.
  - **Ưu tiên kiểm ở tầng hook/hàm hơn tầng giao diện** khi lỗi thuộc về logic dùng chung: kiểm một trang
    chỉ chứng minh trang đó đúng, còn cache và phiên đăng nhập là thứ mọi trang dùng chung.
  - **Viết xong phải thử làm ca test ĐỎ** bằng cách tạm bỏ đúng dòng code nó bảo vệ. Một ca xanh ở cả bản
    đúng và bản lỗi thì không bảo vệ gì cả — đây là bài học từ hai lỗi nặng ngày 13/08.

## 3. Git

- **Branch:** `feature/<tên>`, `fix/<tên>`. Nhánh chính: `main`.
- **Commit (Conventional Commits):** `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.
- Commit nhỏ, thông điệp rõ ràng bằng tiếng Việt hoặc tiếng Anh (nhất quán).

## 4. Cấu hình & bí mật

- Không commit secret/API key. Dùng `.env` (đã gitignore) + `application.yml` đọc từ env.
- Mỗi CSDL (Postgres/Neo4j/Redis) chạy qua Docker Compose khi dev local.

## 5. Thứ tự ưu tiên khi phát triển

Theo [roadmap.md](roadmap.md): nền tảng → quiz → chơi đơn → **multiplayer** → **AI/RAG** → **Neo4j** → kiểm thử. Ưu tiên đủ 4 trụ cột của phiếu trước khi làm tính năng phụ.

## 6. Cấu trúc kho mã (monorepo: 1 BE + 1 FE)

Toàn bộ dự án nằm trong **một repo duy nhất**, gồm **một thư mục backend** và **một thư mục frontend**:

```
DATN/
├── backend/            # Spring Boot (Maven) — toàn bộ BE của mọi tính năng
│   ├── src/main/java/com/datn/quizai/...      # feature-based package (architecture.md §3)
│   ├── src/main/resources/db/migration/       # Flyway V{n}__*.sql
│   └── src/test/java/...                      # test đi kèm từng tính năng
├── frontend/           # React 18 + Vite + TypeScript — toàn bộ FE
│   └── src/features/<feature>/{api,components,hooks,pages}
├── docs/               # tài liệu & báo cáo
├── infra/              # script khởi tạo hạ tầng (postgres init…)
└── docker-compose.yml  # PostgreSQL + Neo4j + Redis
```

- **Không** tạo repo/thư mục riêng cho từng tính năng. Mỗi tính năng chỉ **thêm package vào `backend/`** và **thêm folder vào `frontend/src/features/`**.
- Tên module BE và tên feature FE **đặt trùng nhau** (`quiz` ↔ `features/quiz`) để dễ tra khi viết báo cáo.

## 7. Quy trình làm việc: lát cắt dọc, tuần tự từng tính năng

**Một tính năng = một lát cắt dọc trọn vẹn BE + FE + test.** Làm xong hẳn tính năng này mới sang tính năng khác — không code dở nhiều tính năng song song.

Thứ tự bắt buộc trong mỗi lát cắt:

1. **Migration** — Flyway `V{n}__*.sql` cho bảng/cột của tính năng (skill `flyway-migration`).
2. **Backend** — Entity → Repository → Service → DTO → Controller (skill `spring-feature`, chuẩn REST ở [api.md](api.md)).
3. **Frontend** — `frontend/src/features/<feature>/` : api client → hooks (TanStack Query) → components → pages → gắn route (skill `react-feature`).
4. **Nối FE ↔ BE** — chạy thật, bấm được happy path trên trình duyệt.
5. **Test & chạy test** — viết test theo tầng rồi **chạy cho pass** (skill `backend-testing`): Service (Mockito) → Repository (`@DataJpaTest` + Testcontainers) → Controller (`@WebMvcTest`) → Security/STOMP/SSE nếu tính năng có.
6. **Cập nhật tài liệu & thu bằng chứng** — `docs/bao-cao/nhat-ky-tien-do.md` (tích `[x]`, ghi số liệu + quyết định kỹ thuật) và `docs/ke-hoach-tien-do.md`; lưu ảnh chụp giao diện vào `docs/bao-cao/hinh-anh/`.
   > **Báo cáo ĐATN viết sau cùng**, không viết dần từng chương. Cuối kỳ mới tổng hợp thành một bộ gồm: file báo cáo + thư mục ảnh + tài liệu test plan / test case. Vì vậy trong mỗi lát cắt chỉ cần **ghi đủ bằng chứng** (số liệu, kết quả test, ảnh) để cuối kỳ dựng lại được, đừng dừng lại soạn chương báo cáo.

### Định nghĩa "xong" (Definition of Done) của một tính năng

- [ ] Migration chạy sạch trên DB trống
- [ ] API hoạt động đúng, có trong Swagger, mã lỗi theo [api.md](api.md)
- [ ] UI gọi được API, happy path chạy mượt trên trình duyệt
- [ ] Test đã viết **và đã chạy pass** (không chỉ viết rồi để đó)
- [ ] Phân quyền đúng vai trò (Guest/Learner/Creator/Admin)
- [ ] Nhật ký tiến độ đã cập nhật (kèm số liệu/kết quả test làm bằng chứng cho báo cáo cuối kỳ)
- [ ] Commit theo Conventional Commits (§3)

> Chỉ khi cả 7 mục trên đã ✅ mới được bắt đầu tính năng kế tiếp. Nếu bị chặn, ghi `[!]` vào nhật ký kèm lý do thay vì bỏ dở sang việc khác.
