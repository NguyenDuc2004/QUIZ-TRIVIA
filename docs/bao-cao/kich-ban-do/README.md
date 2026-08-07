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
