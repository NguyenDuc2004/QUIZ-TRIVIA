package com.datn.quizai.admin.service;

import com.datn.quizai.admin.dto.LiveRoomResponse;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.realtime.domain.GameRoom;
import com.datn.quizai.realtime.domain.RoomState;
import com.datn.quizai.realtime.domain.RoomStatus;
import com.datn.quizai.realtime.repository.GameRoomRepository;
import com.datn.quizai.realtime.service.RoomStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Giám sát và can thiệp vào phòng đấu đang chạy (features/10, FR-81 và FR-82).
 * <p>
 * Chức năng này tồn tại vì phòng đấu là phần duy nhất của hệ thống có <b>trạng thái sống ở hai nơi</b>:
 * metadata bền ở PostgreSQL, còn trạng thái đang chơi ở Redis kèm TTL. Khi hai nơi lệch nhau — bản ghi
 * còn mà trạng thái Redis đã hết hạn — phòng đó "treo": nó hiện trong danh sách nhưng không ai chơi
 * được. Không có trang này thì cách duy nhất để phát hiện là chờ người dùng báo.
 */
@Service
public class AdminRoomService {

    private static final Logger log = LoggerFactory.getLogger(AdminRoomService.class);

    private final GameRoomRepository roomRepository;
    private final RoomStateStore stateStore;

    public AdminRoomService(GameRoomRepository roomRepository, RoomStateStore stateStore) {
        this.roomRepository = roomRepository;
        this.stateStore = stateStore;
    }

    /**
     * Phòng chưa kết thúc, mới nhất trước.
     * <p>
     * Chỉ lấy {@code WAITING} và {@code PLAYING}: phòng đã {@code FINISHED} thuộc lịch sử, không phải
     * thứ cần giám sát. Ghép thêm trạng thái Redis cho từng phòng — số phòng đang mở luôn nhỏ (vài
     * chục) nên đây không phải vấn đề hiệu năng.
     */
    @Transactional(readOnly = true)
    public List<LiveRoomResponse> liveRooms() {
        return roomRepository.findAll().stream()
                .filter(room -> room.getStatus() == RoomStatus.WAITING
                        || room.getStatus() == RoomStatus.PLAYING)
                .sorted(Comparator.comparing(GameRoom::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    private LiveRoomResponse toResponse(GameRoom room) {
        Optional<RoomState> state = stateStore.find(room.getRoomCode());

        // Bản ghi còn mà trạng thái Redis mất = phòng treo. Đây là thông tin quan trọng nhất của trang
        // này, nên tính thành một cờ riêng thay vì để quản trị viên tự suy từ việc số người chơi bị rỗng.
        boolean treo = state.isEmpty();

        return new LiveRoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getHost() == null ? "?" : room.getHost().getDisplayName(),
                room.getQuiz() == null ? "?" : room.getQuiz().getTitle(),
                room.getStatus().name(),
                state.map(s -> s.players().size()).orElse(null),
                state.filter(s -> s.status() == RoomStatus.PLAYING)
                        .map(s -> s.currentIndex() + 1)   // currentIndex là 0-based
                        .orElse(null),
                state.map(RoomState::totalQuestions).orElse(null),
                room.isAllowGuests(),
                room.getCreatedAt(),
                room.getStartedAt(),
                treo);
    }

    /**
     * Cưỡng chế đóng một phòng.
     * <p>
     * Xoá trạng thái ở Redis <b>và</b> chuyển bản ghi sang {@code FINISHED}. Làm một trong hai là chưa
     * đủ: chỉ xoá Redis thì bản ghi vẫn hiện trong danh sách phòng đang mở; chỉ đổi trạng thái bản ghi
     * thì người đang kết nối vẫn còn trạng thái ở Redis và tiếp tục chơi.
     * <p>
     * Không xoá {@code game_rooms}: điểm cuối ván của những người đã chơi nằm ở
     * {@code game_room_players} tham chiếu tới nó.
     */
    @Transactional
    public void forceClose(String roomCode, java.util.UUID adminId) {
        GameRoom room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng " + roomCode));

        if (room.getStatus() == RoomStatus.FINISHED) {
            throw BusinessException.badRequest("Phòng này đã kết thúc rồi");
        }

        stateStore.delete(roomCode);
        room.setStatus(RoomStatus.FINISHED);
        room.setFinishedAt(OffsetDateTime.now());

        log.info("Quản trị {} cưỡng chế đóng phòng {} (trạng thái trước: {})",
                adminId, roomCode, room.getStatus());
    }
}
