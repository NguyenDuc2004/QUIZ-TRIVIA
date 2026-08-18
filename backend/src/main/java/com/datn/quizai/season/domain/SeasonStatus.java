package com.datn.quizai.season.domain;

/** Trạng thái mùa giải (features/15, FR-60). */
public enum SeasonStatus {
    /** Đang nhận điểm. Chỉ có đúng một mùa ở trạng thái này — chỉ mục `uk_seasons_one_active` bảo đảm. */
    ACTIVE,
    /** Đã chốt: bảng xếp hạng lưu vào `season_rankings`, phần thưởng đã trao. */
    ENDED
}
