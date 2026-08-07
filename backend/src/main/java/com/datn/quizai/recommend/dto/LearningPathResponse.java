package com.datn.quizai.recommend.dto;

import java.util.List;

/**
 * Lộ trình học đề xuất (FR-35): các chủ đề đã học xếp theo mức độ yếu, yếu nhất trước.
 *
 * @param topics    toàn bộ chủ đề đã làm, đã sắp thứ tự
 * @param weakCount số chủ đề đang bị coi là yếu
 * @param note      lời nhắn khi chưa đủ dữ liệu để nói gì có ích; null khi lộ trình có nghĩa.
 *                  Trả danh sách rỗng mà không giải thích thì người dùng tưởng hệ thống hỏng
 */
public record LearningPathResponse(
        List<TopicMasteryResponse> topics,
        long weakCount,
        String note
) {
}
