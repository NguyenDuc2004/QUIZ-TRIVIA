---
name: spring-feature
description: Dùng khi tạo mới một tính năng backend Spring Boot trong dự án (module có Controller/Service/Repository/DTO/Entity). Đảm bảo phân lớp, đặt tên và cấu trúc package đúng chuẩn dự án.
---

# Tạo tính năng backend Spring Boot

Áp dụng khi thêm một module/tính năng vào `com.datn.quizai`.

## Quy trình
1. **Đọc** `docs/architecture.md` (cấu trúc package) + `docs/conventions.md` + file feature trong `docs/features/`.
2. **Tạo package** theo tên tính năng: `com.datn.quizai.<feature>`.
3. **Sinh các lớp theo phân lớp:**
   - `Entity` — kế thừa `BaseEntity` (id UUID, createdAt, updatedAt). Không chứa logic nghiệp vụ.
   - `Repository` — `interface extends JpaRepository<Entity, UUID>`.
   - `Dto/Request/Response` — `record`, tách khỏi Entity. Annotate validation trên Request.
   - `Service` — chứa logic; inject qua constructor; `@Transactional` khi cần.
   - `Controller` — `@RestController`, prefix `/api/v1/...`; chỉ nhận/trả DTO; `@Valid`; `@PreAuthorize` cho quyền.
4. **Ánh xạ DTO↔Entity** bằng mapper thủ công hoặc MapStruct (nhất quán toàn dự án).
5. **Nếu đổi schema** → tạo Flyway migration (dùng skill `flyway-migration`).
6. **Viết test:** unit cho Service (Mockito), integration cho Controller/Repository (Testcontainers).

## Checklist
- [ ] Controller không chứa logic nghiệp vụ.
- [ ] Không expose Entity ra API.
- [ ] Có validation trên request DTO.
- [ ] Có `@PreAuthorize`/kiểm tra quyền sở hữu nếu cần.
- [ ] Có exception xử lý qua `@RestControllerAdvice`.
- [ ] Có test tương ứng.

## Chống mẫu (tránh)
- Nhồi logic vào controller. Trả về Entity trực tiếp. Field injection `@Autowired`. Bắt `Exception` chung nuốt lỗi.
