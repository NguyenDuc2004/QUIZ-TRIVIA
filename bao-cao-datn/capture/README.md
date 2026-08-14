# Chụp ảnh màn hình sản phẩm (Hình 3.2–3.10)

Tự động đăng nhập 3 vai trò và chụp các màn hình chính vào `../assets/hinh-3.X.png`.
`build.js` sẽ tự chèn ảnh khi build lại báo cáo.

## Điều kiện

1. **Database** chạy: `docker compose up -d postgres` (từ thư mục gốc dự án).
2. **Backend** chạy ở `:8080` với `.env` đã cấu hình (`ADMIN_INITIAL_*`, `GEMINI_API_KEY`):
   ```bash
   cd backend && mvn spring-boot:run
   ```
3. **Frontend** chạy ở `:5173`:
   ```bash
   cd frontend && npm run dev
   ```
4. Cần ít nhất một **Môn học** trong dữ liệu nền. Nếu chưa có: đăng nhập Admin → `/admin/master-data`
   tạo 1 Khoa + 1 Môn học, rồi chạy seed.

## Chạy

```bash
cd bao-cao/capture

# 0) Cài Playwright (một lần) — chưa có sẵn trong dự án
npm install
npx playwright install chromium

# 1) Seed dữ liệu demo (tạo gv.demo + sv.demo + lớp + câu hỏi + kỳ thi)
node seed.mjs        # hoặc: npm run seed

# 2) Chụp ảnh
node capture.mjs     # hoặc: npm run capture

# 3) Build lại báo cáo để chèn ảnh
cd ../build && node build.js
```

## Màn hình & mức tự động hóa

| Hình | Màn | Tự động? |
|------|-----|----------|
| 3.2 | Đăng nhập | ✅ tự chụp |
| 3.3 | Ngân hàng câu hỏi (GV) | ✅ tự chụp |
| 3.5 | Dashboard + quản lý người dùng (Admin) | ✅ tự chụp |
| 3.7 | Luyện tập / ôn tập (SV) | ✅ tự chụp |
| 3.4 | Làm bài thi + giám sát | ⚠️ best-effort (cần consent + webcam) |
| 3.6 | Trợ lý AI / thống kê chất lượng | ⚠️ best-effort |
| 3.8 | Sinh đề RAG | ⚠️ cần tài liệu đã embedding + GEMINI |
| 3.9 | Dòng thời gian vi phạm | ⚠️ cần lượt thi có sự kiện giám sát |
| 3.10 | Ảnh report test | ❌ chụp thủ công terminal/HTML report |

Các màn ⚠️/❌: nếu ảnh tự chụp ra trống hoặc lỗi, **chụp thủ công** (thao tác thật trong app cho data
phong phú hơn) rồi lưu đè vào `assets/hinh-3.X.png`. Ảnh thật từ thao tác tay thường đẹp hơn cho báo cáo.

> Mật khẩu tài khoản demo sau seed: `Demo@12345` (xem `seed-output.json`).
