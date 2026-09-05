/* Sinh các sơ đồ UML (use case, sequence, VOPC) bằng PlantUML.
 * Đề tài: Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo.
 *
 * PlantUML render đúng icon UML chuẩn (actor stick, oval use case, boundary/control/entity
 * stereotype icons), khớp phong cách HaUI ĐATN.
 *
 * Phụ thuộc: java + plantuml.jar nằm cạnh file này.
 *
 * Danh mục hình render qua PlantUML:
 *   2.1        Use case tổng quát (4 tác nhân + quan hệ tổng quát hóa)
 *   2.2 - 2.5  Use case theo từng tác nhân: Khách, Người học, Người tạo nội dung, Quản trị viên
 *   2.6 - 2.12 Use case chi tiết cho 7 đặc tả
 *   2.13       Biểu đồ lớp thiết kế tổng thể
 *   2.14/2.15  Sequence + VOPC: Đăng nhập
 *   2.16/2.17  Sequence + VOPC: Quản lý quiz và ngân hàng câu hỏi
 *   2.18/2.19  Sequence + VOPC: Làm bài quiz cá nhân
 *   2.20/2.21  Sequence + VOPC: Tham gia phòng đấu thời gian thực
 *   2.22/2.23  Sequence + VOPC: Sinh đề bằng AI từ học liệu
 *   2.24/2.25  Sequence + VOPC: Hỏi trợ lý học tập
 *   2.26/2.27  Sequence + VOPC: Nhận gợi ý bài thi và lộ trình học
 */
const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");

const BUILD = __dirname;
const OUT = path.join(BUILD, "..", "assets");
const DG = path.join(BUILD, "diagrams");
const JAR = path.join(BUILD, "plantuml.jar");
fs.mkdirSync(OUT, { recursive: true });
fs.mkdirSync(DG, { recursive: true });

// Skin chung: phong cách HaUI/Visual Paradigm legacy — body kem vàng nhạt + viền/đường nét hồng magenta
const SKIN = `
skinparam dpi 150
skinparam defaultFontName "Times New Roman"
skinparam defaultFontSize 13
skinparam shadowing false
skinparam roundCorner 0
skinparam ArrowColor #C99AAB
skinparam ArrowFontColor #202020
skinparam Padding 4
skinparam ParticipantPadding 18
skinparam BoxPadding 8
skinparam SequenceMessageAlign center
skinparam guillemet false

skinparam usecase {
  BackgroundColor #FFFADC
  BorderColor #C99AAB
  FontColor #1a1a1a
}
skinparam actor {
  BackgroundColor #FFFFFF
  BorderColor #404040
  FontColor #1a1a1a
}
skinparam rectangle {
  BackgroundColor #FAFAFA
  BorderColor #808080
}

skinparam sequence {
  ArrowColor #C99AAB
  ArrowThickness 1
  LifeLineBorderColor #C99AAB
  LifeLineBackgroundColor #FFFFFF
  ParticipantBackgroundColor #FFFADC
  ParticipantBorderColor #C99AAB
  ParticipantFontColor #1a1a1a
  ParticipantFontSize 12
  ActorBackgroundColor #FFFFFF
  ActorBorderColor #404040
  GroupBorderColor #C99AAB
  GroupBackgroundColor #FFF5F8
  BoxBorderColor #C99AAB
}

skinparam class {
  BackgroundColor #FFFADC
  BorderColor #C99AAB
  HeaderBackgroundColor #FFEFC4
  AttributeFontColor #1a1a1a
  FontColor #1a1a1a
  ArrowColor #C99AAB
  AttributeFontSize 12
  FontSize 13
}
skinparam stereotype {
  CBackgroundColor #FFF1A8
  CBorderColor #C99AAB
  EBackgroundColor #FFF1A8
  EBorderColor #C99AAB
  ABackgroundColor #FFF1A8
  ABorderColor #C99AAB
}
`;

const D = {};

/* ============================================================
 * 2.1 — USE CASE TỔNG QUÁT
 * ============================================================ */
D["2.1"] = `@startuml
${SKIN}
left to right direction
skinparam nodesep 22
skinparam ranksep 60

actor "Khách" as G
actor "Người học" as L
actor "Người tạo nội dung" as C
actor "Quản trị viên" as A

rectangle "HỆ THỐNG QUIZ/TRIVIA TÍCH HỢP AI" {
  usecase "Xem quiz công khai" as U1
  usecase "Đăng ký, đăng nhập" as U2
  usecase "Làm bài quiz cá nhân" as U3
  usecase "Tham gia phòng đấu\\nthời gian thực" as U4
  usecase "Hỏi trợ lý học tập\\n(RAG)" as U5
  usecase "Nhận gợi ý và\\nlộ trình học" as U6
  usecase "Xem tiến độ,\\nlịch sử làm bài" as U7
  usecase "Quản lý quiz và\\nngân hàng câu hỏi" as U8
  usecase "Nạp và chia sẻ\\nhọc liệu" as U9
  usecase "Sinh đề bằng AI" as U10
  usecase "Duyệt câu hỏi\\ndo AI sinh" as U11
  usecase "Mở và điều khiển\\nphòng đấu" as U12
  usecase "Chấm tay câu tự luận" as U13
  usecase "Quản lý người dùng" as U14
  usecase "Giám sát chi phí AI" as U15
}

G --> U1
G --> U2
L --> U3
L --> U4
L --> U5
L --> U6
L --> U7
C --> U8
C --> U9
C --> U10
C --> U11
C --> U12
C --> U13
A --> U14
A --> U15

C -up-|> L
A -up-|> C

note bottom of L
  Creator có toàn bộ quyền của Learner,
  Admin có toàn bộ quyền của Creator
end note
@enduml`;

/* ============================================================
 * 2.2 - 2.5 — USE CASE THEO TỪNG TÁC NHÂN
 * ============================================================ */
D["2.2"] = `@startuml
${SKIN}
left to right direction
actor "Khách" as G
rectangle "Chức năng dành cho khách chưa đăng nhập" {
  usecase "Xem danh sách quiz công khai" as U1
  usecase "Xem thông tin giới thiệu một quiz\\n(không xem được nội dung câu hỏi)" as U2
  usecase "Tìm kiếm và lọc quiz" as U3
  usecase "Đăng ký tài khoản" as U4
  usecase "Đăng nhập" as U5
  usecase "Đăng nhập bằng Google" as U6
  usecase "Quên mật khẩu (OTP qua email)" as U7
  usecase "Vào phòng đấu bằng mã PIN\\nvới tư cách khách" as U8
}
G --> U1
G --> U2
G --> U3
G --> U4
G --> U5
G --> U6
G --> U7
G --> U8
U6 .up.> U5 : <<extend>>
note right of U8
  Chỉ khi chủ phòng bật
  tùy chọn cho khách
end note
@enduml`;

D["2.3"] = `@startuml
${SKIN}
left to right direction
actor "Người học" as L
rectangle "Chức năng dành cho người học" {
  usecase "Làm bài quiz cá nhân" as U1
  usecase "Nộp bài và xem kết quả" as U2
  usecase "Xem lời giải thích đáp án" as U3
  usecase "Xem lịch sử làm bài" as U4
  usecase "Xem tiến độ học theo chủ đề" as U5
  usecase "Tham gia phòng đấu\\nthời gian thực" as U6
  usecase "Trả lời trong phòng đấu" as U7
  usecase "Xem bảng xếp hạng trực tiếp" as U8
  usecase "Hỏi trợ lý học tập" as U9
  usecase "Xem hội thoại đã lưu" as U10
  usecase "Nhận gợi ý quiz" as U11
  usecase "Xem lộ trình học" as U12
  usecase "Quản lý hồ sơ, đổi mật khẩu" as U13
}
L --> U1
L --> U4
L --> U5
L --> U6
L --> U9
L --> U10
L --> U11
L --> U12
L --> U13
U1 ..> U2 : <<include>>
U2 ..> U3 : <<include>>
U6 ..> U7 : <<include>>
U7 ..> U8 : <<include>>
@enduml`;

D["2.4"] = `@startuml
${SKIN}
left to right direction
actor "Người tạo nội dung" as C
rectangle "Chức năng dành cho người tạo nội dung" {
  usecase "Quản lý quiz\\n(thêm, sửa, xóa)" as U1
  usecase "Quản lý ngân hàng câu hỏi\\n(5 loại câu hỏi)" as U2
  usecase "Tải ảnh bìa quiz" as U3
  usecase "Nạp học liệu\\n(PDF/DOCX/TXT/dán tay)" as U4
  usecase "Chia sẻ học liệu\\ncho người học" as U5
  usecase "Sinh đề bằng AI từ học liệu" as U6
  usecase "Duyệt câu hỏi do AI sinh" as U7
  usecase "Mở phòng đấu" as U8
  usecase "Điều khiển ván đấu" as U9
  usecase "Chấm tay câu tự luận" as U10
  usecase "Xem thống kê quiz của mình" as U11
}
C --> U1
C --> U2
C --> U4
C --> U5
C --> U6
C --> U8
C --> U10
C --> U11
U1 ..> U3 : <<include>>
U6 ..> U7 : <<include>>
U8 ..> U9 : <<include>>
note right of U6
  Yêu cầu học liệu đã ở
  trạng thái sẵn sàng
end note
@enduml`;

D["2.5"] = `@startuml
${SKIN}
left to right direction
actor "Quản trị viên" as A
rectangle "Chức năng dành cho quản trị viên" {
  usecase "Quản lý tài khoản người dùng" as U1
  usecase "Đổi vai trò người dùng" as U2
  usecase "Quản lý toàn bộ nội dung\\n(quiz, câu hỏi)" as U3
  usecase "Quản lý danh mục" as U4
  usecase "Cấu hình nhà cung cấp AI\\n(thứ tự Gemini → Groq)" as U5
  usecase "Giám sát nhật ký gọi AI" as U6
  usecase "Giám sát chi phí và số token" as U7
}
A --> U1
A --> U3
A --> U4
A --> U5
A --> U6
U1 ..> U2 : <<include>>
U6 ..> U7 : <<include>>
@enduml`;

/* ============================================================
 * 2.6 - 2.12 — USE CASE CHI TIẾT CHO 7 ĐẶC TẢ
 * ============================================================ */
D["2.6"] = `@startuml
${SKIN}
left to right direction
actor "Khách" as G
rectangle "UC-01 — Đăng nhập" {
  usecase "Đăng nhập" as U1
  usecase "Xác thực email và mật khẩu" as U2
  usecase "Cấp access token\\nvà refresh token" as U3
  usecase "Đăng nhập bằng Google\\n(xác minh ID token)" as U4
  usecase "Đặt lại mật khẩu bằng OTP" as U5
}
G --> U1
U1 ..> U2 : <<include>>
U1 ..> U3 : <<include>>
U4 .up.> U1 : <<extend>>
U5 .up.> U1 : <<extend>>
@enduml`;

D["2.7"] = `@startuml
${SKIN}
left to right direction
actor "Người tạo nội dung" as C
rectangle "UC-03 — Quản lý quiz và ngân hàng câu hỏi" {
  usecase "Quản lý quiz" as U1
  usecase "Kiểm tra quyền sở hữu" as U2
  usecase "Soạn câu hỏi mới" as U3
  usecase "Chọn câu từ ngân hàng" as U4
  usecase "Sắp thứ tự câu trong đề" as U5
  usecase "Tải ảnh bìa\\n(kiểm tra chữ ký byte)" as U6
  usecase "Sinh đề bằng AI" as U7
}
C --> U1
U1 ..> U2 : <<include>>
U1 ..> U5 : <<include>>
U3 .up.> U1 : <<extend>>
U4 .up.> U1 : <<extend>>
U6 .up.> U1 : <<extend>>
U7 .up.> U3 : <<extend>>
@enduml`;

D["2.8"] = `@startuml
${SKIN}
left to right direction
actor "Người học" as L
rectangle "UC-05 — Làm bài quiz cá nhân" {
  usecase "Làm bài quiz" as U1
  usecase "Chốt đề tại thời điểm bắt đầu" as U2
  usecase "Tự động lưu câu trả lời" as U3
  usecase "Nộp bài" as U4
  usecase "Chấm tự động\\n(câu có đáp án xác định)" as U5
  usecase "Chấm câu tự luận bằng AI" as U6
  usecase "Đồng bộ hành vi sang Neo4j" as U7
  usecase "Xem kết quả và giải thích" as U8
}
L --> U1
U1 ..> U2 : <<include>>
U1 ..> U3 : <<include>>
U1 ..> U4 : <<include>>
U4 ..> U5 : <<include>>
U4 ..> U7 : <<include>>
U4 ..> U8 : <<include>>
U6 .up.> U4 : <<extend>>
@enduml`;

D["2.9"] = `@startuml
${SKIN}
left to right direction
actor "Người học" as L
actor "Khách" as G
actor "Người tạo nội dung" as C
rectangle "UC-06 — Tham gia phòng đấu thời gian thực" {
  usecase "Tham gia phòng bằng mã PIN" as U1
  usecase "Xác thực JWT tại\\nkhung STOMP CONNECT" as U2
  usecase "Nhận khóa phiên khách" as U3
  usecase "Trả lời câu hỏi" as U4
  usecase "Tính điểm theo tốc độ" as U5
  usecase "Xem bảng xếp hạng trực tiếp" as U6
  usecase "Mở phòng và điều khiển ván" as U7
  usecase "Vào lại sau khi mất kết nối\\n(giữ nguyên điểm)" as U8
}
L --> U1
G --> U1
C --> U7
U1 ..> U2 : <<include>>
U1 ..> U4 : <<include>>
U4 ..> U5 : <<include>>
U5 ..> U6 : <<include>>
U3 .up.> U1 : <<extend>>
U8 .up.> U1 : <<extend>>
note bottom of U3
  Chỉ khi chủ phòng bật
  allow_guests
end note
@enduml`;

D["2.10"] = `@startuml
${SKIN}
left to right direction
actor "Người tạo nội dung" as C
rectangle "UC-07 — Sinh đề bằng AI từ học liệu" {
  usecase "Nạp học liệu" as U1
  usecase "Bóc tách văn bản (Tika)" as U2
  usecase "Chia đoạn và sinh vector nhúng" as U3
  usecase "Sinh đề bằng AI" as U4
  usecase "Truy hồi đoạn học liệu liên quan" as U5
  usecase "Gọi mô hình sinh câu hỏi" as U6
  usecase "Chuyển nhà cung cấp dự phòng" as U7
  usecase "Kiểm chứng JSON trả về" as U8
  usecase "Duyệt câu hỏi trước khi\\nvào ngân hàng" as U9
}
C --> U1
C --> U4
C --> U9
U1 ..> U2 : <<include>>
U2 ..> U3 : <<include>>
U4 ..> U5 : <<include>>
U5 ..> U6 : <<include>>
U6 ..> U8 : <<include>>
U7 .up.> U6 : <<extend>>
note right of U7
  Khi Gemini lỗi tạm thời
  (429 / 5xx / timeout)
end note
@enduml`;

D["2.11"] = `@startuml
${SKIN}
left to right direction
actor "Người học" as L
actor "Người tạo nội dung" as C
rectangle "UC-09 — Hỏi trợ lý học tập" {
  usecase "Hỏi trợ lý học tập" as U1
  usecase "Sinh vector cho câu hỏi" as U2
  usecase "Truy hồi trong phạm vi\\nđược phép đọc" as U3
  usecase "Lọc theo ngưỡng khoảng cách" as U4
  usecase "Trả lời theo luồng (SSE)" as U5
  usecase "Trả kèm trích dẫn nguồn" as U6
  usecase "Trả lời 'không biết'\\nkhi không có tài liệu liên quan" as U7
  usecase "Xem lại hội thoại đã lưu" as U8
}
L --> U1
C --> U1
L --> U8
U1 ..> U2 : <<include>>
U2 ..> U3 : <<include>>
U3 ..> U4 : <<include>>
U4 ..> U5 : <<include>>
U5 ..> U6 : <<include>>
U7 .up.> U4 : <<extend>>
@enduml`;

D["2.12"] = `@startuml
${SKIN}
left to right direction
actor "Người học" as L
rectangle "UC-10 — Nhận gợi ý bài thi và lộ trình học" {
  usecase "Nhận gợi ý quiz" as U1
  usecase "Tìm chủ đề còn yếu\\n(quan hệ PRACTICED)" as U2
  usecase "Loại quiz đã từng làm\\n(quan hệ ATTEMPTED)" as U3
  usecase "Gợi ý theo người học tương tự" as U4
  usecase "Xếp hạng và nêu lý do gợi ý" as U5
  usecase "Xem lộ trình học" as U6
  usecase "Dựng lại đồ thị từ lịch sử" as U7
}
L --> U1
L --> U6
U1 ..> U2 : <<include>>
U1 ..> U3 : <<include>>
U1 ..> U4 : <<include>>
U1 ..> U5 : <<include>>
U7 .up.> U1 : <<extend>>
note right of U7
  Dùng cho dữ liệu có trước
  khi tính năng ra đời
end note
@enduml`;

/* ============================================================
 * 2.13 — BIỂU ĐỒ LỚP THIẾT KẾ TỔNG THỂ
 * ============================================================ */
D["2.13"] = `@startuml
' Style sạch cho biểu đồ lớp thiết kế tổng thể (khác phong cách hồng của VOPC)
skinparam dpi 150
skinparam defaultFontName "Times New Roman"
skinparam defaultFontSize 12
skinparam shadowing false
skinparam roundCorner 4
skinparam classAttributeIconSize 0
skinparam guillemet false
hide circle
skinparam nodesep 35
skinparam ranksep 55

skinparam class {
  BackgroundColor #F4F8FC
  BorderColor #2E5984
  HeaderBackgroundColor #C7DAEE
  ArrowColor #2E5984
  AttributeFontSize 11
  FontSize 12
}

class User {
  -id : UUID
  -email : String
  -passwordHash : String
  -googleId : String
  -displayName : String
  -role : Role
}

class Category {
  -id : UUID
  -name : String
  -slug : String
}

class Quiz {
  -id : UUID
  -title : String
  -difficulty : Difficulty
  -visibility : Visibility
  -timeLimitSec : Integer
  -isAiGenerated : boolean
}

class Question {
  -id : UUID
  -type : QuestionType
  -content : String
  -explanation : String
  -rubric : String
  -topic : String
  -points : int
  -source : QuestionSource
}

class QuestionOption {
  -id : UUID
  -content : String
  -isCorrect : boolean
  -orderIndex : int
}

class QuizQuestion {
  -orderIndex : int
}

class QuizAttempt {
  -id : UUID
  -mode : AttemptMode
  -status : AttemptStatus
  -startedAt : OffsetDateTime
  -expiresAt : OffsetDateTime
  -totalScore : int
  -maxScore : int
}

class AttemptAnswer {
  -id : UUID
  -userAnswer : AnswerPayload
  -isCorrect : Boolean
  -score : int
  -aiFeedback : String
  -aiSuggestions : String
  -gradedBy : GradedBy
}

class LearningMaterial {
  -id : UUID
  -title : String
  -sourceType : MaterialSourceType
  -status : MaterialStatus
  -shared : boolean
  -chunkCount : int
}

class MaterialChunk {
  -id : UUID
  -chunkIndex : int
  -content : String
  -embedding : vector(768)
}

class GameRoom {
  -id : UUID
  -roomCode : String
  -status : RoomStatus
  -secondsPerQuestion : Integer
  -allowGuests : boolean
}

class GameRoomPlayer {
  -id : UUID
  -displayName : String
  -avatar : String
  -isGuest : boolean
  -finalScore : Integer
}

class ChatSession {
  -id : UUID
  -title : String
}

class ChatMessage {
  -id : UUID
  -role : ChatRole
  -content : String
}

class AiJob {
  -id : UUID
  -type : JobType
  -status : JobStatus
}

User "1" --> "0..*" Quiz : sở hữu
User "1" --> "0..*" Question : soạn
User "1" --> "0..*" QuizAttempt : làm bài
User "1" --> "0..*" LearningMaterial : nạp
User "1" --> "0..*" GameRoom : mở phòng
User "1" --> "0..*" ChatSession : hội thoại
User "1" --> "0..*" AiJob : yêu cầu
User "0..1" --> "0..*" GameRoomPlayer : tham gia
Category "1" --> "0..*" Quiz : phân loại
Quiz "1" --> "0..*" QuizQuestion
Question "1" --> "0..*" QuizQuestion
Question "1" *-- "0..*" QuestionOption
Quiz "1" --> "0..*" QuizAttempt
QuizAttempt "1" *-- "1..*" AttemptAnswer
Question "1" --> "0..*" AttemptAnswer
LearningMaterial "1" *-- "0..*" MaterialChunk
Quiz "1" --> "0..*" GameRoom : dùng cho
GameRoom "1" *-- "1..*" GameRoomPlayer
ChatSession "1" *-- "0..*" ChatMessage
@enduml`;

/* ============================================================
 * 2.14 / 2.15 — ĐĂNG NHẬP
 * ============================================================ */
D["2.14"] = `@startuml
${SKIN}
title Biểu đồ trình tự — Use case Đăng nhập

actor "Người dùng" as U
boundary "LoginPage" as B
control "AuthController" as CT
control "AuthService" as S
entity "UserRepository" as R
control "JwtService" as J
control "RefreshTokenService" as RT
database "Redis" as RD

U -> B : nhập email, mật khẩu
B -> CT : POST /api/v1/auth/login
CT -> S : login(request)
S -> S : chuẩn hóa email về chữ thường
S -> R : findByEmail(email)
R --> S : User
alt Không tìm thấy hoặc mật khẩu sai
  S --> CT : BusinessException 401
  CT --> B : cùng một thông báo cho cả hai trường hợp
  B --> U : "Email hoặc mật khẩu không đúng"
else Xác thực thành công
  S -> S : so khớp BCrypt
  S -> J : generateAccessToken(user)
  J --> S : JWT (15 phút, chứa vai trò)
  S -> RT : issue(userId)
  RT -> RD : SET session:{token} TTL 14 ngày
  RT -> RD : SADD user-sessions:{userId}
  RT --> S : refreshToken
  S --> CT : AuthResponse(user, tokens)
  CT --> B : 200 OK
  B --> U : vào trang chính, menu theo vai trò
end
@enduml`;

D["2.15"] = `@startuml
${SKIN}
title Biểu đồ lớp VOPC — Use case Đăng nhập
left to right direction

class LoginPage <<boundary>>
class AuthController <<control>>
class AuthService <<control>>
class JwtService <<control>>
class RefreshTokenService <<control>>
class User <<entity>>
class UserRepository <<control>>

LoginPage --> AuthController
AuthController --> AuthService
AuthService --> UserRepository
UserRepository --> User
AuthService --> JwtService
AuthService --> RefreshTokenService
@enduml`;

/* ============================================================
 * 2.16 / 2.17 — QUẢN LÝ QUIZ VÀ NGÂN HÀNG CÂU HỎI
 * ============================================================ */
D["2.16"] = `@startuml
${SKIN}
title Biểu đồ trình tự — Use case Quản lý quiz và ngân hàng câu hỏi

actor "Người tạo nội dung" as C
boundary "QuizEditorPage" as B
control "QuizController" as QC
control "QuizService" as QS
control "OwnershipGuard" as OG
control "QuestionService" as QNS
entity "Quiz" as Q
entity "Question" as QN
entity "QuestionOption" as QO
control "FileStorageService" as FS

C -> B : nhập tiêu đề, danh mục, độ khó, thời lượng
B -> QC : POST /api/v1/quizzes
QC -> QS : create(request, currentUser)
QS -> Q : new Quiz(ownerId = currentUser)
QS --> QC : QuizResponse
QC --> B : 201 Created

C -> B : soạn câu hỏi mới
B -> QC : POST /api/v1/questions
QC -> QNS : create(request)
QNS -> QNS : kiểm tra dữ liệu theo từng loại câu hỏi
alt Dữ liệu không hợp lệ
  QNS --> B : 400 kèm lỗi từng trường
else Hợp lệ
  QNS -> QN : lưu Question
  QNS -> QO : lưu các phương án
end

C -> B : thêm câu vào quiz, sắp thứ tự
B -> QC : PUT /api/v1/quizzes/{id}/questions
QC -> QS : setQuestions(quizId, ids)
QS -> OG : requireOwner(quiz, currentUser)
alt Không phải chủ sở hữu
  OG --> QC : NotFoundException
  QC --> B : 404 (không tiết lộ tài nguyên tồn tại)
else Là chủ sở hữu
  QS -> Q : cập nhật liên kết kèm orderIndex
end

C -> B : tải ảnh bìa
B -> QC : POST /api/v1/files/images
QC -> FS : store(file)
FS -> FS : nhận dạng theo chữ ký byte, giới hạn 2MB
FS --> QC : đường dẫn /uploads/images/{uuid}.ext
@enduml`;

D["2.17"] = `@startuml
${SKIN}
title Biểu đồ lớp VOPC — Use case Quản lý quiz và ngân hàng câu hỏi
left to right direction

class QuizEditorPage <<boundary>>
class QuizController <<control>>
class QuizService <<control>>
class QuestionService <<control>>
class OwnershipGuard <<control>>
class FileStorageService <<control>>
class Quiz <<entity>>
class Question <<entity>>
class QuestionOption <<entity>>
class QuizQuestion <<entity>>

QuizEditorPage --> QuizController
QuizController --> QuizService
QuizController --> QuestionService
QuizService --> OwnershipGuard
QuizService --> Quiz
QuizService --> QuizQuestion
QuestionService --> Question
QuestionService --> QuestionOption
QuizController --> FileStorageService
@enduml`;

/* ============================================================
 * 2.18 / 2.19 — LÀM BÀI QUIZ CÁ NHÂN
 * ============================================================ */
D["2.18"] = `@startuml
${SKIN}
title Biểu đồ trình tự — Use case Làm bài quiz cá nhân

actor "Người học" as L
boundary "AttemptPage" as B
control "AttemptController" as AC
control "AttemptService" as AS
entity "QuizAttempt" as QA
entity "AttemptAnswer" as AA
control "AnswerGrader" as AG
control "AiGradingService" as AI
control "AttemptGraphSyncService" as GS
database "Neo4j" as NEO

L -> B : chọn chế độ và bắt đầu
B -> AC : POST /api/v1/quizzes/{id}/attempts
AC -> AS : start(quizId, userId, mode)
alt Đã có lượt đang làm dở
  AS --> AC : trả về đúng lượt đó để làm tiếp
else Tạo lượt mới
  AS -> QA : new QuizAttempt(maxScore chốt lúc này)
  AS -> AA : sinh sẵn từng câu của riêng lượt này
  note right of AA : chốt đề — sửa quiz sau đó\\nkhông ảnh hưởng bài đang làm
end
AS --> B : đề bài (KHÔNG kèm đáp án đúng)

loop mỗi câu trả lời
  L -> B : chọn đáp án
  B -> AC : PUT /api/v1/attempts/{id}/answers/{qid}
  AC -> AS : saveAnswer(...)
  AS -> AA : lưu ngay, không chờ tới lúc nộp
end

L -> B : nộp bài
B -> AC : POST /api/v1/attempts/{id}/submit
AC -> AS : submit(attemptId, userId)
alt Đã quá thời điểm hết hạn
  AS -> QA : status = EXPIRED, chỉ chấm câu đã trả lời
else Còn hạn
  AS -> AG : chấm các câu có đáp án xác định
  AG --> AS : điểm từng câu
  AS -> AI : xếp câu tự luận vào hàng đợi chấm AI
  AS -> QA : tính tổng điểm, status = SUBMITTED
end
AS -> GS : publish AttemptSubmittedEvent (AFTER_COMMIT)
GS -> NEO : MERGE (User)-[:ATTEMPTED]->(Quiz), cập nhật PRACTICED
AS --> B : kết quả kèm đáp án đúng và lời giải thích
B --> L : hiển thị điểm và giải thích
@enduml`;

D["2.19"] = `@startuml
${SKIN}
title Biểu đồ lớp VOPC — Use case Làm bài quiz cá nhân
left to right direction

class AttemptPage <<boundary>>
class AttemptController <<control>>
class AttemptService <<control>>
class AnswerGrader <<control>>
class AiGradingService <<control>>
class AttemptGraphSyncService <<control>>
class QuizAttempt <<entity>>
class AttemptAnswer <<entity>>
class Question <<entity>>

AttemptPage --> AttemptController
AttemptController --> AttemptService
AttemptService --> QuizAttempt
AttemptService --> AttemptAnswer
AttemptService --> AnswerGrader
AnswerGrader --> Question
AttemptService --> AiGradingService
AttemptService --> AttemptGraphSyncService
@enduml`;

/* ============================================================
 * 2.20 / 2.21 — THAM GIA PHÒNG ĐẤU THỜI GIAN THỰC
 * ============================================================ */
D["2.20"] = `@startuml
${SKIN}
title Biểu đồ trình tự — Use case Tham gia phòng đấu thời gian thực

actor "Chủ phòng" as H
actor "Người chơi" as P
boundary "RoomPage" as B
control "StompAuthChannelInterceptor" as AUTH
control "RoomStompController" as RC
control "RoomService" as RS
control "SpeedScorer" as SC
control "RoomStateStore" as ST
control "GameEventPublisher" as PUB
control "GameEventRelay" as REL
database "Redis" as RD

H -> RS : mở phòng từ một quiz
RS -> RS : sinh mã PIN 6 ký tự (bỏ 0/O, 1/I)
RS -> RD : lưu trạng thái phòng, TTL

P -> B : nhập mã PIN, chọn biệt danh
B -> AUTH : CONNECT (Authorization: Bearer JWT)
alt Token không hợp lệ
  AUTH --> B : từ chối kết nối
else Hợp lệ hoặc có khóa phiên khách
  AUTH -> RC : cho phép đăng ký /topic/room/{code}
  RC -> RS : join(code, player)
  RS -> ST : thêm người chơi
  RS -> PUB : phát danh sách người chơi
  PUB -> RD : PUBLISH room:{code}:events
  RD -> REL : nhận sự kiện
  REL --> B : cập nhật danh sách
end

H -> RC : bắt đầu ván
loop mỗi câu hỏi
  RC -> PUB : phát câu hỏi + thời gian giới hạn
  PUB -> RD : PUBLISH
  RD -> REL : fan-out tới mọi tiến trình
  REL --> B : hiển thị câu hỏi đồng thời
  P -> B : chọn đáp án
  B -> RC : gửi đáp án
  RC -> SC : score(correct, elapsedMs)
  SC --> RC : điểm theo độ chính xác và tốc độ
  RC -> ST : cập nhật điểm trên Redis
  RC -> PUB : phát bảng xếp hạng mới
  PUB -> RD : PUBLISH
  RD -> REL : fan-out
  REL --> B : bảng xếp hạng trực tiếp
end

RC -> RS : kết thúc ván
RS -> RS : ghi final_score xuống PostgreSQL
RS --> B : kết quả cuối ván

note over ST, RD
  Trạng thái đang chơi chỉ nằm ở Redis;
  chỉ kết quả cuối ván ghi xuống CSDL quan hệ
end note
@enduml`;

D["2.21"] = `@startuml
${SKIN}
title Biểu đồ lớp VOPC — Use case Tham gia phòng đấu thời gian thực
left to right direction

class RoomPage <<boundary>>
class RoomStompController <<control>>
class StompAuthChannelInterceptor <<control>>
class RoomService <<control>>
class SpeedScorer <<control>>
class RoomStateStore <<control>>
class GameEventPublisher <<control>>
class GameEventRelay <<control>>
class GameRoom <<entity>>
class GameRoomPlayer <<entity>>
class RoomState <<entity>>

RoomPage --> StompAuthChannelInterceptor
StompAuthChannelInterceptor --> RoomStompController
RoomStompController --> RoomService
RoomStompController --> SpeedScorer
RoomService --> RoomStateStore
RoomStateStore --> RoomState
RoomService --> GameRoom
RoomService --> GameRoomPlayer
RoomService --> GameEventPublisher
GameEventPublisher --> GameEventRelay
GameEventRelay --> RoomPage
@enduml`;

/* ============================================================
 * 2.22 / 2.23 — SINH ĐỀ BẰNG AI TỪ HỌC LIỆU
 * ============================================================ */
D["2.22"] = `@startuml
${SKIN}
title Biểu đồ trình tự — Use case Sinh đề bằng AI từ học liệu

actor "Người tạo nội dung" as C
boundary "MaterialsPage / GeneratePage" as B
control "AiController" as AC
control "MaterialService" as MS
control "TextExtractor (Tika)" as TK
control "TextChunker" as CH
control "AiOrchestrator" as ORCH
entity "MaterialChunkRepository" as MCR
control "AiJobService" as JS
control "QuestionGenerationService" as QG
control "QuestionPromptBuilder" as PB
control "QuestionJsonParser" as JP
control "AiRequestLogger" as LOG

group Pha 1 — Nạp học liệu (chạy nền)
  C -> B : tải tệp PDF/DOCX/TXT
  B -> AC : POST /api/v1/ai/materials/upload
  AC -> MS : createFromFile(file)
  MS -> TK : extract(file)
  alt Tệp hỏng, vượt 10MB hoặc không bóc tách được
    MS --> B : status = FAILED kèm lý do hiển thị
  else Bóc tách được
    TK --> MS : văn bản thuần
    MS -> CH : chia đoạn có chồng lấp
    CH --> MS : danh sách đoạn
    loop mỗi đoạn
      MS -> ORCH : embed(chunk)
      ORCH --> MS : vector 768 chiều
      MS -> MCR : lưu content + embedding
    end
    MS --> B : status = READY
  end
end

group Pha 2 — Sinh đề
  C -> B : chọn học liệu, chủ đề, độ khó, số lượng
  B -> AC : POST /api/v1/ai/questions/generate
  AC -> JS : enqueue(GENERATE_QUESTIONS)
  JS --> AC : jobId
  AC --> B : 202 Accepted kèm jobId
  JS -> QG : run(job)
  QG -> ORCH : embed(chủ đề yêu cầu)
  QG -> MCR : searchSimilar(ownerId, embedding, topK)
  MCR --> QG : các đoạn liên quan
  QG -> PB : dựng prompt (chỉ dẫn + ngữ cảnh rào + lược đồ JSON)
  QG -> ORCH : generate(prompt)
  ORCH -> ORCH : gọi Gemini
  alt Gemini lỗi tạm thời (429/5xx/timeout)
    ORCH -> ORCH : chuyển sang Groq
  end
  ORCH -> LOG : ghi provider, token, độ trễ
  ORCH --> QG : JSON câu hỏi
  QG -> JP : parse và kiểm chứng
  JP --> QG : loại câu sai cấu trúc
  QG -> JS : lưu câu hỏi nháp kèm đoạn nguồn
  C -> B : xem, sửa và DUYỆT từng câu
  B -> AC : POST /api/v1/ai/questions/approve
  AC --> B : câu đã duyệt vào ngân hàng
end
@enduml`;

D["2.23"] = `@startuml
${SKIN}
title Biểu đồ lớp VOPC — Use case Sinh đề bằng AI từ học liệu
left to right direction

class MaterialsPage <<boundary>>
class GenerateQuestionsPage <<boundary>>
class AiController <<control>>
class MaterialService <<control>>
class MaterialIngestionService <<control>>
class TextExtractor <<control>>
class TextChunker <<control>>
class AiJobService <<control>>
class QuestionGenerationService <<control>>
class QuestionPromptBuilder <<control>>
class QuestionJsonParser <<control>>
class AiOrchestrator <<control>>
class AiRequestLogger <<control>>
class LearningMaterial <<entity>>
class MaterialChunk <<entity>>
class AiJob <<entity>>
class Question <<entity>>

MaterialsPage --> AiController
GenerateQuestionsPage --> AiController
AiController --> MaterialService
MaterialService --> MaterialIngestionService
MaterialIngestionService --> TextExtractor
MaterialIngestionService --> TextChunker
MaterialIngestionService --> AiOrchestrator
MaterialService --> LearningMaterial
MaterialIngestionService --> MaterialChunk
AiController --> AiJobService
AiJobService --> AiJob
AiJobService --> QuestionGenerationService
QuestionGenerationService --> QuestionPromptBuilder
QuestionGenerationService --> AiOrchestrator
QuestionGenerationService --> QuestionJsonParser
QuestionGenerationService --> Question
AiOrchestrator --> AiRequestLogger
@enduml`;

/* ============================================================
 * 2.24 / 2.25 — HỎI TRỢ LÝ HỌC TẬP
 * ============================================================ */
D["2.24"] = `@startuml
${SKIN}
title Biểu đồ trình tự — Use case Hỏi trợ lý học tập

actor "Người học" as L
boundary "AssistantPage" as B
control "ChatController" as CC
control "ChatService" as CS
control "AiOrchestrator" as ORCH
entity "MaterialChunkRepository" as MCR
control "ChatPromptBuilder" as PB
entity "ChatSession" as SES
entity "ChatMessage" as MSG

L -> B : nhập câu hỏi (tối đa 2000 ký tự)
B -> CC : POST /api/v1/ai/chat (SSE)
CC -> CS : prepare(userId, sessionId, question)
alt Chưa có phiên
  CS -> SES : mở phiên mới, tiêu đề cắt từ câu hỏi đầu
end
CS -> ORCH : embed(question)
ORCH --> CS : vector truy vấn
CS -> MCR : searchSimilarIncludingShared(userId, vector, topK=5)
note right of MCR
  Lọc quyền TRƯỚC trong CTE materialized:
  tài liệu của tôi HOẶC đã bật shared,
  rồi mới tính khoảng cách cosine
end note
MCR --> CS : các đoạn kèm khoảng cách
CS -> CS : loại đoạn vượt ngưỡng 0,75

alt Không còn đoạn nào đủ liên quan
  CS -> PB : prompt nói rõ "không có tài liệu liên quan"
  note right of PB : trợ lý trả lời KHÔNG BIẾT,\\nkhông suy đoán từ kiến thức nền
else Còn đoạn hợp lệ
  CS -> PB : dựng prompt (chỉ dẫn + ngữ cảnh + lịch sử phiên)
end

CC --> B : event "meta" — sessionId + danh sách nguồn
CS -> ORCH : stream(prompt)
alt Lỗi trước mảnh chữ đầu tiên
  ORCH -> ORCH : chuyển sang Groq
else Lỗi giữa luồng
  ORCH --> CC : event "error"
  note right of ORCH
    KHÔNG chuyển nhà cung cấp giữa dòng —
    sẽ nối câu trả lời của hai mô hình
  end note
end
loop từng mảnh chữ
  ORCH --> CC : delta
  CC --> B : event "token"
  B --> L : chữ hiện dần
end
CS -> MSG : lưu câu hỏi và câu trả lời
@enduml`;

D["2.25"] = `@startuml
${SKIN}
title Biểu đồ lớp VOPC — Use case Hỏi trợ lý học tập
left to right direction

class AssistantPage <<boundary>>
class ChatController <<control>>
class ChatService <<control>>
class ChatPromptBuilder <<control>>
class AiOrchestrator <<control>>
class MaterialChunkRepository <<control>>
class ChatSession <<entity>>
class ChatMessage <<entity>>
class LearningMaterial <<entity>>
class MaterialChunk <<entity>>

AssistantPage --> ChatController
ChatController --> ChatService
ChatService --> AiOrchestrator
ChatService --> MaterialChunkRepository
MaterialChunkRepository --> MaterialChunk
MaterialChunk --> LearningMaterial
ChatService --> ChatPromptBuilder
ChatService --> ChatSession
ChatSession --> ChatMessage
@enduml`;

/* ============================================================
 * 2.26 / 2.27 — GỢI Ý BÀI THI VÀ LỘ TRÌNH HỌC
 * ============================================================ */
D["2.26"] = `@startuml
${SKIN}
title Biểu đồ trình tự — Use case Nhận gợi ý bài thi và lộ trình học

actor "Người học" as L
boundary "RecommendationPage" as B
control "RecommendationController" as RC
control "RecommendationService" as RS
entity "RecommendationRepository" as RR
database "Neo4j" as NEO

L -> B : mở trang gợi ý
B -> RC : GET /api/v1/recommendations
RC -> RS : forUser(userId)

RS -> RR : weakTopics(userId)
RR -> NEO : MATCH (u:User)-[p:PRACTICED]->(t:Topic)\\nWHERE p.accuracy < ngưỡng
NEO --> RR : danh sách chủ đề còn yếu
RR --> RS : chủ đề yếu

RS -> RR : quizzesByTopic(topics, loại trừ đã làm)
RR -> NEO : MATCH (q:Quiz)-[:COVERS]->(t:Topic)\\nWHERE NOT (u)-[:ATTEMPTED]->(q)
NEO --> RR : quiz theo chủ đề yếu
RR --> RS : danh sách quiz

RS -> RR : peerQuizzes(userId)
RR -> NEO : MATCH (u)-[:ATTEMPTED]->(shared)<-[:ATTEMPTED]-(peer)\\nWITH peer, count(DISTINCT shared) AS similarity
NEO --> RR : quiz của người học tương tự
RR --> RS : danh sách quiz

alt Chưa có dữ liệu hành vi
  RS --> RC : danh sách rỗng + hướng dẫn làm một bài
  note right of RS : KHÔNG gợi ý bừa\\ntheo độ phổ biến
else Neo4j không phản hồi
  RS --> RC : danh sách rỗng (không phải lỗi 500)
  note right of RS : các chức năng khác\\nkhông bị ảnh hưởng
else Có dữ liệu
  RS -> RS : hợp nhất, xếp hạng, kèm lý do gợi ý
  RS --> RC : danh sách gợi ý
end
RC --> B : 200 OK
B --> L : hiển thị quiz gợi ý kèm lý do và lộ trình học
@enduml`;

D["2.27"] = `@startuml
${SKIN}
title Biểu đồ lớp VOPC — Use case Nhận gợi ý bài thi và lộ trình học
left to right direction

class RecommendationPage <<boundary>>
class LearningPathPage <<boundary>>
class RecommendationController <<control>>
class RecommendationService <<control>>
class RecommendationRepository <<control>>
class AttemptGraphSyncService <<control>>
class UserNode <<entity>>
class QuizNode <<entity>>
class TopicNode <<entity>>

RecommendationPage --> RecommendationController
LearningPathPage --> RecommendationController
RecommendationController --> RecommendationService
RecommendationService --> RecommendationRepository
RecommendationRepository --> UserNode
RecommendationRepository --> QuizNode
RecommendationRepository --> TopicNode
AttemptGraphSyncService --> UserNode
AttemptGraphSyncService --> QuizNode
AttemptGraphSyncService --> TopicNode
@enduml`;

/* ===================== RENDER ===================== */
const keys = Object.keys(D);
let ok = 0, fail = 0;
for (const num of keys) {
  const puml = path.join(DG, `hinh-${num}.puml`);
  const png = path.join(OUT, `hinh-${num}.png`);
  fs.writeFileSync(puml, D[num], "utf8");
  try {
    // -tpng output PNG, -charset UTF-8 cho tiếng Việt, -o tới thư mục assets
    execFileSync("java", [
      "-Dfile.encoding=UTF-8",
      "-jar", JAR,
      "-tpng",
      "-charset", "UTF-8",
      "-o", OUT,
      puml,
    ], { stdio: "pipe" });
    if (fs.existsSync(png)) {
      console.log(`OK  hinh-${num}.png (${fs.statSync(png).size} bytes)`);
      ok++;
    } else {
      console.error(`FAIL hinh-${num}: PNG không được tạo`);
      fail++;
    }
  } catch (e) {
    console.error(`FAIL hinh-${num}:`, (e.stderr || e.message || "").toString().slice(0, 600));
    fail++;
  }
}
console.log(`\nDone: ${ok}/${keys.length} sơ đồ PlantUML (${fail} fail).`);
