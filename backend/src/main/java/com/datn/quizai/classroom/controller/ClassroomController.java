package com.datn.quizai.classroom.controller;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.classroom.domain.MemberRole;
import com.datn.quizai.classroom.dto.AssignmentResponse;
import com.datn.quizai.classroom.dto.ClassroomResponse;
import com.datn.quizai.classroom.dto.CreateAssignmentRequest;
import com.datn.quizai.classroom.dto.CreateClassroomRequest;
import com.datn.quizai.classroom.dto.MemberResponse;
import com.datn.quizai.classroom.service.AssignmentService;
import com.datn.quizai.classroom.service.ClassroomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lớp học và bài tập (features/14).
 *
 * <h3>Ai tạo được lớp</h3>
 * <b>CREATOR/ADMIN</b> — tạo lớp là việc của người dạy. Nhưng <b>tham gia</b> lớp thì mọi tài khoản đã đăng
 * nhập đều làm được: học sinh là người học, không phải người soạn nội dung, nên bắt họ có vai trò CREATOR
 * chỉ để vào lớp là sai hẳn mô hình.
 *
 * <h3>Không có endpoint nào nhận userId</h3>
 * Danh tính luôn lấy từ token. Ngoại lệ duy nhất là hai endpoint quản lý thành viên, nơi chủ nhiệm thao tác
 * lên <i>người khác</i> — và cả hai đều kiểm chủ nhiệm trước.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Classroom", description = "Lớp học, thành viên và bài tập được giao")
@SecurityRequirement(name = "bearerAuth")
public class ClassroomController {

    private final ClassroomService classroomService;
    private final AssignmentService assignmentService;

    public ClassroomController(ClassroomService classroomService, AssignmentService assignmentService) {
        this.classroomService = classroomService;
        this.assignmentService = assignmentService;
    }

    // ------------------------------------------------------------------ lớp

    @GetMapping("/classrooms")
    @Operation(summary = "Lớp của tôi — cả lớp tôi dạy lẫn lớp tôi học, kèm vai trò của tôi trong từng lớp")
    public List<ClassroomResponse> cuaToi(@AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return classroomService.cuaToi(current);
    }

    @PostMapping("/classrooms")
    @PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
    @Operation(summary = "Tạo lớp mới; hệ thống sinh mã lớp 6 ký tự để phát cho học sinh")
    public ResponseEntity<ClassroomResponse> tao(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @Valid @RequestBody CreateClassroomRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(classroomService.tao(request, current));
    }

    @GetMapping("/classrooms/{id}")
    @Operation(summary = "Chi tiết lớp. Người ngoài lớp nhận 404 — không tiết lộ lớp đó có tồn tại.")
    public ClassroomResponse chiTiet(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                     @PathVariable UUID id) {
        return classroomService.chiTiet(id, current);
    }

    @PutMapping("/classrooms/{id}")
    @Operation(summary = "Sửa tên/mô tả lớp — chủ nhiệm hoặc trợ giảng. Mã lớp không đổi được.")
    public ClassroomResponse capNhat(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                     @PathVariable UUID id,
                                     @Valid @RequestBody CreateClassroomRequest request) {
        return classroomService.capNhat(id, request, current);
    }

    @DeleteMapping("/classrooms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xoá lớp — CHỈ chủ nhiệm. Bài làm và điểm của học sinh không mất, chỉ thôi thuộc "
            + "về một bài tập.")
    public void xoa(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                    @PathVariable UUID id) {
        classroomService.xoa(id, current);
    }

    // ------------------------------------------------------------------ thành viên

    @PostMapping("/classrooms/join/{code}")
    @Operation(summary = "Tham gia lớp bằng mã. Mọi tài khoản đã đăng nhập đều vào được. Vào lại lớp đã ở "
            + "trong thì không lỗi, chỉ trả lại lớp đó.")
    public ClassroomResponse thamGia(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                     @PathVariable String code) {
        return classroomService.thamGia(code, current);
    }

    @GetMapping("/classrooms/{id}/members")
    @Operation(summary = "Danh sách thành viên — chủ nhiệm hoặc trợ giảng")
    public List<MemberResponse> thanhVien(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                          @PathVariable UUID id) {
        return classroomService.thanhVien(id, current);
    }

    @PutMapping("/classrooms/{id}/members/{userId}/role")
    @Operation(summary = "Đổi vai trò thành viên (STUDENT / CO_TEACHER) — CHỈ chủ nhiệm")
    public MemberResponse doiVaiTro(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                    @PathVariable UUID id,
                                    @PathVariable UUID userId,
                                    @RequestBody Map<String, String> body) {

        MemberRole vaiTro = MemberRole.valueOf(body.getOrDefault("role", "STUDENT"));
        return classroomService.doiVaiTro(id, userId, vaiTro, current);
    }

    @DeleteMapping("/classrooms/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xoá thành viên khỏi lớp — CHỈ chủ nhiệm. Bài làm của họ vẫn còn.")
    public void xoaThanhVien(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                             @PathVariable UUID id,
                             @PathVariable UUID userId) {
        classroomService.xoaThanhVien(id, userId, current);
    }

    // ------------------------------------------------------------------ bài tập của lớp

    @GetMapping("/classrooms/{id}/assignments")
    @Operation(summary = "Bài tập đã giao cho lớp này")
    public List<AssignmentResponse> baiTapCuaLop(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID id) {
        return assignmentService.cuaLop(id, current);
    }

    @PostMapping("/classrooms/{id}/assignments")
    @Operation(summary = "Giao một quiz cho lớp — chủ nhiệm hoặc trợ giảng, và chỉ giao được quiz của "
            + "chính mình")
    public ResponseEntity<AssignmentResponse> giao(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID id,
            @Valid @RequestBody CreateAssignmentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.giao(id, request, current));
    }
}
