package com.datn.quizai.realtime.service;

import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.integrity.domain.RoomProctoringType;
import com.datn.quizai.integrity.service.RoomFlagDetector;
import com.datn.quizai.realtime.domain.RoomParticipant;
import com.datn.quizai.realtime.domain.RoomProctoringEvent;
import com.datn.quizai.realtime.domain.RoomState;
import com.datn.quizai.realtime.dto.GameEvent;
import com.datn.quizai.realtime.dto.GameEventType;
import com.datn.quizai.realtime.dto.ProctoringFlagView;
import com.datn.quizai.realtime.dto.ProctoringWarningView;
import com.datn.quizai.realtime.dto.RoomProctoringPlayerSummary;
import com.datn.quizai.realtime.repository.GameRoomRepository;
import com.datn.quizai.realtime.repository.RoomProctoringEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cảnh báo gian lận <b>trực tiếp</b> trong phòng đấu (features/12, phần cảnh báo live).
 * <p>
 * Ba việc: nhận tín hiệu hành vi từ người chơi, gửi cờ đỏ riêng cho host khi đủ khuôn lặp, và chuyển lời
 * nhắc của host tới đúng một người chơi.
 *
 * <h3>Quyền của host dừng ở "nhắc"</h3>
 * Không có trừ điểm, không buộc nút đuổi vào tín hiệu hành vi. Ở màn rà soát sau bài thi cá nhân, giáo viên
 * có <i>thời gian</i>: đọc chuỗi tín hiệu, cân nhắc hoàn cảnh, hỏi lại học sinh, và quyết định lùi lại được.
 * Giữa phòng đấu thì host có ba giây, đang lo điều hành, trên một tín hiệu vẫn giả mạo được. Một thông báo
 * bật lên → cờ đỏ → người chơi bị đuổi khỏi cuộc thi tính điểm, không hoàn tác, không được nói gì.
 * <p>
 * Đuổi khỏi phòng vẫn nên có, nhưng cho việc khác (phá phòng, biệt danh bậy) — đó là việc của features/04.
 */
@Service
public class RoomProctoringService {

    private static final Logger log = LoggerFactory.getLogger(RoomProctoringService.class);

    /**
     * Chặn trên số tín hiệu ghi cho mỗi người trong mỗi phòng.
     * <p>
     * Vượt mức này thì tín hiệu bị bỏ im lặng, <b>không</b> báo lỗi về client: người chơi không nên thấy một
     * thông báo lỗi vì cơ chế giám sát đã đủ dữ liệu. Mất tín hiệu chỉ làm cờ đến muộn hơn — chấp nhận được,
     * vì tới lúc chạm mức này thì khuôn lặp đã hình thành từ lâu.
     */
    private static final int TOI_DA_MOI_NGUOI = 200;

    private final RoomProctoringEventRepository eventRepository;
    private final GameRoomRepository roomRepository;
    private final RoomStateStore stateStore;
    private final GameEventPublisher publisher;

    public RoomProctoringService(RoomProctoringEventRepository eventRepository,
                                 GameRoomRepository roomRepository,
                                 RoomStateStore stateStore,
                                 GameEventPublisher publisher) {
        this.eventRepository = eventRepository;
        this.roomRepository = roomRepository;
        this.stateStore = stateStore;
        this.publisher = publisher;
    }

    /**
     * Ghi một tín hiệu và gửi cờ cho host nếu đã đủ khuôn lặp.
     *
     * @param participant danh tính lấy từ phiên STOMP, <b>không</b> từ payload — nên không ai gửi được tín
     *                    hiệu thay cho người khác
     */
    @Transactional
    public void ghiNhan(String roomCode, RoomParticipant participant, RoomProctoringType loai) {
        RoomState state = stateStore.require(roomCode);

        // Người không ở trong phòng thì không có tín hiệu nào để ghi. Bỏ im lặng thay vì ném lỗi: đây có thể
        // là tín hiệu cuối của người vừa rời phòng, một trường hợp bình thường chứ không phải tấn công.
        if (!state.hasPlayer(participant.playerId())) {
            return;
        }
        // Host tự theo dõi mình thì vô nghĩa: cờ về chính host sẽ hiện trên màn hình của host.
        if (participant.playerId().equals(state.hostId())) {
            return;
        }
        UUID roomId = roomRepository.findIdByRoomCode(roomCode)
                .orElseThrow(() -> BusinessException.notFound("Phòng không tồn tại"));

        List<RoomProctoringEvent> daCo =
                eventRepository.findByRoomIdAndPlayerIdOrderByOccurredAtAsc(roomId, participant.playerId());
        if (daCo.size() >= TOI_DA_MOI_NGUOI) {
            return;
        }

        String tenTrongPhong = tenTrongPhong(state, participant.playerId());
        RoomProctoringEvent moi = new RoomProctoringEvent(roomId, participant.playerId(), tenTrongPhong,
                participant.guest(), loai, state.currentIndex(), OffsetDateTime.now());
        eventRepository.save(moi);

        // Đánh giá trên danh sách đã có CỘNG tín hiệu vừa tới. Không đọc lại từ cơ sở dữ liệu: `save()` chưa
        // flush nên lần đọc thứ hai trong cùng transaction có thể chưa thấy dòng mới, và cờ sẽ trễ một nhịp.
        List<RoomFlagDetector.TinHieu> chuoi = new ArrayList<>(daCo.size() + 1);
        for (RoomProctoringEvent e : daCo) {
            chuoi.add(new RoomFlagDetector.TinHieu(e.getEventType(), e.getQuestionIndex()));
        }
        chuoi.add(new RoomFlagDetector.TinHieu(loai, state.currentIndex()));

        RoomFlagDetector.KetQua co = RoomFlagDetector.danhGia(chuoi);
        if (!co.biGanCo()) {
            return;
        }
        publisher.toUser(roomCode, state.hostId(), GameEvent.of(GameEventType.PROCTORING_FLAG,
                new ProctoringFlagView(participant.playerId(), tenTrongPhong, participant.guest(),
                        co.soCauLap(), co.lyDo())));
    }

    /**
     * Host nhắc riêng một người chơi.
     * <p>
     * Lời nhắc đi qua {@code toUser} tới đúng người đó. Phòng đấu chỉ có một kênh phát chung mà mọi người
     * chơi đều subscribe, nên đẩy lời nhắc lên đó là công bố tên người bị nghi cho cả phòng.
     */
    public void nhacRieng(String roomCode, RoomParticipant nguoiGui, UUID playerId) {
        RoomState state = stateStore.require(roomCode);
        if (!nguoiGui.playerId().equals(state.hostId())) {
            throw BusinessException.forbidden("Chỉ chủ phòng được nhắc người chơi");
        }
        if (playerId == null || !state.hasPlayer(playerId)) {
            throw BusinessException.notFound("Người chơi không còn trong phòng");
        }
        publisher.toUser(roomCode, playerId,
                GameEvent.of(GameEventType.PROCTORING_WARNING, ProctoringWarningView.macDinh()));

        log.info("Host {} nhắc người chơi {} ở phòng {}", nguoiGui.playerId(), playerId, roomCode);
    }

    /**
     * Bản tổng kết cho host xem <b>sau ván</b>, gộp theo người chơi.
     * <p>
     * Chỉ host xem được. Đây là lý do bảng {@code room_proctoring_events} tồn tại thay vì chỉ giữ tín hiệu
     * trong bộ nhớ: giữa ván host đang lo điều hành, cờ hiện ra rồi trôi đi mà không kịp đọc.
     */
    @Transactional(readOnly = true)
    public List<RoomProctoringPlayerSummary> tongKet(String roomCode, UUID nguoiXemId) {
        RoomState state = stateStore.find(roomCode).orElse(null);
        UUID hostId = state != null
                ? state.hostId()
                : roomRepository.findByRoomCode(roomCode)
                        .orElseThrow(() -> BusinessException.notFound("Phòng không tồn tại"))
                        .getHost().getId();

        if (!hostId.equals(nguoiXemId)) {
            throw BusinessException.forbidden("Chỉ chủ phòng xem được tổng kết này");
        }
        UUID roomId = roomRepository.findIdByRoomCode(roomCode)
                .orElseThrow(() -> BusinessException.notFound("Phòng không tồn tại"));

        Map<UUID, List<RoomProctoringEvent>> theoNguoi = new LinkedHashMap<>();
        for (RoomProctoringEvent e : eventRepository.findByRoomIdOrderByOccurredAtAsc(roomId)) {
            theoNguoi.computeIfAbsent(e.getPlayerId(), k -> new ArrayList<>()).add(e);
        }

        List<RoomProctoringPlayerSummary> ketQua = new ArrayList<>();
        theoNguoi.forEach((playerId, cacTinHieu) -> {
            List<RoomFlagDetector.TinHieu> chuoi = cacTinHieu.stream()
                    .map(e -> new RoomFlagDetector.TinHieu(e.getEventType(), e.getQuestionIndex()))
                    .toList();
            RoomProctoringEvent dau = cacTinHieu.getFirst();
            // Cùng một hàm quyết định với cờ trực tiếp giữa ván. Đếm lại ở đây thì hai màn hình sẽ nói khác
            // nhau ngay lần đầu ai đó sửa ngưỡng.
            RoomFlagDetector.KetQua danhGia = RoomFlagDetector.danhGia(chuoi);

            ketQua.add(new RoomProctoringPlayerSummary(playerId, dau.getPlayerName(), dau.isGuest(),
                    (int) cacTinHieu.stream()
                            .filter(e -> e.getEventType() == RoomProctoringType.TAB_HIDDEN).count(),
                    danhGia.soCauLap(),
                    danhGia.biGanCo()));
        });

        // Người bị gắn cờ lên đầu, rồi tới người rời trang nhiều nhất: host mở bản này ra để tìm ai đáng hỏi,
        // không phải để đọc hết danh sách.
        ketQua.sort(Comparator.comparing(RoomProctoringPlayerSummary::biGanCo).reversed()
                .thenComparing(Comparator.comparingInt(RoomProctoringPlayerSummary::soLanRoiTrang).reversed()));
        return ketQua;
    }

    /**
     * Tên hiển thị lấy từ trạng thái phòng, không lấy từ {@link RoomParticipant#displayName()} — trường đó
     * là null với thành viên đã đăng nhập.
     */
    private String tenTrongPhong(RoomState state, UUID playerId) {
        return state.players().stream()
                .filter(p -> p.playerId().equals(playerId))
                .map(RoomState.PlayerState::displayName)
                .findFirst()
                .orElse(null);
    }
}
