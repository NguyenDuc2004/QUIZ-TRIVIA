package com.datn.quizai.recommend.service;

import com.datn.quizai.recommend.repository.GraphWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Tạo ràng buộc duy nhất cho đồ thị gợi ý lúc ứng dụng đã sẵn sàng (docs/features/07).
 * <p>
 * Neo4j không có schema nên không có bước này thì {@code MERGE} vẫn chạy — chỉ là **quét toàn bộ
 * nút** mỗi lần, và đồ thị càng lớn càng chậm dần mà không có triệu chứng gì ngoài việc mọi thứ ì
 * đi. Ràng buộc duy nhất đồng thời là index, nên đây vừa là đúng đắn vừa là hiệu năng.
 * <p>
 * Chạy ở {@link ApplicationReadyEvent} chứ không phải lúc dựng bean: nếu Neo4j chưa lên thì việc
 * này hỏng, và <b>hỏng ở đây không được cản ứng dụng khởi động</b>. Gợi ý là tính năng phụ trợ —
 * không có nó thì trang chủ thiếu một khu, còn không khởi động được thì cả hệ thống chết.
 */
@Component
public class GraphSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(GraphSchemaInitializer.class);

    private final GraphWriter graphWriter;
    private final GraphSyncService graphSyncService;

    public GraphSchemaInitializer(GraphWriter graphWriter, GraphSyncService graphSyncService) {
        this.graphWriter = graphWriter;
        this.graphSyncService = graphSyncService;
    }

    /**
     * Chạy ở luồng nền: dựng danh mục có thể mất vài giây với ngân hàng quiz lớn, mà không việc gì
     * phải bắt ứng dụng chờ nó xong mới nhận request đầu tiên.
     */
    @Async("aiTaskExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void createConstraints() {
        try {
            graphWriter.ensureConstraints();
            log.info("Đồ thị gợi ý: đã có ràng buộc duy nhất cho User/Quiz/Topic");
            graphSyncService.syncPublicCatalog();
        } catch (Exception e) {
            log.warn("Chưa tạo được ràng buộc Neo4j ({}). Gợi ý sẽ chưa chạy cho tới khi Neo4j lên; "
                    + "phần còn lại của hệ thống không bị ảnh hưởng.", e.getMessage());
        }
    }
}
