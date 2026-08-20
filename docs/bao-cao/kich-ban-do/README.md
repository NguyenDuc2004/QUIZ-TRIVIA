# Kịch bản đo — lưu lại để chạy lại và kiểm chứng

Hai script này sinh ra số liệu trong `../so-lieu-3.5-hieu-nang-realtime.md`. Lưu cùng repo để bất kỳ
ai cũng chạy lại được và đối chiếu — số liệu trong báo cáo mà không tái lập được thì không kiểm chứng
được.

Cần: backend chạy ở `localhost:8080`, Node 20+, chạy từ thư mục `frontend/` (mượn `@stomp/stompjs`
trong `node_modules`).

```bash
cd frontend

# Thang tải: độ trễ phát câu hỏi theo số người trong phòng
node ../docs/bao-cao/kich-ban-do/loadtest_room.mjs 10 30 50 100 150 200

# Tách nghẽn: chỉ nhận câu hỏi, không gửi đáp án
node ../docs/bao-cao/kich-ban-do/loadtest_room.mjs 200 --khong-tra-loi

# Vai trò Redis Pub/Sub — cần backend thứ hai ở cổng 8081:
#   cd backend && ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
node ../docs/bao-cao/kich-ban-do/loadtest_two_instances.mjs 40
```

Hai script đăng ký tài khoản `@example.com` trên CSDL đang chạy. Dọn sau khi đo:

```bash
docker exec -i quiz_postgres psql -U quiz -d quizdb -c "DELETE FROM users WHERE email LIKE '%example.com';"
```

---

## Kiểm chứng cảnh báo live phòng đấu — `kiemchung_canhbao_phongdau.mjs`

Không phải phép **đo** mà là phép **kiểm** (features/12, cảnh báo live): chạy một ván thật với host, một học
sinh và một khách vãng lai, rồi khẳng định 13 điều — trong đó hai điều quan trọng nhất là *cờ đỏ KHÔNG lên kênh
phát chung* và *một câu duy nhất thì chưa gắn cờ*.

```
node ../docs/bao-cao/kich-ban-do/kiemchung_canhbao_phongdau.mjs
```

Dùng `@stomp/stompjs` + SockJS **giống y trình duyệt** nên nó nói đúng giao thức thật, không giả lập. Điểm đồng
bộ là `GET /rooms/{code}/proctoring` chứ không phải một khoảng chờ đoán bừa — lý do ở
[features/12](../../features/12-anti-cheat.md), mục "Một bài học về test".

## Đánh giá độ chính xác AI (mục 3.6) — `danhgia_ai.mjs`

**Phải khởi động backend với số lần thử lại bằng 1**, nếu không chính phép đo sẽ đốt hết hạn mức nó
cần để chạy (một bài chấm hỏng với 4 lần thử tiêu 4 lượt trong hạn mức 20 lượt/ngày):

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.ai.max-attempts-background=1
```

Rồi:

```bash
cd frontend
node ../docs/bao-cao/kich-ban-do/danhgia_ai.mjs
```

Mất khoảng 12 phút — kịch bản cố ý **chờ 70 giây giữa các lượt** vì Gemini miễn phí giới hạn 5
lượt/phút. Tiêu khoảng 10 lượt trong hạn mức ngày.

Bài nào chấm hỏng (`AI_FAILED`) bị **loại khỏi thống kê**, không tính là 0 điểm — gộp "AI chấm 0"
với "AI không chạy" thì con số độ chính xác mất hết ý nghĩa.
