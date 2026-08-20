package com.datn.quizai.realtime.repository;

import com.datn.quizai.realtime.domain.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GameRoomRepository extends JpaRepository<GameRoom, UUID> {

    boolean existsByRoomCode(String roomCode);

    /** Nạp phòng kèm host và quiz — đủ để dựng DTO thông tin phòng mà không sinh thêm truy vấn. */
    @Query("""
            select r from GameRoom r
              join fetch r.host
              join fetch r.quiz q
              join fetch q.owner
            where r.roomCode = :roomCode
            """)
    Optional<GameRoom> findByRoomCode(@Param("roomCode") String roomCode);

    /**
     * Chỉ lấy id từ mã phòng.
     * <p>
     * {@link #findByRoomCode} join-fetch cả host, quiz và chủ quiz — đúng cho màn thông tin phòng, nhưng
     * quá nhiều cho đường ghi tín hiệu hành vi vốn chỉ cần khoá ngoại. Tín hiệu tới liên tục suốt ván nên
     * chênh lệch này cộng dồn.
     */
    @Query("select r.id from GameRoom r where r.roomCode = :roomCode")
    Optional<UUID> findIdByRoomCode(@Param("roomCode") String roomCode);

    /**
     * Nạp phòng kèm danh sách người chơi — dùng khi kết thúc ván để ghi điểm cuối.
     * Chỉ fetch một collection nên không vướng MultipleBagFetchException.
     */
    @Query("""
            select distinct r from GameRoom r
              join fetch r.host
              join fetch r.quiz
              left join fetch r.players p
              left join fetch p.user
            where r.roomCode = :roomCode
            """)
    Optional<GameRoom> findByRoomCodeWithPlayers(@Param("roomCode") String roomCode);
}
