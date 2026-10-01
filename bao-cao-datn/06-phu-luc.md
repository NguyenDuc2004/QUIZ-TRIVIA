# PHỤ LỤC

Phần này chứa những bảng tra cứu đầy đủ đã được dẫn trong thân báo cáo. Chúng đặt ở đây thay vì trong thân bài vì người đọc tra tới khi cần kiểm một chi tiết, chứ không đọc tuần tự.

## Phụ lục A. Đặc tả chi tiết các use case

Năm use case chính của hệ thống, đặc tả lần lượt từ Bảng A.1 đến Bảng A.5 theo mẫu: tác nhân, tiền điều kiện, luồng sự kiện chính, luồng thay thế và hậu điều kiện. Biểu đồ use case tương ứng của từng use case nằm ở mục 2.1.3.

**Bảng A.1. Đặc tả use case UC-01 — Đăng nhập**
| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Khách (người dùng đã có tài khoản) |
| Tiền điều kiện | Tài khoản đã tồn tại, được tạo qua đăng ký hoặc qua liên kết Google |
| Luồng chính | 1. Người dùng nhập email và mật khẩu. 2. Hệ thống chuẩn hóa email về chữ thường và tìm tài khoản. 3. Hệ thống so khớp mật khẩu với giá trị băm BCrypt đã lưu. 4. Hệ thống cấp access token 15 phút chứa vai trò và refresh token lưu ở Redis. 5. Giao diện hiển thị menu theo vai trò và chuyển vào trang danh sách quiz. |
| Luồng thay thế | 3a. Email không tồn tại hoặc mật khẩu sai → trả lỗi 401 với **cùng một thông báo** cho cả hai trường hợp, tránh để dò được email nào đã đăng ký. 1a. Chọn đăng nhập bằng Google → hệ thống xác minh ID token (chữ ký, tổ chức phát hành, hạn dùng, định danh ứng dụng nhận token), yêu cầu email đã được Google xác minh, rồi liên kết hoặc tạo tài khoản mới với vai trò LEARNER. 3b. Tài khoản chỉ đăng nhập bằng Google mà người dùng nhập mật khẩu → hướng dẫn dùng chức năng quên mật khẩu để đặt mật khẩu đầu tiên. |
| Hậu điều kiện | Phiên đăng nhập được thiết lập; các thiết bị khác không bị ảnh hưởng vì mỗi lần đăng nhập cấp một refresh token riêng |

**Bảng A.2. Đặc tả use case UC-06 — Tham gia phòng đấu thời gian thực**
| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Người học; Khách khi chủ phòng cho phép |
| Tiền điều kiện | Phòng tồn tại và đang ở trạng thái chờ |
| Luồng chính | 1. Chủ phòng mở phòng từ một quiz; hệ thống sinh mã PIN sáu ký tự (loại các ký tự dễ đọc nhầm) và mã QR. 2. Người chơi nhập mã PIN, chọn biệt danh và ảnh đại diện. 3. Hệ thống mở kết nối WebSocket, xác thực JWT tại khung STOMP CONNECT và đăng ký người chơi vào chủ đề của phòng. 4. Hệ thống phát danh sách người chơi cập nhật tới toàn bộ phòng. 5. Chủ phòng bắt đầu ván. 6. Hệ thống phát câu hỏi đồng thời tới mọi người chơi kèm thời gian giới hạn cho câu đó. 7. Người chơi chọn đáp án. 8. Hệ thống chấm ngay, tính điểm theo độ chính xác kết hợp thời gian trả lời, cập nhật trạng thái phòng ở Redis. 9. Hệ thống xuất bản sự kiện qua Redis Pub/Sub để mọi tiến trình máy chủ phát bảng xếp hạng mới tới người chơi của mình. 10. Lặp lại bước 6–9 tới câu cuối, sau đó phát kết quả cuối ván và ghi điểm cuối xuống cơ sở dữ liệu. |
| Luồng thay thế | 2a. Người chơi chưa có tài khoản → nếu chủ phòng đã bật tùy chọn cho khách thì được cấp khóa phiên khách (chỉ mở đúng phòng này, sống sáu giờ); nếu không, trả về 403. 3a. Token không hợp lệ → từ chối kết nối WebSocket. 7a. Không trả lời trong thời gian giới hạn → tính không điểm cho câu đó. 3b. Người chơi mất kết nối rồi vào lại → nhận lại trạng thái phòng hiện tại và **giữ nguyên điểm đã có**. |
| Hậu điều kiện | Kết quả ván được lưu; người chơi có tài khoản được ghi nhận vào lịch sử, dữ liệu của khách vãng lai không lưu ngoài phạm vi ván đấu |

**Bảng A.3. Đặc tả use case UC-07 — Sinh đề bằng AI từ học liệu**
| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Người tạo nội dung |
| Tiền điều kiện | Đã đăng nhập với vai trò CREATOR hoặc ADMIN; đã nạp học liệu và học liệu ở trạng thái sẵn sàng (đã sinh xong vector nhúng) |
| Luồng chính | 1. Người tạo nội dung nạp học liệu: tải tệp PDF/DOCX/TXT hoặc dán văn bản. 2. Hệ thống bóc tách văn bản bằng Apache Tika, chia đoạn, sinh vector nhúng cho từng đoạn, lưu vào kho vector và cập nhật trạng thái sang sẵn sàng. 3. Người tạo nội dung chọn học liệu, chủ đề, độ khó, loại câu hỏi và số lượng cần sinh (tối đa 20). 4. Hệ thống tạo công việc nền và **trả về mã công việc ngay**. 5. Hệ thống truy hồi các đoạn học liệu liên quan nhất tới chủ đề yêu cầu. 6. Hệ thống dựng prompt gồm chỉ dẫn hệ thống, ngữ cảnh học liệu được rào trong khối dữ liệu riêng, và lược đồ JSON đầu ra. 7. Hệ thống gọi mô hình qua lớp điều phối, phân tích và kiểm chứng JSON trả về, loại các câu sai cấu trúc. 8. Hệ thống lưu câu hỏi ở dạng nháp kèm nhà cung cấp, mô hình và các đoạn học liệu đã dựa vào. 9. Người tạo nội dung xem từng câu cùng đoạn học liệu nguồn, sửa nếu cần, rồi **duyệt** để đưa vào ngân hàng câu hỏi. |
| Luồng thay thế | 2a. Tệp hỏng, vượt 10 MB hoặc không bóc tách được văn bản → chuyển học liệu sang trạng thái thất bại kèm lý do hiển thị trên giao diện. 7a. Nhà cung cấp chính lỗi tạm thời (vượt hạn mức, lỗi máy chủ, hết thời gian chờ) → tự chuyển sang nhà cung cấp dự phòng. 7b. Vượt hạn mức số lượt mỗi phút → trả lỗi kèm **số giây cần chờ cụ thể** theo phản hồi của nhà cung cấp. 7c. Cả hai nhà cung cấp lỗi → công việc chuyển sang trạng thái thất bại kèm thông báo dễ hiểu. 9a. Không duyệt câu nào → câu hỏi nháp không vào ngân hàng, không ảnh hưởng dữ liệu hiện có. |
| Hậu điều kiện | Các câu hỏi được duyệt đã vào ngân hàng câu hỏi và dùng được cho quiz; mọi lời gọi mô hình được ghi nhật ký kèm số token và độ trễ |

**Bảng A.4. Đặc tả use case UC-09 — Hỏi trợ lý học tập**
| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Người học, Người tạo nội dung |
| Tiền điều kiện | Đã đăng nhập |
| Luồng chính | 1. Người dùng nhập câu hỏi (tối đa 2000 ký tự). 2. Hệ thống mở phiên hội thoại mới nếu chưa có và đặt tiêu đề phiên từ câu hỏi đầu tiên. 3. Hệ thống sinh vector nhúng cho câu hỏi. 4. Hệ thống truy hồi các đoạn học liệu gần nghĩa nhất **trong phạm vi người dùng được phép đọc**: tài liệu của chính họ và tài liệu người khác đã chủ động chia sẻ. 5. Hệ thống loại các đoạn có khoảng cách vượt ngưỡng liên quan. 6. Hệ thống dựng prompt gồm chỉ dẫn hệ thống, ngữ cảnh học liệu và lịch sử hội thoại của phiên. 7. Hệ thống gửi trước danh sách tài liệu sẽ dựa vào, rồi truyền câu trả lời theo từng mảnh chữ qua SSE. 8. Hệ thống lưu câu hỏi và câu trả lời vào phiên hội thoại. |
| Luồng thay thế | 5a. Không còn đoạn nào đủ liên quan → prompt nói rõ không có tài liệu liên quan; trợ lý **trả lời là không biết** thay vì suy đoán từ kiến thức nền. 7a. Mô hình lỗi trước khi phát mảnh chữ đầu tiên → chuyển sang nhà cung cấp dự phòng. 7b. Mô hình lỗi giữa luồng → phát sự kiện lỗi để giao diện hiển thị; **không** chuyển nhà cung cấp giữa dòng vì sẽ nối câu trả lời của hai mô hình thành một đoạn vô nghĩa. 1a. Người dùng dừng câu trả lời đang chạy → giao diện hủy yêu cầu. |
| Hậu điều kiện | Hội thoại được lưu và mở lại được; mỗi câu trả lời gắn với danh sách tài liệu đã dựa vào |

**Bảng A.5. Đặc tả use case UC-10 — Nhận gợi ý bài thi và lộ trình học**
| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Người học |
| Tiền điều kiện | Đã đăng nhập; đã có ít nhất một lượt làm bài để hệ thống có dữ liệu hành vi |
| Luồng chính | 1. Người học mở trang gợi ý. 2. Hệ thống truy vấn đồ thị tìm các chủ đề người học có độ chính xác thấp và các quiz thuộc chủ đề đó mà họ chưa từng làm. 3. Hệ thống truy vấn nhóm người học có nhiều bài làm trùng nhau, lấy các quiz họ đã làm mà người này chưa làm. 4. Hệ thống hợp nhất, xếp hạng và trả về danh sách gợi ý **kèm lý do gợi ý**. 5. Người học chọn một quiz trong danh sách và bắt đầu làm bài. |
| Luồng thay thế | 2a. Người học chưa có dữ liệu hành vi → trả về danh sách rỗng kèm hướng dẫn làm một bài để hệ thống hiểu năng lực, **không** gợi ý bừa theo độ phổ biến. 2b. Cơ sở dữ liệu đồ thị không phản hồi → trả về danh sách rỗng thay vì lỗi hệ thống; các chức năng khác không bị ảnh hưởng. |
| Hậu điều kiện | Người học nhận được danh sách gợi ý phù hợp năng lực kèm lý do để hiểu vì sao được gợi ý |

## Phụ lục B. Mô tả các bảng trong cơ sở dữ liệu

Toàn bộ 35 bảng của lược đồ PostgreSQL, nhóm theo chức năng. Sơ đồ thực thể quan hệ tổng quan nằm ở Hình 2.18 trong mục 2.2.1.

**Bảng B.1. Các nhóm bảng trong lược đồ cơ sở dữ liệu**
| Nhóm | Bảng | Mô tả |
|------|------|-------|
| người dùng và danh mục | `users` | Tài khoản: email, mật khẩu băm, định danh Google, vai trò. Ràng buộc kiểm tra bảo đảm mỗi tài khoản có ít nhất một cách đăng nhập |
|  | `categories` | Danh mục quiz |
| quiz và ngân hàng câu hỏi | `quizzes` | Tiêu đề, danh mục, độ khó, chế độ hiển thị, thời lượng, ảnh bìa, cờ đánh dấu nội dung sinh từ AI |
|  | `questions` | Câu hỏi năm loại: nội dung, độ khó, điểm, chủ đề, lời giải thích, tiêu chí chấm cho câu tự luận |
|  | `question_options` | Phương án trả lời kèm cờ đáp án đúng và thứ tự hiển thị |
|  | `quiz_questions` | Bảng nối quiz với câu hỏi, kèm thứ tự câu trong đề |
| làm bài và chấm điểm | `quiz_attempts` | Lượt làm bài: chế độ, trạng thái, thời điểm bắt đầu và hết hạn, điểm tối đa chốt lúc bắt đầu. Chỉ mục một phần giới hạn mỗi người một bài dở trên một quiz |
|  | `attempt_answers` | Từng câu trong đề của riêng một lượt, sinh sẵn lúc bắt đầu để chốt đề; lưu câu trả lời `jsonb`, điểm, nhận xét AI, người chấm |
| học liệu, RAG và tác vụ AI | `learning_materials` | Học liệu đã nạp: trạng thái xử lý, số đoạn, và cờ `shared` cho phép người học khác hỏi trợ lý trên tài liệu này |
|  | `material_chunks` | Đoạn học liệu kèm vector nhúng 768 chiều (pgvector) |
|  | `ai_jobs` | Tác vụ AI chạy nền: loại, trạng thái, tham số và kết quả `jsonb` |
|  | `ai_request_logs` | Nhật ký lời gọi mô hình: nhà cung cấp, số token vào/ra, độ trễ. Ghi trong giao dịch riêng nên công việc chính hỏng thì bản ghi vẫn còn |
| phòng đấu và trợ lý học tập | `game_rooms` | Mã PIN sáu ký tự, chủ phòng, quiz, cờ cho khách vào chơi. Trạng thái đang chơi nằm ở Redis, không ở đây |
|  | `game_room_players` | Biệt danh và ảnh đại diện chốt lúc chơi, cờ khách vãng lai, điểm cuối ván. Cột người dùng cho phép rỗng để khách chơi được |
|  | `chat_sessions` | Phiên hội thoại với trợ lý, tiêu đề cắt từ câu hỏi đầu tiên |
|  | `chat_messages` | Từng lượt hỏi và trả lời trong một phiên |
| thẻ ghi nhớ và lặp lại ngắt quãng | `flashcard_decks` | Bộ thẻ của một người dùng |
|  | `flashcards` | Mặt trước, mặt sau, gợi ý, và nguồn tạo: tự soạn, sinh từ AI, hoặc dựng từ câu đã làm sai |
|  | `flashcard_reviews` | Trạng thái lặp lại ngắt quãng theo từng cặp (thẻ, người dùng). Tách riêng vì một thẻ dùng chung có thể được nhiều người ôn với lịch khác hẳn nhau |
| trò chơi hóa và bảng xếp hạng theo mùa | `user_stats` | Điểm kinh nghiệm, cấp độ, chuỗi ngày học hiện tại và dài nhất |
|  | `xp_events` | Sổ từng lần cộng điểm. Ràng buộc duy nhất trên (người dùng, loại nguồn, khóa nguồn) là chốt **idempotent** ở tầng cơ sở dữ liệu |
|  | `badges`, `user_badges` | Định nghĩa huy hiệu và bản ghi trao huy hiệu kèm thời điểm |
|  | `daily_challenges`, `user_daily_progress` | Thử thách của từng ngày và tiến độ của mỗi người trên thử thách đó |
|  | `seasons`, `season_rankings` | Mùa giải và bảng xếp hạng **chốt lại khi mùa kết thúc**. Bảng đang diễn ra nằm ở Redis vì nó dựng lại được từ `xp_events`, không phải nguồn sự thật |
| chống gian lận thi | `proctoring_events` | Tín hiệu hành vi trong chế độ thi: sáu loại, thời điểm, và chi tiết `jsonb` **chỉ chứa số** — thao tác dán chỉ lưu độ dài, không lưu nội dung |
|  | `attempt_integrity` | Tổng hợp mỗi lượt thi: điểm rủi ro 0–100, danh sách cờ giải thích lý do, nhận định của mô hình, trạng thái rà soát |
|  | `room_proctoring_events` | Tín hiệu trong phòng đấu, tách riêng vì danh tính ở đây là định danh trong phạm vi phòng nên bảng **không có** khóa ngoại tới `users` |
| lớp học và giao bài | `classrooms` | Lớp học kèm **mã sáu ký tự** để học viên tự tham gia; mã bỏ các ký tự dễ đọc nhầm vì nó được đọc to trong lớp và chép tay lên bảng |
|  | `classroom_members` | Thành viên lớp kèm vai trò. **Giáo viên chủ nhiệm không nằm ở đây** mà là cột chủ sở hữu của `classrooms`, để không có hai nguồn sự thật cho cùng một câu hỏi |
|  | `assignments` | Gắn một quiz cho một lớp kèm hạn nộp. **Không lưu trạng thái nộp**: năm trạng thái đều suy ra được lúc hiển thị từ hạn nộp và lượt làm bài |
| thông báo | `notifications` | Thông báo kèm **khóa chống trùng**, chặn bằng ràng buộc duy nhất ở tầng cơ sở dữ liệu chứ không kiểm trong mã — kiểm trong mã thua khi hai tiến trình cùng thức dậy đúng mốc giờ |
|  | `notification_settings` | Lưu **danh sách loại đã tắt** chứ không phải đã bật, để người chưa mở trang cài đặt vẫn nhận đủ mọi loại |

## Phụ lục C. Kịch bản kiểm thử

Các kịch bản kiểm thử tiêu biểu cùng kết quả thực tế, dẫn ở mục 3.4.2.

**Bảng C.1. Các kịch bản kiểm thử tiêu biểu**
| STT | Chức năng | Kịch bản | Kết quả mong đợi | Kết quả |
|----:|-----------|----------|------------------|---------|
| 1 | Đăng ký | Tự đăng ký vai trò quản trị | Hạ xuống vai trò người học | Đạt |
| 2 | Đăng nhập Google | Tài khoản đã có, gửi kèm vai trò cao hơn | Giữ nguyên vai trò cũ | Đạt |
| 3 | Đổi mật khẩu | Hai thiết bị đang đăng nhập | Thu hồi phiên trên **mọi** thiết bị | Đạt |
| 4 | Xem quiz | Khách xem quiz riêng tư của người khác | Trả về 404 chứ không phải 403 | Đạt |
| 5 | Làm bài | Chủ quiz sửa đề khi có người đang làm dở | Lượt đang làm giữ nguyên đề đã chốt | Đạt |
| 6 | Chấm tự luận | Mô hình trả điểm vượt trần của câu | Giới hạn cứng về trần thật | Đạt |
| 7 | Phòng đấu | Người chơi chia trên hai tiến trình máy chủ | Cả hai bên nhận đủ sự kiện | Đạt |
| 8 | Sinh đề AI | Người dùng đã hết hạn mức trong ngày | Trả 429 ngay, không gọi mô hình | Đạt |
| 9 | Trợ lý học tập | Học liệu của người khác chưa chia sẻ | Không xuất hiện trong truy hồi | Đạt |
| 10 | Gợi ý | Neo4j ngừng hoạt động | Trả danh sách rỗng, không làm hỏng việc nộp bài | Đạt |
| 11 | Tải ảnh | Tệp mã lệnh đặt đuôi `.png` | Từ chối theo chữ ký byte | Đạt |
| 12 | Chống gian lận | Mốc thời gian ở tương lai do đồng hồ máy khách sai | Cắt về thời điểm hiện tại | Đạt |

Phân bố số phép kiểm theo từng nhóm chức năng ở phía máy chủ, dẫn ở mục 3.4.3.

**Bảng C.2. Phân bố phép kiểm theo nhóm chức năng (máy chủ)**
| Nhóm chức năng | Số phép kiểm | Nhóm chức năng | Số phép kiểm |
|----------------|-------------:|----------------|-------------:|
| AI: RAG, sinh đề, chấm tự luận, hạn mức | 116 | Flashcard và lặp lại ngắt quãng | 25 |
| Làm bài và chấm điểm | 57 | Thông báo và nhắc ôn | 24 |
| Xác thực và phân quyền | 43 | Bảng xếp hạng theo mùa | 24 |
| Quản lý quiz và câu hỏi | 38 | Gamification | 21 |
| Lớp học và giao bài | 38 | Trợ lý học tập | 20 |
| Chống gian lận | 33 | Quản trị hệ thống | 19 |
| Phòng đấu thời gian thực | 30 | Thống kê và báo cáo | 17 |
| Tải ảnh lên | 28 | Hồ sơ người dùng | 16 |
| Gợi ý cá nhân hoá (Neo4j) | 28 | Khởi động ứng dụng | 1 |
