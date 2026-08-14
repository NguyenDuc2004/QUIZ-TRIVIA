# 01 — Xác thực & Phân quyền

**Ưu tiên:** [M] Must

## Mục tiêu
Cho phép người dùng đăng ký, đăng nhập an toàn và phân quyền truy cập theo vai trò (RBAC).

## Use case
- Guest đăng ký tài khoản, đăng nhập.
- Người dùng đổi mật khẩu, khôi phục khi quên.
- Hệ thống phân quyền Learner / Creator / Admin.

## Yêu cầu chức năng
- **FR-1** [M] ✅ Đăng ký bằng email + mật khẩu (email chuẩn hóa chữ thường, băm BCrypt). Xác thực email: chưa làm (tùy chọn).
- **FR-2** [M] ✅ Đăng nhập/đăng xuất; Access Token (JWT HS256, 15 phút) + Refresh Token (Redis, 14 ngày, có rotation).
- **FR-3** [S] ✅ Đăng nhập bằng **Google** (Google Identity Services, luồng ID token).
- **FR-4** [M] ✅ Quên/đặt lại mật khẩu bằng **mã OTP 6 chữ số gửi qua email** (Gmail SMTP + App Password).

### Đăng nhập Google — vì sao chọn luồng ID token

Có hai cách nối Google: **luồng chuyển hướng phía máy chủ** (Authorization Code, cần Client Secret,
cần redirect URI khớp tuyệt đối) và **luồng ID token** (frontend nhận token từ Google, gửi cho
backend xác minh chữ ký). Dự án dùng cách thứ hai: không phải giữ Client Secret, không phải khai báo
lại redirect URI mỗi lần đổi tên miền, và backend vẫn là bên duy nhất quyết định "người này là ai" —
frontend chỉ chuyển tiếp một mẩu token nó không tự đọc.

`GoogleIdTokenVerifier` của thư viện chính chủ lo tải khoá công khai, kiểm chữ ký RS256, `iss`, hạn
dùng, và quan trọng nhất là **`aud` phải bằng Client ID của ứng dụng này** — bỏ bước đó thì một
token Google hợp lệ cấp cho ứng dụng khác cũng đăng nhập vào đây được.

| Tình huống | Xử lý |
|---|---|
| Đã liên kết (khớp `google_id`) | Đăng nhập luôn, đồng bộ lại ảnh đại diện |
| Email đã có tài khoản mật khẩu | **Liên kết** Google vào tài khoản đó, giữ nguyên mật khẩu và tên hiển thị cũ — người dùng dùng được cả hai cách |
| Hoàn toàn mới | Tạo tài khoản **không mật khẩu**, vai trò **LEARNER** (không cho chọn vai trò, cùng lý do đăng ký thường bị hạ vai trò ADMIN) |

Chỉ liên kết theo email khi Google báo **`email_verified`**; nếu không, ai tạo tài khoản Google mang
email của người khác cũng chiếm được tài khoản của họ.

Khoá liên kết là `sub` của Google chứ **không phải email** — `sub` không đổi kể cả khi người dùng
đổi địa chỉ Gmail.

Tài khoản chỉ-Google không có "mật khẩu hiện tại" để đối chiếu, nên `change-password` trả **400** kèm
hướng dẫn dùng **Quên mật khẩu** để đặt mật khẩu đầu tiên — đường đó chạy được vì OTP gửi về chính
hòm thư Google đã xác minh.

### Quên mật khẩu qua OTP — bốn lớp bảo vệ

| Chặn kiểu tấn công gì | Cách làm |
|---|---|
| Dò danh sách người dùng | `forgot-password` **luôn trả 204**, dù email có tài khoản hay không |
| Đọc trộm Redis | OTP lưu **dạng băm BCrypt**, không lưu thô |
| Dò 6 chữ số | Sai quá **5 lần** thì huỷ mã, bắt xin lại |
| Bơm email vào hòm thư người khác | Giãn cách **60 giây** giữa hai lần xin mã (429) |

Thêm: mã sống 10 phút, chỉ dùng **một lần**, và đặt lại mật khẩu xong thì **thu hồi phiên trên mọi
thiết bị** — người vừa lấy lại tài khoản cần chắc kẻ chiếm dụng bị đá ra.

`reset-password` xác minh mã **trước** khi tra người dùng: làm ngược lại thì thời gian phản hồi giữa
"email không tồn tại" và "mã sai" khác nhau, đủ để dò email qua độ trễ.
- **FR-5** [M] ✅ Quản lý hồ sơ: `PUT /users/me` (tên, avatar) + `POST /auth/change-password`.
- **FR-6** [M] ✅ Phân quyền theo vai trò: enum LEARNER/CREATOR/ADMIN trong token, `@EnableMethodSecurity` cho `@PreAuthorize`; tự đăng ký ADMIN bị hạ xuống LEARNER.

## Luồng xử lý (đăng nhập)
1. Người dùng gửi email + mật khẩu.
2. Server xác minh (BCrypt), cấp access token (15 phút) + refresh token.
3. Client lưu token, gửi kèm `Authorization: Bearer` ở các request sau.
4. Access token hết hạn → dùng refresh token để lấy token mới (rotation).

## API liên quan
Xem [api.md](../api.md) mục 1–2 (`/auth/*`, `/users/me`).

## Dữ liệu liên quan
Bảng `users` — xem [database.md](../database.md) mục 1.2.

## Ghi chú kỹ thuật
- Mật khẩu băm BCrypt; không lưu plaintext.
- Phân quyền controller bằng `@PreAuthorize`.
- Chi tiết bảo mật: [security.md](../security.md).

## Quy tắc truy cập cho Guest (chưa đăng nhập)

`SecurityConfig` chỉ `permitAll` đúng các đường dẫn sau, **mọi thứ còn lại `authenticated()`**:

```
POST /api/v1/auth/register, /login, /refresh, /forgot-password, /reset-password
GET  /api/v1/quizzes, /api/v1/quizzes/{id}      (chỉ bản ghi visibility = public)
GET  /v3/api-docs/**, /swagger-ui/**            (tài liệu API, môi trường dev)
```

- **Guest không được làm bài**: `POST /quizzes/{id}/attempts` và toàn bộ `/attempts/**` yêu cầu đăng nhập → trả **401**.
- `GET /quizzes/{id}` với Guest **không kèm danh sách câu hỏi** (tránh lộ đề); chỉ trả tiêu đề, mô tả, danh mục, độ khó, số câu, thời lượng.
- Quiz `visibility = private` với Guest trả **404** (không phải 403) để không lộ sự tồn tại của tài nguyên.
- WebSocket `/ws`: xác thực JWT ngay tại handshake, Guest bị từ chối kết nối.

## Hết phiên ở phía client: ba lỗi trong axios interceptor

Phát hiện khi làm features/09 — giao diện **trông như vẫn đăng nhập** trong khi mọi API cần quyền đều
trả 401. Cả ba lỗi đều không làm hỏng build và không ca test nào bắt được.

### 1. Nhánh "không có refresh token" lặng lẽ bỏ qua

Interceptor bản đầu chỉ xử lý nhánh *làm mới thất bại*. Trường hợp **không có** refresh token thì
`shouldRefresh` bằng `false` và nó `reject` trong im lặng — không xoá phiên, không điều hướng:

```
token chết + user vẫn còn trong localStorage
  → header hiện tên người dùng như đang đăng nhập
  → GET /quizzes vẫn tải được (API công khai, không cần token!)
  → mọi API cần quyền 401, khu nào cần quyền thì trống trơn
```

Người dùng không có cách nào biết mình cần đăng nhập lại. Cái làm nó khó thấy là **API công khai vẫn
chạy** nên trang trông vẫn sống. Nay **mọi** lối 401 không cứu được đều đi qua `endSession()`.

### 2. Nhiều request cùng gặp 401 → mỗi request tự đi làm mới

Backend **luân chuyển** refresh token (`refreshTokenService.rotate`), nên lượt làm mới đầu tiên làm
token cũ mất hiệu lực ngay. Một trang mở ra thường bắn vài request cần quyền cùng lúc:

```
3 request 401 cùng lúc → 3 lượt refresh song song
  lượt 1: đổi được token mới, token cũ chết
  lượt 2, 3: cầm token đã chết → thất bại → endSession()
  ⇒ đẩy người dùng ra trang đăng nhập DÙ PHIÊN VẪN CÒN CỨU ĐƯỢC
```

Sửa bằng một biến `refreshInFlight` giữ lượt đang chạy: mọi request cùng chờ đúng một lượt.

### 3. Xoá token nhưng không xoá `user`, và không nói vì sao

`tokenStorage.clear()` chỉ xoá hai khoá token; `user` do `persist` của Zustand lưu ở khoá
`quizai-auth` vẫn còn. Nay có `clearPersistedSession()` xoá cả ba — xoá **thẳng khoá localStorage**
chứ không chỉ gọi `clearSession()`, vì `persist` ghi xuống đĩa không đồng bộ mà ngay sau đó là một
lần điều hướng cứng, nên bản ghi có thể chưa kịp xuống.

Và điều hướng kèm `?expired=1` để trang đăng nhập nói rõ *"Phiên đăng nhập đã hết"*. Bị ném ra trang
đăng nhập không rõ vì sao thì người dùng tưởng hệ thống lỗi.

> Câu thông báo ghi *"những câu bạn đã trả lời vẫn được lưu"* — đúng, vì mỗi câu được gửi lên server
> ngay khi chọn (hoặc khi rời ô với câu tự luận). **Không** hứa "không mất gì": chữ đang gõ mà chưa
> rời ô thì vẫn mất.

Riêng các endpoint trong `NO_RETRY_PATHS` (`/auth/login`, `/register`, `/refresh`, `/logout`) trả 401
là chuyện bình thường — sai mật khẩu, refresh token chết — nên để form tự hiện lỗi, không đá người
dùng ra khỏi trang đang thao tác.

### 4. `ProtectedRoute` coi "không có access token" là "chưa đăng nhập"

Lỗi nặng nhất trong bốn, và nó **không nằm trong interceptor** — nên sửa xong ba lỗi trên vẫn còn.

`useIsAuthenticated()` xét `Boolean(user && tokenStorage.getAccess())`. Nhưng access token sống 15
phút, nên **sự tồn tại của nó chưa bao giờ là bằng chứng phiên còn sống**: nó có thể đã hết hạn mà
vẫn nằm nguyên trong localStorage. Thứ quyết định phiên còn hay hết là **refresh token**.

Cái sai đó tạo ra một bất đối xứng vô lý cho *cùng một* trạng thái phiên "cần làm mới token":

| Access token | Kết quả |
|---|---|
| hết hạn nhưng **còn** | coi là đã đăng nhập → trang hiện ra → interceptor làm mới → chạy bình thường ✅ |
| **không còn** | coi là chưa đăng nhập → `ProtectedRoute` đẩy về /login **trước khi** có request nào kịp làm mới ❌ |

Phiên còn cứu được, xử lý hai kiểu khác nhau chỉ vì cái token chết có tình cờ còn trong localStorage
hay không. Nay xét `access || refresh`.

Refresh token *chết* (khác *không có*) vẫn cho qua — và đúng như vậy: lời gọi API đầu tiên sẽ 401,
interceptor thử làm mới, thất bại rồi kết thúc phiên **kèm thông báo**. Chặn ở tầng giao diện thì
người dùng bị đẩy đi mà không ai nói vì sao.

### Thứ tự xoá phiên: một cuộc đua nhỏ nhưng làm mất hẳn thông báo

`endSession()` **không** được xoá state React trước khi điều hướng cứng. Zustand cập nhật đồng bộ →
React kịp render lại → `ProtectedRoute` thấy "chưa đăng nhập" và tự `Navigate` sang `/login` **trần**,
cướp mất `?expired=1`. Người dùng lại về đúng tình trạng bị đẩy ra mà không biết vì sao.

Nên tách hai hàm: `clearStoredSession()` chỉ xoá localStorage (dùng ngay trước điều hướng cứng — trang
mới nạp lại từ localStorage nên thế là đủ), còn `clearPersistedSession()` xoá cả state, dùng khi
**không** điều hướng (đã đang ở trang đăng nhập).

## Cache TanStack Query sống qua hai phiên đăng nhập → rò dữ liệu giữa các tài khoản

Phát hiện 13/08/2026 khi đăng nhập lần lượt hai tài khoản trên cùng trình duyệt: **đăng nhập tài khoản
A nhưng thấy lịch sử chat của tài khoản B**.

Backend không sai — `findByUserIdOrderByUpdatedAtDesc(userId)` và `findOwned(id, userId)` đều lọc theo
người gọi. Lỗi nằm ở client, và cơ chế của nó là:

```
đăng xuất  → clearSession() + navigate('/login')      ← điều hướng phía CLIENT
đăng nhập  → setSession(người mới) + navigate('/')     ← cũng phía client
```

Không có lần nạp lại trang nào ở giữa, nên `QueryClient` (tạo một lần ở `main.tsx`) **sống nguyên qua
cả hai phiên** cùng toàn bộ dữ liệu đã tải. Thêm `staleTime: 30_000` thì dữ liệu người trước còn được
coi là *tươi*, nên các trang hiện nó ra ngay mà **không gọi lại API** — người dùng mới không có cách
nào biết mình đang xem dữ liệu người khác.

Rò không giới hạn ở lịch sử chat: **mọi** dữ liệu đi qua cache đều rò — lượt làm bài, tiến độ học,
quiz của tôi, ngân hàng câu hỏi, học liệu.

Nay `queryClient.clear()` chạy ở **cả bốn** lối đổi danh tính: `useLogin`, `useGoogleLogin`,
`useRegister`, `useLogout`. Xoá ở cả lối vào và lối ra vì hai lối không bao hàm nhau — người dùng có
thể mở thẳng `/login` mà chưa từng bấm đăng xuất. Ở lối vào, xoá **trước** `setSession()`: không để tồn
tại khoảnh khắc nào danh tính đã là người mới trong khi cache vẫn là dữ liệu người cũ.

`endSession()` trong interceptor không cần xoá thêm: nhánh chính điều hướng cứng bằng
`window.location.assign` nên cả cache mất theo, còn nhánh "đang ở /login" thì trang đăng nhập không
hiển thị dữ liệu người dùng nào, và lượt đăng nhập kế tiếp sẽ xoá.

> **Đã có ca test hồi quy** — `src/features/auth/hooks/useAuthMutations.test.tsx`, ba ca: đăng nhập xoá
> cache, đăng xuất xoá cache, và **xoá đúng trước khi đặt phiên mới** (ca thứ ba kiểm *thứ tự*, vì nếu
> `setSession` chạy trước thì tồn tại một khoảnh khắc component đã thấy người dùng mới nhưng đọc được
> dữ liệu người cũ). Ba ca kiểm ở tầng hook chứ không tầng giao diện: đây là lỗi của **vòng đời cache**,
> không của một trang cụ thể — kiểm một trang chỉ chứng minh trang đó sạch, còn cache là thứ mọi trang
> dùng chung.
>
> Đã thử làm chúng đỏ để chắc chúng có tác dụng: bỏ `queryClient.clear()` khỏi `useLogin`/`useLogout` thì
> **cả 3 ca đỏ**, khôi phục thì xanh lại.

> Bài học: **xoá phiên không chỉ là xoá token.** Phải xoá mọi thứ được lưu theo danh tính người dùng,
> mà trong ứng dụng một trang thì cache dữ liệu là chỗ dễ quên nhất — nó không nằm trong localStorage
> để lộ ra khi dọn, và nó biến mất mỗi lần F5 nên khi dò lỗi bằng tay rất dễ tưởng là không có vấn đề.
