package com.datn.quizai.attempt.dto;

import com.datn.quizai.attempt.domain.AttemptMode;

/**
 * Tùy chọn khi bắt đầu làm bài. Body có thể bỏ trống — mặc định là chế độ thi.
 */
public record StartAttemptRequest(AttemptMode mode) {

    public AttemptMode modeOrDefault() {
        return mode == null ? AttemptMode.EXAM : mode;
    }
}
