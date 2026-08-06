package com.datn.quizai.file.dto;

/**
 * Kết quả tải file lên.
 *
 * @param url         đường dẫn công khai để nhúng vào {@code <img src>} hoặc lưu vào CSDL
 * @param fileName    tên file do server sinh (không phải tên client gửi lên)
 * @param size        dung lượng theo byte
 * @param contentType kiểu MIME <b>dò được từ nội dung file</b>, không phải kiểu client khai
 */
public record UploadedFileResponse(String url, String fileName, long size, String contentType) {
}
