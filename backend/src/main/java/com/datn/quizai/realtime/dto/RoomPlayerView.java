package com.datn.quizai.realtime.dto;

import com.datn.quizai.realtime.domain.RoomState;

import java.util.UUID;

/**
 * Một người chơi trên thẻ phòng chờ và trên bảng xếp hạng trực tiếp.
 *
 * @param avatarEmoji kèm sẵn emoji và màu để frontend vẽ ngay, khỏi phải tra bảng avatar
 */
public record RoomPlayerView(int rank, UUID playerId, String displayName, boolean guest,
                             boolean ready, String avatar, String avatarEmoji, String avatarColor,
                             int score, int correctCount) {

    public static RoomPlayerView of(int rank, RoomState.PlayerState player) {
        return new RoomPlayerView(rank, player.playerId(), player.displayName(), player.guest(),
                player.ready(),
                player.avatar() == null ? null : player.avatar().name(),
                player.avatar() == null ? null : player.avatar().emoji(),
                player.avatar() == null ? null : player.avatar().color(),
                player.score(), player.correctCount());
    }
}
