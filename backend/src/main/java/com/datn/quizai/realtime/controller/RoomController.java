package com.datn.quizai.realtime.controller;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.realtime.dto.CreateRoomRequest;
import com.datn.quizai.realtime.dto.RoomView;
import com.datn.quizai.realtime.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản lý phòng đấu qua REST — docs/api.md §5.1.
 * <p>
 * Phần REST chỉ lo vòng đời phòng (mở / vào / xem / rời). Diễn biến ván đấu đi qua STOMP,
 * xem {@link RoomStompController}. Mọi endpoint đều yêu cầu đăng nhập: Guest không vào phòng đấu.
 */
@RestController
@RequestMapping("/api/v1/rooms")
@Tag(name = "Room", description = "Phòng đấu trí thời gian thực")
@SecurityRequirement(name = "bearerAuth")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @Operation(summary = "Mở phòng từ một quiz; host tự động là người chơi đầu tiên")
    public ResponseEntity<RoomView> create(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                           @Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request, current));
    }

    @PostMapping("/{roomCode}/join")
    @Operation(summary = "Vào phòng bằng mã; vào lại phòng cũ vẫn giữ nguyên điểm")
    public RoomView join(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                         @PathVariable String roomCode) {
        return roomService.join(roomCode, current);
    }

    @GetMapping("/{roomCode}")
    @Operation(summary = "Ảnh chụp phòng — dùng để dựng lại màn hình sau khi mất kết nối")
    public RoomView get(@PathVariable String roomCode) {
        return roomService.get(roomCode);
    }

    @DeleteMapping("/{roomCode}/players/me")
    @Operation(summary = "Rời phòng")
    public ResponseEntity<Void> leave(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                      @PathVariable String roomCode) {
        roomService.leave(roomCode, current);
        return ResponseEntity.noContent().build();
    }
}
