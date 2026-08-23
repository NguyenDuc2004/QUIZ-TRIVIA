package com.datn.quizai.classroom.service;

import com.datn.quizai.attempt.domain.AttemptStatus;
import com.datn.quizai.attempt.domain.QuizAttempt;
import com.datn.quizai.attempt.repository.QuizAttemptRepository;
import com.datn.quizai.attempt.service.AttemptService;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.classroom.domain.Assignment;
import com.datn.quizai.classroom.domain.Classroom;
import com.datn.quizai.classroom.dto.AssignmentResponse;
import com.datn.quizai.classroom.dto.AssignmentResultRow;
import com.datn.quizai.classroom.dto.AssignmentResultsResponse;
import com.datn.quizai.classroom.dto.CreateAssignmentRequest;
import com.datn.quizai.classroom.dto.TrangThaiBaiTap;
import com.datn.quizai.classroom.repository.AssignmentRepository;
import com.datn.quizai.classroom.repository.ClassroomMemberRepository;
import com.datn.quizai.common.OwnershipGuard;
import com.datn.quizai.integrity.domain.AttemptIntegrity;
import com.datn.quizai.integrity.domain.ReviewStatus;
import com.datn.quizai.integrity.repository.AttemptIntegrityRepository;
import com.datn.quizai.integrity.service.RiskScorer;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.quiz.repository.QuizRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Giao bài và theo dõi kết quả (features/14, FR-55 → FR-57).
 *
 * <h3>Không dựng cơ chế làm bài mới</h3>
 * Học sinh làm bài tập bằng đúng luồng {@code quiz_attempts} của features/03. Lớp này chỉ thêm ba việc:
 * kiểm người gọi có ở trong lớp không, kiểm bài đã mở chưa, và đóng dấu {@code assignment_id} lên lượt vừa
 * tạo. Nhờ vậy chấm điểm, thống kê và <b>cả chống gian lận của features/12</b> chạy sẵn cho bài tập mà không
 * phải viết thêm gì — lượt bài tập là lượt {@code EXAM}.
 *
 * <h3>Vì sao có endpoint bắt đầu riêng thay vì thêm tham số vào endpoint cũ</h3>
 * Quiz của giáo viên thường để {@code PRIVATE}, mà {@code AttemptService.start} chặn quiz PRIVATE của người
 * khác — đúng, và không nên nới. Thay vì nhét kiến thức về lớp học vào tầng làm bài, phần cho phép nằm ở
 * đây: đã là thành viên của lớp được giao bài thì được làm quiz đó, dù nó PRIVATE.
 */
@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final ClassroomMemberRepository memberRepository;
    private final ClassroomService classroomService;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final AttemptService attemptService;
    private final AttemptIntegrityRepository integrityRepository;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             ClassroomMemberRepository memberRepository,
                             ClassroomService classroomService,
                             QuizRepository quizRepository,
                             QuizAttemptRepository attemptRepository,
                             AttemptService attemptService,
                             AttemptIntegrityRepository integrityRepository) {
        this.assignmentRepository = assignmentRepository;
        this.memberRepository = memberRepository;
        this.classroomService = classroomService;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.attemptService = attemptService;
        this.integrityRepository = integrityRepository;
    }

    // ------------------------------------------------------------------ giáo viên

    /**
     * Giao một quiz cho lớp (FR-55).
     * <p>
     * Chỉ giao được quiz <b>của chính mình</b>. Cho giao quiz người khác thì một giáo viên có thể phát tán
     * quiz PRIVATE của đồng nghiệp cho cả lớp mình — và chủ quiz không có cách nào biết.
     */
    @Transactional
    public AssignmentResponse giao(UUID classroomId, CreateAssignmentRequest request,
                                   JwtService.AuthenticatedUser current) {
        Classroom lop = classroomService.requireGiaoVien(classroomId, current);

        Quiz quiz = quizRepository.findByIdWithQuestions(request.quizId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz"));

        if (!OwnershipGuard.canManage(quiz.getOwner().getId(), current)) {
            throw BusinessException.notFound("Không tìm thấy quiz");
        }
        if (quiz.getQuizQuestions().isEmpty()) {
            // Chặn ở đây thay vì để học sinh phát hiện: bài tập rỗng thì em nào bấm vào cũng nhận lỗi, và
            // giáo viên chỉ biết khi có người kêu
            throw BusinessException.badRequest("Quiz này chưa có câu hỏi nào, chưa giao được");
        }

        Assignment baiTap = new Assignment(lop, quiz, request.title().trim(), request.instruction(),
                request.openAt(), request.dueAt());
        assignmentRepository.save(baiTap);

        log.info("Lớp {} được giao bài tập {} từ quiz {}", classroomId, baiTap.getId(), quiz.getId());
        return AssignmentResponse.choGiaoVien(baiTap);
    }

    /** Bài đã giao cho một lớp — bản của giáo viên, không kèm trạng thái của ai. */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> cuaLop(UUID classroomId, JwtService.AuthenticatedUser current) {
        classroomService.requireThanhVien(classroomId, current);
        return assignmentRepository.findByClassroomWithQuiz(classroomId).stream()
                .map(AssignmentResponse::choGiaoVien)
                .toList();
    }

    @Transactional
    public void xoa(UUID assignmentId, JwtService.AuthenticatedUser current) {
        Assignment baiTap = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy bài tập"));
        classroomService.requireGiaoVien(baiTap.getClassroom().getId(), current);
        assignmentRepository.delete(baiTap);
    }

    /**
     * Bảng theo dõi lớp cho một bài tập (FR-57).
     * <p>
     * Có <b>một dòng cho mỗi thành viên</b>, kể cả người chưa làm — đó chính là câu hỏi giáo viên cần trả lời.
     * Chỉ liệt kê người đã nộp thì bảng "theo dõi" biến thành bảng "điểm", và *ai chưa làm* không có chỗ nào
     * nói.
     *
     * <h4>Kèm điểm rủi ro (FR-47)</h4>
     * Bảng này là <b>đường vào thứ ba</b> của báo cáo tính toàn vẹn, bên cạnh hàng chờ của Admin và trang
     * thống kê quiz. Thiếu nó thì yêu cầu chỉ đúng trên giấy: bài tập giao cho lớp <i>chạy ở chế độ thi</i>
     * nên có thu tín hiệu hành vi, mà giáo viên chủ nhiệm — người hiểu hoàn cảnh lớp mình nhất — lại mở
     * đúng màn hình này chứ không mở trang thống kê quiz. Tín hiệu được ghi nhận nhưng không ai thấy thì
     * bằng không ghi.
     */
    @Transactional(readOnly = true)
    public AssignmentResultsResponse ketQua(UUID assignmentId, JwtService.AuthenticatedUser current) {
        Assignment baiTap = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy bài tập"));
        classroomService.requireGiaoVien(baiTap.getClassroom().getId(), current);

        // Một truy vấn lấy hết lượt của bài tập, rồi tra theo người — thay vì hỏi cơ sở dữ liệu một lần cho
        // mỗi học sinh. Lớp 40 người thì đó là 40 truy vấn cho một trang, và nó không lộ ra khi thử lớp 3 người.
        Map<UUID, QuizAttempt> theoNguoi = new HashMap<>();
        List<UUID> luotIds = new ArrayList<>();
        for (QuizAttempt luot : attemptRepository.findByAssignmentId(assignmentId)) {
            theoNguoi.put(luot.getUser().getId(), luot);
            luotIds.add(luot.getId());
        }

        // Một truy vấn cho cả bảng, cùng lý do với vòng lặp trên: gọi trong vòng lặp thì lớp 40 người thành
        // 40 truy vấn cho một cột hiển thị, và điều đó không lộ ra khi thử với lớp 3 người.
        Map<UUID, AttemptIntegrity> toanVen = luotIds.isEmpty()
                ? Map.of()
                : integrityRepository.findByAttemptIdIn(luotIds).stream()
                        .collect(Collectors.toMap(AttemptIntegrity::getAttemptId, tv -> tv));

        OffsetDateTime now = OffsetDateTime.now();
        List<AssignmentResultRow> danhSach = new ArrayList<>();
        long soDaNop = 0;
        long soNopTre = 0;
        long tongDiem = 0;

        for (var thanhVien : memberRepository.findByClassroomWithUser(baiTap.getClassroom().getId())) {
            QuizAttempt luot = theoNguoi.get(thanhVien.getUser().getId());
            TrangThaiBaiTap trangThai = trangThaiCua(baiTap, luot, now);

            boolean daNop = trangThai == TrangThaiBaiTap.DA_NOP || trangThai == TrangThaiBaiTap.NOP_TRE;
            if (daNop) {
                soDaNop++;
                tongDiem += luot.getTotalScore();
            }
            if (trangThai == TrangThaiBaiTap.NOP_TRE) {
                soNopTre++;
            }

            // Hai biến rời thay vì hai biểu thức ba ngôi: chúng phải cùng có hoặc cùng không, và viết
            // thành một khối if thì không có cách nào lệch nhau về sau. Giống hệt AnalyticsService.
            Integer diemRuiRo = null;
            ReviewStatus trangThaiRaSoat = null;
            AttemptIntegrity tv = luot == null ? null : toanVen.get(luot.getId());
            if (tv != null && tv.getRiskScore() >= RiskScorer.NGUONG_GAN_CO) {
                diemRuiRo = tv.getRiskScore();
                trangThaiRaSoat = tv.getReviewStatus();
            }

            danhSach.add(new AssignmentResultRow(
                    thanhVien.getUser().getId(),
                    thanhVien.getUser().getDisplayName(),
                    luot == null ? null : luot.getId(),
                    daNop ? luot.getTotalScore() : null,
                    luot == null ? null : luot.getMaxScore(),
                    luot == null ? null : luot.getSubmittedAt(),
                    trangThai,
                    trangThai.nhan(),
                    diemRuiRo,
                    trangThaiRaSoat));
        }

        // Trung bình tính trên BÀI ĐÃ NỘP. Coi người chưa làm là 0 điểm thì con số này nói về tỉ lệ nộp chứ
        // không nói về chất lượng bài — mà tỉ lệ nộp đã có `soDaNop/soThanhVien` trả lời rồi.
        Integer trungBinh = soDaNop == 0 ? null : (int) Math.round((double) tongDiem / soDaNop);

        return new AssignmentResultsResponse(
                AssignmentResponse.choGiaoVien(baiTap),
                danhSach.size(), soDaNop, soNopTre, trungBinh, danhSach);
    }

    // ------------------------------------------------------------------ học sinh

    /** Bài tập của tôi ở mọi lớp tôi tham gia, kèm trạng thái của chính tôi (FR-56). */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> cuaToi(JwtService.AuthenticatedUser current) {
        OffsetDateTime now = OffsetDateTime.now();
        List<Assignment> baiTaps = assignmentRepository.findVisibleFor(current.id(), now);

        Map<UUID, QuizAttempt> theoBaiTap = new HashMap<>();
        for (QuizAttempt luot : attemptRepository.findByUserWithAssignment(current.id())) {
            theoBaiTap.put(luot.getAssignmentId(), luot);
        }

        List<AssignmentResponse> ket = new ArrayList<>();
        for (Assignment baiTap : baiTaps) {
            QuizAttempt luot = theoBaiTap.get(baiTap.getId());
            TrangThaiBaiTap trangThai = trangThaiCua(baiTap, luot, now);

            ket.add(AssignmentResponse.choHocSinh(
                    baiTap, trangThai,
                    luot == null ? null : luot.getId(),
                    luot != null && luot.getStatus() == AttemptStatus.SUBMITTED ? luot.getTotalScore() : null,
                    luot == null ? null : luot.getMaxScore()));
        }
        return ket;
    }

    /**
     * Bắt đầu (hoặc làm tiếp) một bài tập.
     * <p>
     * Ba chốt trước khi cho làm: là thành viên của lớp, bài đã tới giờ mở, và chưa nộp bài này. Chốt thứ ba
     * còn được ràng buộc duy nhất của cơ sở dữ liệu bảo vệ — kiểm trong Java thua cuộc khi học sinh mở hai tab.
     * <p>
     * <b>Quá hạn vẫn làm được.</b> Khoá cứng lúc hết hạn thì một em mất mạng mười phút là mất trắng bài, và
     * giáo viên không còn cách nào biết em ấy có làm hay không. Bài nộp muộn được đánh dấu rõ, quyết định trừ
     * điểm hay không là của giáo viên.
     */
    @Transactional
    public UUID batDau(UUID assignmentId, JwtService.AuthenticatedUser current) {
        Assignment baiTap = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy bài tập"));
        classroomService.requireThanhVien(baiTap.getClassroom().getId(), current);

        OffsetDateTime now = OffsetDateTime.now();
        if (!baiTap.daMo(now)) {
            throw BusinessException.badRequest("Bài tập này chưa tới giờ mở");
        }

        var daCo = attemptRepository.findByAssignmentIdAndUserId(assignmentId, current.id());
        if (daCo.isPresent()) {
            if (daCo.get().getStatus() == AttemptStatus.IN_PROGRESS) {
                return daCo.get().getId();   // làm tiếp bài đang dở
            }
            throw BusinessException.badRequest("Bạn đã nộp bài tập này rồi");
        }

        // Chế độ EXAM: bài tập là bài tính điểm, nên nó cũng được features/12 thu tín hiệu hành vi như mọi
        // lượt thi khác — không phải cấu hình gì thêm.
        UUID attemptId = attemptService.batDauChoBaiTap(baiTap.getQuiz().getId(), assignmentId, current);
        log.info("Người dùng {} bắt đầu bài tập {}", current.id(), assignmentId);
        return attemptId;
    }

    // ------------------------------------------------------------------ nội bộ

    /**
     * Trạng thái của một bài tập với một người, tính từ ba thứ: hạn nộp, lượt làm bài, thời điểm hiện tại.
     * <p>
     * Là {@code static} và không đụng repository để test được bằng unit test chạy trong vài milli-giây — đây
     * là phần có nhánh logic thật của cả lát cắt.
     */
    static TrangThaiBaiTap trangThaiCua(Assignment baiTap, QuizAttempt luot, OffsetDateTime now) {
        if (luot == null) {
            return baiTap.quaHan(now) ? TrangThaiBaiTap.QUA_HAN : TrangThaiBaiTap.CHUA_LAM;
        }
        if (luot.getStatus() == AttemptStatus.IN_PROGRESS) {
            return TrangThaiBaiTap.DANG_LAM;
        }
        // Đã nộp: so thời điểm NỘP với hạn, không so thời điểm hiện tại. Bài nộp đúng hạn không được biến
        // thành "nộp muộn" chỉ vì hôm nay giáo viên mới mở bảng ra xem.
        OffsetDateTime nopLuc = luot.getSubmittedAt();
        boolean tre = baiTap.getDueAt() != null && nopLuc != null && nopLuc.isAfter(baiTap.getDueAt());
        return tre ? TrangThaiBaiTap.NOP_TRE : TrangThaiBaiTap.DA_NOP;
    }
}
