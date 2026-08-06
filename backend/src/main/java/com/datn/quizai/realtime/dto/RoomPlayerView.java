package com.datn.quizai.realtime.dto;

import com.datn.quizai.realtime.domain.RoomState;

import java.util.UUID;

/** Một người chơi trên bảng xếp hạng trực tiếp. */
public record RoomPlayerView(int rank, UUID userId, String displayName, int score, int correctCount) {

    public static RoomPlayerView of(int rank, RoomState.PlayerState player) {
        return new RoomPlayerView(rank, player.userId(), player.displayName(),
                player.score(), player.correctCount());
    }
}
