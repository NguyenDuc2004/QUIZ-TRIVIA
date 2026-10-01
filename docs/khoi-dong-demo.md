# Khởi động web bằng tay để demo

Tờ thao tác cho buổi bảo vệ. Làm đúng thứ tự — bước 1 phải xong trước bước 3, lý do ghi ở cuối.

Mở **ba cửa sổ terminal**, mỗi cửa sổ giữ một tiến trình chạy suốt buổi. Đừng đóng cửa sổ nào.

---

## Bước 1 — Nối mạng TRƯỚC KHI chạy backend

Điện thoại bật hotspot → laptop nối vào hotspot đó.

Kiểm địa chỉ laptop vừa nhận:

```powershell
ipconfig | findstr IPv4
```

Mong đợi: `192.168.43.x` (hotspot Android) hoặc `172.20.10.x` (hotspot iPhone).

> Bước này phải xong trước bước 3. Backend dò địa chỉ LAN **một lần lúc khởi động rồi giữ nguyên cả phiên** — đổi mạng sau đó thì mã QR vẫn mang địa chỉ cũ, mà màn hình trông vẫn bình thường nên rất khó phát hiện.

> **Rút dây mạng và tắt mọi kết nối khác.** Backend chọn card mạng đang thật sự đi ra Internet. Nếu laptop vừa cắm dây Ethernet vừa nối hotspot, nó rất dễ chọn nhầm card Ethernet, và mã QR sẽ mang một địa chỉ mà điện thoại không gọi tới được. Chỉ để đúng một kết nối: hotspot.

---

## Bước 2 — Ba dịch vụ dữ liệu

Bật **Docker Desktop** (biểu tượng con cá voi, đợi tới khi hết chữ "starting"), rồi:

```bash
cd D:\DATN
docker compose up -d
```

Kiểm:

```bash
docker compose ps
```

Mong đợi ba dòng `quiz_postgres`, `quiz_neo4j`, `quiz_redis` đều **Up**, riêng postgres có thêm **(healthy)**.

---

## Bước 3 — Backend (cửa sổ 1, để chạy suốt)

```bash
cd D:\DATN\backend
./mvnw spring-boot:run
```

**Chạy ở cổng mặc định 8080, không cần tham số gì thêm.** Trước đây cổng này bị dịch vụ `MTAgentService` của MiniTool ShadowMaker chiếm nên phải lách sang 8081; dịch vụ đó nay đã dừng và chuyển sang khởi động thủ công, nên 8080 trống. Nếu lỡ thấy backend không lên được, kiểm xem dịch vụ ấy có tự bật lại không.

Kiểm (mở terminal khác):

```bash
curl http://localhost:8080/actuator/health
```

Mong đợi: `{"status":"UP"}`

---

## Bước 4 — Frontend (cửa sổ 2, để chạy suốt)

```bash
cd D:\DATN\frontend
npm run dev
```

Mong đợi Vite in ra **hai** địa chỉ:

```
Local:   http://localhost:5173/
Network: http://192.168.43.x:5173/
```

Dòng **Network** là dòng quan trọng — nó chứng tỏ điện thoại gọi tới được. Không thấy dòng này thì điện thoại sẽ không vào được phòng.

Kiểm cho chắc, thay `<ip>` bằng địa chỉ ở bước 1:

```bash
curl -o /dev/null -w "%{http_code}
" http://<ip>:5173
curl -o /dev/null -w "%{http_code}
" "http://<ip>:5173/api/v1/quizzes?size=1"
```

Cả hai phải ra `200`. Dòng thứ hai quan trọng hơn: nó chứng tỏ không chỉ trang tĩnh mà cả đường gọi dữ liệu cũng thông — đúng thứ điện thoại cần.

---

## Bước 5 — Kiểm mã QR trước khi vào phòng thi

Đăng nhập, vào **Phòng đấu**, tạo một phòng, rồi **nhìn kỹ đường dẫn dưới mã QR**.

| Thấy gì | Nghĩa là |
|---|---|
| `http://192.168.43.x:5173/join/...` | Đúng. Quét được. |
| `http://172.20.10.x:5173/join/...` | Đúng. Quét được. |
| `http://localhost:5173/join/...` | **Sai** — backend không dò được mạng. Tắt backend, kiểm lại bước 1, chạy lại bước 3. |
| IP lạ không giống `ipconfig` | **Sai** — backend khởi động trước khi nối hotspot. Tắt backend, chạy lại bước 3. |

Lấy điện thoại thứ hai quét thử một lần **trước khi vào phòng bảo vệ**. Đừng để lần quét đầu tiên diễn ra trước mặt hội đồng.

---

## Tài khoản demo

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Người học | `hs1.demo@quizai.local` | `MatKhau@123` |
| Người học thứ hai | `hs2.demo@quizai.local` | `MatKhau@123` |
| Người tạo nội dung | `gv.demo@quizai.local` | `MatKhau@123` |
| Quản trị | xem `APP_ADMIN_EMAIL` trong `.env` | xem `APP_ADMIN_PASSWORD` |

Khách chưa có tài khoản vẫn vào phòng đấu được bằng mã PIN, miễn là chủ phòng bật *cho khách vào*.

---

## Hai điều đã biết trước, đừng để bị bất ngờ

**Đăng nhập bằng Google không chạy.** Client ID chưa khai báo `http://localhost:5173` trong danh sách origin được phép. Dùng email và mật khẩu. Nếu hội đồng hỏi: đây là cấu hình origin phía Google cho từng địa chỉ triển khai, không phải lỗi mã nguồn.

**Chức năng AI cần Internet, phòng đấu thì không.** Phòng đấu chạy hoàn toàn trong hotspot nên sóng yếu cũng không sao. Riêng sinh đề và trợ lý gọi ra Gemini, nên nếu 4G chập chờn thì demo phần đó sau, hoặc cho xem kết quả đã sinh sẵn.

---

## Phương án hai nếu hotspot hỏng

```bash
cloudflared tunnel --url http://localhost:5173
```

Lấy URL công khai nó in ra, rồi chạy lại backend kèm:

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--app.frontend.base-url=https://<url-hầm>"
```

Mã QR sẽ mang URL công khai, điện thoại vào được kể cả khi dùng 4G riêng.

---

## Tắt sau khi xong

Nhấn `Ctrl+C` ở cửa sổ frontend và backend, rồi:

```bash
cd D:\DATN
docker compose down
```
