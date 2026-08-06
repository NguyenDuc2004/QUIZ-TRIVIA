package com.datn.quizai.ai.domain;

/** Trạng thái job AI mà client hỏi qua {@code GET /ai/jobs/{id}}. */
public enum AiJobStatus {
    PENDING, RUNNING, SUCCEEDED, FAILED;

    public boolean isFinished() {
        return this == SUCCEEDED || this == FAILED;
    }
}
