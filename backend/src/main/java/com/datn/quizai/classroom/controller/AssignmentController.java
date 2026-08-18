package com.datn.quizai.classroom.controller;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.classroom.dto.AssignmentResponse;
import com.datn.quizai.classroom.dto.AssignmentResultsResponse;
import com.datn.quizai.classroom.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bài tập nhìn từ phía học sinh, và bảng theo dõi của giáo viên (features/14, FR-56 & FR-57).
 * <p>
 * Tách khỏi {@code ClassroomController} vì hai endpoint dưới đây <b>không nằm dưới một lớp cụ thể</b>:
 * {@code /me/assignments} gộp bài của mọi lớp, còn hai cái kia làm việc trên một bài tập mà người gọi có thể
 * không nhớ nó thuộc lớp nào.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Classroom", description = "Lớp học, thành viên và bài tập được giao")
@SecurityRequirement(name = "bearerAuth")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/me/assignments")
    @Operation(summary = "Bài tập được giao cho tôi ở mọi lớp, kèm trạng thái của chính tôi. Bài hẹn giờ "
            + "mở chưa tới giờ thì không hiện.")
    public List<AssignmentResponse> cuaToi(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return assignmentService.cuaToi(current);
    }

    @PostMapping("/assignments/{id}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Bắt đầu hoặc làm tiếp một bài tập. Luôn ở chế độ EXAM. Đã nộp rồi thì 400 — mỗi "
            + "học sinh một lượt cho mỗi bài tập. Quá hạn vẫn làm được, và bài sẽ được đánh dấu nộp muộn.")
    public Map<String, UUID> batDau(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                    @PathVariable UUID id) {
        return Map.of("attemptId", assignmentService.batDau(id, current));
    }

    @GetMapping("/assignments/{id}/results")
    @Operation(summary = "Bảng theo dõi lớp cho một bài tập — chủ nhiệm hoặc trợ giảng. Có MỘT dòng cho "
            + "mỗi thành viên, kể cả người chưa làm.")
    public AssignmentResultsResponse ketQua(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID id) {
        return assignmentService.ketQua(id, current);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Gỡ bài tập khỏi lớp — chủ nhiệm hoặc trợ giảng. Bài làm của học sinh vẫn còn.")
    public void xoa(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                    @PathVariable UUID id) {
        assignmentService.xoa(id, current);
    }
}
