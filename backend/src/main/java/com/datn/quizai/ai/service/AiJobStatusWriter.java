package com.datn.quizai.ai.service;

import com.datn.quizai.ai.repository.AiJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ghi trạng thái job AI trong transaction ngắn, tách khỏi {@link AiJobService}.
 * <p>
 * Là <b>bean riêng</b> vì hai lý do:
 * <ul>
 *   <li>Gọi {@code this.method()} trong cùng lớp không đi qua proxy nên {@code @Transactional}
 *       mất tác dụng — thay đổi trạng thái sẽ không bao giờ được ghi xuống.</li>
 *   <li>{@code REQUIRES_NEW} để mỗi lần đổi trạng thái commit ngay: client hỏi giữa chừng phải
 *       thấy được {@code RUNNING}, chứ không phải chờ tới lúc job xong mới thấy đổi.</li>
 * </ul>
 */
@Service
public class AiJobStatusWriter {

    private final AiJobRepository repository;

    public AiJobStatusWriter(AiJobRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRunning(UUID jobId) {
        repository.findById(jobId).ifPresent(job -> job.markRunning());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSucceeded(UUID jobId, String result) {
        repository.findById(jobId).ifPresent(job -> job.markSucceeded(result));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID jobId, String reason) {
        repository.findById(jobId).ifPresent(job -> job.markFailed(reason));
    }
}
