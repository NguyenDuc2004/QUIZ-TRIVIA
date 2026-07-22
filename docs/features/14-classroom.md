# 14 — Lớp học / Nhóm (Classroom & Assignment)

**Ưu tiên:** [S] Should · **Tận dụng:** quiz, attempt, analytics sẵn có

## Mục tiêu
Biến ứng dụng thành công cụ giáo dục thật: Creator (giáo viên) tạo lớp, mời học sinh, **giao bài (assignment)** có hạn nộp, và theo dõi tiến độ cả lớp.

## Use case
- Giáo viên tạo lớp, chia sẻ mã lớp (class code) để học sinh tham gia.
- Giáo viên giao một quiz làm bài tập, đặt hạn nộp.
- Học sinh làm bài tập được giao; giáo viên xem kết quả toàn lớp.

## Yêu cầu chức năng
- **FR-54** [S] CRUD lớp học; tham gia lớp bằng **class code**; quản lý thành viên.
- **FR-55** [S] Giao **assignment** (gắn quiz cho lớp) với thời gian mở/đóng, hạn nộp.
- **FR-56** [S] Học sinh xem danh sách bài được giao & trạng thái (chưa làm/đã nộp/quá hạn).
- **FR-57** [S] Giáo viên xem **bảng theo dõi lớp:** ai đã nộp, điểm, tỉ lệ hoàn thành, câu sai nhiều.
- **FR-58** [C] Xuất bảng điểm lớp (CSV/PDF).
- **FR-59** [C] Vai trò trong lớp: chủ nhiệm (owner) / trợ giảng (co-teacher).

## Luồng xử lý
```
Giáo viên tạo lớp → class_code
Học sinh join bằng class_code → classroom_members
Giáo viên tạo assignment(quiz, open_at, due_at) cho lớp
Học sinh làm → quiz_attempt gắn assignment_id
Giáo viên xem tổng hợp từ attempts theo assignment (dùng analytics)
```

## API liên quan
```
GET/POST/PUT/DELETE /api/v1/classrooms                Quản lý lớp
POST   /api/v1/classrooms/{code}/join                 Tham gia lớp
GET    /api/v1/classrooms/{id}/members                Thành viên
POST   /api/v1/classrooms/{id}/assignments            Giao bài
GET    /api/v1/classrooms/{id}/assignments            Danh sách bài giao
GET    /api/v1/assignments/{id}/results               Kết quả toàn lớp (giáo viên)
GET    /api/v1/me/assignments                          Bài được giao cho tôi (học sinh)
```

## Dữ liệu liên quan (bổ sung PostgreSQL)
- `classrooms(id, owner_id, name, class_code unique, description, created_at)`
- `classroom_members(id, classroom_id, user_id, role: student/co_teacher, joined_at)`
- `assignments(id, classroom_id, quiz_id, title, open_at, due_at, created_at)`
- `quiz_attempts.assignment_id` (FK nullable — gắn attempt vào bài giao)

## Ghi chú kỹ thuật
- Kiểm quyền: chỉ owner/co_teacher xem kết quả lớp; học sinh chỉ thấy bài & kết quả của mình.
- Trạng thái nộp tính từ `quiz_attempts` + `due_at` (đúng hạn/quá hạn).
- Tái dùng [features/09-analytics.md](09-analytics.md) cho thống kê lớp.
- Có thể kết hợp thông báo ([features/16](16-notifications.md)) nhắc hạn nộp.
