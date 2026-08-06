package com.datn.quizai.ai.domain;

/** Vòng đời một tài liệu học liệu. */
public enum MaterialStatus {
    /** Đang trích text, cắt đoạn và sinh embedding ở job nền. */
    PROCESSING,
    /** Đã có embedding, dùng được để sinh đề. */
    READY,
    /** Xử lý hỏng — xem `error_message` để biết lý do. */
    FAILED
}
