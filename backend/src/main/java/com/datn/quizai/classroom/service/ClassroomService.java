package com.datn.quizai.classroom.service;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.classroom.domain.Classroom;
import com.datn.quizai.classroom.domain.ClassroomMember;
import com.datn.quizai.classroom.domain.MemberRole;
import com.datn.quizai.classroom.dto.ClassroomResponse;
import com.datn.quizai.classroom.dto.CreateClassroomRequest;
import com.datn.quizai.classroom.dto.MemberResponse;
import com.datn.quizai.classroom.repository.AssignmentRepository;
import com.datn.quizai.classroom.repository.ClassroomMemberRepository;
import com.datn.quizai.classroom.repository.ClassroomRepository;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lớp học và thành viên (features/14, FR-54).
 *
 * <h3>Ba mức quyền, và chúng không giống nhau</h3>
 * <ul>
 *   <li><b>Chủ nhiệm</b> — làm mọi thứ, kể cả xoá lớp và đổi vai trò người khác.</li>
 *   <li><b>Trợ giảng</b> — giao bài, xem kết quả, xem thành viên. <b>Không</b> xoá lớp, <b>không</b> đổi vai
 *       trò: hai việc đó không hoàn tác được, và cho trợ giảng tự nâng người khác lên trợ giảng là mở một
 *       đường để quyền lan ra mà chủ nhiệm không biết.</li>
 *   <li><b>Học sinh</b> — thấy bài được giao và kết quả của chính mình.</li>
 * </ul>
 * Người ngoài lớp nhận <b>404</b> chứ không phải 403, cùng quy ước với phần còn lại của dự án: 403 đã là một
 * xác nhận rằng lớp đó có thật.
 */
@Service
public class ClassroomService {

    private static final Logger log = LoggerFactory.getLogger(ClassroomService.class);

    /**
     * Số lần thử lại khi mã lớp sinh ra trùng.
     * <p>
     * Không gian mã là 31 mũ 6, khoảng 887 triệu, nên trùng gần như không xảy ra; năm lần là để phòng trường
     * hợp bộ sinh hỏng chứ không phải để phòng va chạm ngẫu nhiên. Hết năm lần mà vẫn trùng thì <b>ném lỗi</b>
     * chứ không quay vô hạn: quay mãi thì request treo và không ai biết vì sao.
     */
    private static final int SO_LAN_THU_MA = 5;

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository memberRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public ClassroomService(ClassroomRepository classroomRepository,
                            ClassroomMemberRepository memberRepository,
                            AssignmentRepository assignmentRepository,
                            UserRepository userRepository) {
        this.classroomRepository = classroomRepository;
        this.memberRepository = memberRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ lớp

    @Transactional
    public ClassroomResponse tao(CreateClassroomRequest request, JwtService.AuthenticatedUser current) {
        User owner = userRepository.getReferenceById(current.id());
        Classroom lop = new Classroom(owner, request.name().trim(), request.description(), sinhMaChuaDung());
        classroomRepository.save(lop);

        log.info("Người dùng {} tạo lớp {} mã {}", current.id(), lop.getId(), lop.getClassCode());
        return ClassroomResponse.of(lop, "OWNER", 0, 0);
    }

    /**
     * Lớp của tôi — cả lớp tôi dạy lẫn lớp tôi học.
     * <p>
     * Một endpoint trả cả hai, kèm {@code vaiTroCuaToi} để giao diện tự chia nhóm. Hai endpoint riêng thì
     * người vừa dạy lớp này vừa học lớp kia phải gọi hai lần và tự ghép — mà đó là trường hợp bình thường,
     * không phải ngoại lệ.
     */
    @Transactional(readOnly = true)
    public List<ClassroomResponse> cuaToi(JwtService.AuthenticatedUser current) {
        List<ClassroomResponse> ket = new ArrayList<>();

        for (Classroom lop : classroomRepository.findByOwnerIdOrderByCreatedAtDesc(current.id())) {
            ket.add(dungResponse(lop, "OWNER"));
        }
        for (Classroom lop : classroomRepository.findJoinedBy(current.id())) {
            MemberRole vaiTro = memberRepository.findByClassroomIdAndUserId(lop.getId(), current.id())
                    .map(ClassroomMember::getRole)
                    .orElse(MemberRole.STUDENT);
            ket.add(dungResponse(lop, vaiTro.name()));
        }
        return ket;
    }

    @Transactional(readOnly = true)
    public ClassroomResponse chiTiet(UUID classroomId, JwtService.AuthenticatedUser current) {
        Classroom lop = requireThanhVien(classroomId, current);
        return dungResponse(lop, vaiTroCua(lop, current));
    }

    @Transactional
    public ClassroomResponse capNhat(UUID classroomId, CreateClassroomRequest request,
                                     JwtService.AuthenticatedUser current) {
        Classroom lop = requireGiaoVien(classroomId, current);
        lop.setName(request.name().trim());
        lop.setDescription(request.description());
        return dungResponse(lop, vaiTroCua(lop, current));
    }

    /**
     * Xoá lớp — <b>chỉ chủ nhiệm</b>.
     * <p>
     * Xoá lớp kéo theo mọi bài tập của lớp (ON DELETE CASCADE), và mỗi bài tập bị xoá sẽ gỡ liên kết khỏi các
     * lượt làm bài (ON DELETE SET NULL). Bài làm và điểm của học sinh <b>không mất</b> — chúng chỉ thôi thuộc
     * về một bài tập. Đó là chủ ý: giáo viên xoá lớp của mình không được phép xoá dữ liệu học tập của người
     * khác.
     */
    @Transactional
    public void xoa(UUID classroomId, JwtService.AuthenticatedUser current) {
        Classroom lop = requireChuNhiem(classroomId, current);
        classroomRepository.delete(lop);
        log.info("Đã xoá lớp {} theo yêu cầu của chủ nhiệm {}", classroomId, current.id());
    }

    // ------------------------------------------------------------------ thành viên

    /**
     * Tham gia lớp bằng mã.
     * <p>
     * Vào lại lớp đã ở trong thì <b>không lỗi</b>, chỉ trả lại lớp đó: học sinh bấm "Tham gia" hai lần là
     * chuyện thường, và một thông báo lỗi cho một việc đã ở trạng thái mong muốn chỉ làm họ tưởng mình sai.
     */
    @Transactional
    public ClassroomResponse thamGia(String classCode, JwtService.AuthenticatedUser current) {
        Classroom lop = classroomRepository.findByClassCode(classCode.trim().toUpperCase())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy lớp với mã này"));

        if (lop.getOwner().getId().equals(current.id())) {
            throw BusinessException.badRequest("Bạn là chủ nhiệm lớp này rồi");
        }

        var daCo = memberRepository.findByClassroomIdAndUserId(lop.getId(), current.id());
        if (daCo.isPresent()) {
            return dungResponse(lop, daCo.get().getRole().name());
        }

        User user = userRepository.getReferenceById(current.id());
        memberRepository.save(new ClassroomMember(lop, user));
        log.info("Người dùng {} tham gia lớp {}", current.id(), lop.getId());

        return dungResponse(lop, MemberRole.STUDENT.name());
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> thanhVien(UUID classroomId, JwtService.AuthenticatedUser current) {
        requireGiaoVien(classroomId, current);
        return memberRepository.findByClassroomWithUser(classroomId).stream()
                .map(MemberResponse::from)
                .toList();
    }

    /** Đổi vai trò một thành viên (FR-59) — chỉ chủ nhiệm. */
    @Transactional
    public MemberResponse doiVaiTro(UUID classroomId, UUID userId, MemberRole vaiTroMoi,
                                    JwtService.AuthenticatedUser current) {
        requireChuNhiem(classroomId, current);

        ClassroomMember thanhVien = memberRepository.findByClassroomIdAndUserId(classroomId, userId)
                .orElseThrow(() -> BusinessException.notFound("Người này không ở trong lớp"));

        thanhVien.setRole(vaiTroMoi);
        return MemberResponse.from(thanhVien);
    }

    /** Xoá một thành viên khỏi lớp — chỉ chủ nhiệm. Bài làm của họ vẫn còn, chỉ thôi thuộc lớp này. */
    @Transactional
    public void xoaThanhVien(UUID classroomId, UUID userId, JwtService.AuthenticatedUser current) {
        requireChuNhiem(classroomId, current);
        memberRepository.findByClassroomIdAndUserId(classroomId, userId)
                .ifPresent(memberRepository::delete);
    }

    // ------------------------------------------------------------------ kiểm quyền, dùng chung

    /** Chủ nhiệm hoặc trợ giảng. Ném 404 nếu không phải. */
    @Transactional(readOnly = true)
    public Classroom requireGiaoVien(UUID classroomId, JwtService.AuthenticatedUser current) {
        Classroom lop = requireThanhVien(classroomId, current);
        String vaiTro = vaiTroCua(lop, current);

        if (!"OWNER".equals(vaiTro) && !"CO_TEACHER".equals(vaiTro)) {
            throw BusinessException.notFound("Không tìm thấy lớp học");
        }
        return lop;
    }

    private Classroom requireChuNhiem(UUID classroomId, JwtService.AuthenticatedUser current) {
        Classroom lop = requireThanhVien(classroomId, current);
        if (!lop.getOwner().getId().equals(current.id())) {
            throw BusinessException.forbidden("Chỉ chủ nhiệm lớp mới làm được việc này");
        }
        return lop;
    }

    /** Bất kỳ ai trong lớp: chủ nhiệm, trợ giảng, hoặc học sinh. */
    @Transactional(readOnly = true)
    public Classroom requireThanhVien(UUID classroomId, JwtService.AuthenticatedUser current) {
        Classroom lop = classroomRepository.findById(classroomId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy lớp học"));

        boolean laChu = lop.getOwner().getId().equals(current.id());
        boolean laThanhVien = memberRepository
                .findByClassroomIdAndUserId(classroomId, current.id()).isPresent();

        if (!laChu && !laThanhVien) {
            // 404 chứ không 403: 403 đã là xác nhận rằng lớp đó có thật
            throw BusinessException.notFound("Không tìm thấy lớp học");
        }
        return lop;
    }

    private String vaiTroCua(Classroom lop, JwtService.AuthenticatedUser current) {
        if (lop.getOwner().getId().equals(current.id())) {
            return "OWNER";
        }
        return memberRepository.findByClassroomIdAndUserId(lop.getId(), current.id())
                .map(m -> m.getRole().name())
                .orElse("STUDENT");
    }

    private ClassroomResponse dungResponse(Classroom lop, String vaiTro) {
        return ClassroomResponse.of(lop, vaiTro,
                memberRepository.countByClassroomId(lop.getId()),
                assignmentRepository.findByClassroomWithQuiz(lop.getId()).size());
    }

    private String sinhMaChuaDung() {
        for (int i = 0; i < SO_LAN_THU_MA; i++) {
            String ma = ClassCodeGenerator.sinh();
            if (!classroomRepository.existsByClassCode(ma)) {
                return ma;
            }
        }
        throw new IllegalStateException("Không sinh được mã lớp chưa dùng sau " + SO_LAN_THU_MA + " lần");
    }
}
