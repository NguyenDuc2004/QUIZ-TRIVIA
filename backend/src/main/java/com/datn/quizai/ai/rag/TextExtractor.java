package com.datn.quizai.ai.rag;

import com.datn.quizai.common.exception.BusinessException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Trích văn bản thuần từ file học liệu bằng Apache Tika (docs/tech-stack.md §1).
 * <p>
 * Dùng {@code AutoDetectParser} để một đường xử lý được cả PDF, DOCX và TXT — Tika tự nhận dạng
 * theo nội dung, không dựa vào phần mở rộng do client đặt.
 */
@Component
public class TextExtractor {

    private static final Logger log = LoggerFactory.getLogger(TextExtractor.class);

    /**
     * Giới hạn ký tự trích ra. {@code BodyContentHandler(-1)} là không giới hạn, nhưng một file
     * vài trăm trang sẽ nuốt hết bộ nhớ và sinh ra hàng nghìn lần gọi embedding — vừa chậm vừa tốn.
     */
    private static final int MAX_CHARS = 500_000;

    public String extract(InputStream input, String fileName) {
        BodyContentHandler handler = new BodyContentHandler(MAX_CHARS);
        Metadata metadata = new Metadata();

        try {
            new AutoDetectParser().parse(input, handler, metadata, new ParseContext());
            String text = handler.toString();

            if (text.isBlank()) {
                throw BusinessException.badRequest(
                        "Không trích được chữ nào từ tài liệu. File ảnh scan cần OCR trước khi tải lên.");
            }

            log.info("Đã trích {} ký tự từ {} (kiểu {})",
                    text.length(), fileName, metadata.get(Metadata.CONTENT_TYPE));
            return text;

        } catch (SAXException e) {
            // Tika ném SAXException khi chạm trần MAX_CHARS — phần đã đọc vẫn dùng được
            String partial = handler.toString();
            if (!partial.isBlank()) {
                log.warn("Tài liệu {} dài quá {} ký tự, chỉ dùng phần đầu", fileName, MAX_CHARS);
                return partial;
            }
            throw BusinessException.badRequest("Không đọc được nội dung tài liệu");

        } catch (IOException | TikaException e) {
            log.warn("Tika không đọc được {}: {}", fileName, e.getMessage());
            throw BusinessException.badRequest("Không đọc được tài liệu: định dạng không hỗ trợ hoặc file hỏng");
        }
    }
}
