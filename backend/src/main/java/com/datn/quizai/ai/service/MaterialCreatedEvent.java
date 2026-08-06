package com.datn.quizai.ai.service;

import java.util.UUID;

/**
 * Phát ra khi một học liệu vừa được tạo, để job nền bắt đầu nạp vào kho vector.
 * <p>
 * Dùng sự kiện thay vì gọi thẳng phương thức {@code @Async} là để chờ <b>transaction commit
 * xong</b> mới chạy: gọi thẳng thì luồng nền khởi động ngay trong lúc transaction tạo học liệu
 * còn chưa commit, và nó đọc CSDL không thấy dòng nào — lỗi {@code No value present}.
 */
public record MaterialCreatedEvent(UUID materialId, String rawText, UUID ownerId) {
}
