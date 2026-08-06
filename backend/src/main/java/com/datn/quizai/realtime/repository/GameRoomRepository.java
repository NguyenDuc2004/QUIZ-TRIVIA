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
