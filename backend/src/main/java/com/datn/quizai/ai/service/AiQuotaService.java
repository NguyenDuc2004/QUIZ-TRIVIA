package com.datn.quizai.ai.service;

import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Hạn mức số lượt gọi AI mỗi ngày cho từng người (features/10, FR-84).
 *
 * <h3>Vì sao tính năng này từng bị hoãn</h3>
 * Bản trước có thể làm một ô nhập hạn mức ở khu quản trị trong nửa ngày. Nhưng {@code AiOrchestrator} khi ấy
 * không đếm lượt gọi theo từng người, nên ô đó chỉ <b>lưu được một con số mà không chặn được gì</b> — quản
 * trị viên sẽ tin rằng chi phí đã bị giới hạn trong khi thực tế không. Đó là kiểu sai <i>tệ hơn</i> việc
 * thiếu tính năng: nó thay một khoảng trống nhìn thấy được bằng một sự an tâm sai.
 * <p>
 * Nên phần đếm và điểm chặn phải có trước; ô nhập chỉ là phần cuối.
 *
 * <h3>Đếm ở Redis, sự thật ở PostgreSQL</h3>
 * Bộ đếm tăng ở mỗi lời gọi AI — ghi vào PostgreSQL là một UPDATE cho mỗi lời gọi trên đúng một dòng mà
 * nhiều luồng cùng tranh. Nên đếm ở Redis, và <b>dựng lại từ {@code ai_request_logs} khi khoá chưa có</b>.
 * Redis ở dự án này chạy không bật AOF, nên một lần restart là mất bộ đếm; không dựng lại thì hạn mức của
 * cả hệ thống được reset về 0 mỗi lần khởi động lại. Cùng nguyên tắc đã dùng cho bảng xếp hạng mùa.
 *
 * <h3>Đếm LỜI GỌI CỦA NGƯỜI DÙNG, không đếm lần thử lại</h3>
 * Một lần sinh đề hỏng rồi thử lại 3 lần vẫn là <b>một</b> lượt của người dùng. Đếm từng lần thử thì hạn
 * mức phụ thuộc vào việc nhà cung cấp hôm nay có ổn định hay không — người dùng bị trừ lượt vì sự cố mà họ
 * không gây ra, và quản trị viên không đoán được "20 lượt" nghĩa là bao nhiêu việc.
 *
 * <h3>Nhúng học liệu KHÔNG tính vào hạn mức</h3>
 * Một tài liệu chia thành 50 đoạn là 50 lời gọi {@code embed} cho <b>một</b> hành động của người dùng. Tính
 * chúng vào thì hạn mức 20 lượt hết ngay ở tài liệu đầu tiên, và con số quản trị viên đặt không còn nghĩa
 * gì. Chi phí phần này được theo dõi qua FR-74 (bảng chi phí theo người), không qua hạn mức.
 */
@Service
public class AiQuotaService {

    private static final Logger log = LoggerFactory.getLogger(AiQuotaService.class);

    private static final String KEY_PREFIX = "aiquota:";

    /** Giữ khoá qua nửa đêm một chút để không mất bộ đếm ngay tại thời điểm chuyển ngày. */
    private static final Duration TTL = Duration.ofHours(26);

    private final StringRedisTemplate redis;
    private final UserRepository userRepository;
    private final AiRequestLogger requestLogger;
    private final int hanMucMacDinh;
    private final ZoneId muiGio;

    public AiQuotaService(StringRedisTemplate redis,
                          UserRepository userRepository,
                          AiRequestLogger requestLogger,
                          @Value("${app.ai.default-daily-quota:0}") int hanMucMacDinh,
                          @Value("${app.timezone:Asia/Ho_Chi_Minh}") String muiGio) {
        this.redis = redis;
        this.userRepository = userRepository;
        this.requestLogger = requestLogger;
        this.hanMucMacDinh = hanMucMacDinh;
        this.muiGio = ZoneId.of(muiGio);
    }

    /**
     * Hạn mức áp cho một người: giá trị riêng nếu có, không thì mặc định hệ thống.
     * <p>
     * {@code 0} nghĩa là <b>không giới hạn</b> khi đó là mặc định hệ thống (chưa ai bật tính năng), nhưng
     * nghĩa là <b>cấm</b> khi quản trị viên đặt riêng cho người đó. Hai nghĩa trái ngược của cùng một con
     * số nên phải phân biệt bằng <i>nguồn</i> của nó, và đó là lý do cột trên `users` để NULL được.
     */
    public int hanMucCua(UUID userId) {
        if (userId == null) {
            return 0;   // tác vụ hệ thống, không thuộc về ai — không chặn
        }
        Integer rieng = userRepository.findAiDailyQuotaById(userId).orElse(null);
        return rieng != null ? rieng : hanMucMacDinh;
    }

    /** Số lượt đã dùng hôm nay. Đọc Redis, dựng lại từ bảng audit nếu khoá chưa có. */
    public long daDungHomNay(UUID userId) {
        if (userId == null) {
            return 0;
        }
        String key = key(userId);
        String giaTri = redis.opsForValue().get(key);
        if (giaTri != null) {
            return Long.parseLong(giaTri);
        }
        return dungLai(userId, key);
    }

    /**
     * Kiểm hạn mức <b>mà KHÔNG ghi nhận lượt nào</b>.
     *
     * <h4>Vì sao cần một hàm chỉ-kiểm riêng</h4>
     * Tác vụ AI nặng chạy nền: {@code POST /ai/generate-questions} trả 202 kèm {@code jobId} ngay, còn lời
     * gọi mô hình xảy ra sau ở luồng nền. Nếu chỉ chốt hạn mức trong {@link #kiemTraVaGhiNhan} — nơi luồng
     * nền gọi tới — thì người đã hết lượt vẫn nhận <b>202</b> rồi mới thấy job hỏng.
     * <p>
     * Chi phí vẫn được khống chế đúng (mô hình không bị gọi), nhưng người dùng phải bấm rồi chờ rồi mới
     * biết mình hết lượt — và bấm mười lần thì tạo mười job hỏng. Nên chốt thêm ở lúc <i>nhận việc</i>.
     * <p>
     * <b>Không cộng lượt ở đây</b>: cộng cả lúc nhận việc lẫn lúc gọi mô hình là trừ đôi, và người dùng
     * mất một nửa hạn mức mà không hiểu vì sao.
     *
     * @throws BusinessException 429 khi đã hết lượt trong ngày
     */
    public void kiemTra(UUID userId) {
        HanMuc hm = hanMuc(userId);
        if (hm == null) {
            return;
        }
        if (daDungHomNay(userId) >= hm.gioiHan()) {
            throw hetLuot(hm.gioiHan());
        }
    }

    /**
     * Kiểm hạn mức <b>và</b> ghi nhận một lượt. Gọi một lần cho mỗi yêu cầu của người dùng.
     *
     * @throws BusinessException 429 khi đã hết lượt trong ngày
     */
    public void kiemTraVaGhiNhan(UUID userId) {
        HanMuc hm = hanMuc(userId);
        if (hm == null) {
            return;
        }
        int hanMuc = hm.gioiHan();

        String key = key(userId);
        if (redis.opsForValue().get(key) == null) {
            dungLai(userId, key);
        }

        Long sauKhiTang = redis.opsForValue().increment(key);
        redis.expire(key, TTL);

        if (sauKhiTang != null && sauKhiTang > hanMuc) {
            // Lùi lại lượt vừa cộng: yêu cầu này bị từ chối nên nó không được tính là đã dùng. Không lùi
            // thì mỗi lần bị chặn lại đẩy bộ đếm lên cao thêm, và con số hiện ở khu quản trị thành vô nghĩa.
            redis.opsForValue().decrement(key);
            throw hetLuot(hanMuc);
        }
    }

    /** Hạn mức đang áp; {@code null} = không chặn người này. */
    private record HanMuc(int gioiHan) {
    }

    /**
     * Quyết định hạn mức áp cho một người, hoặc {@code null} nếu không chặn.
     * <p>
     * Một chỗ duy nhất đọc cột và diễn giải ý nghĩa của nó, để {@link #kiemTra} và
     * {@link #kiemTraVaGhiNhan} không bao giờ hiểu khác nhau về cùng một con số.
     */
    private HanMuc hanMuc(UUID userId) {
        if (userId == null) {
            return null;   // tác vụ hệ thống, không thuộc về ai
        }
        Integer rieng = userRepository.findAiDailyQuotaById(userId).orElse(null);

        // `rieng == null && mặc định <= 0` = CHƯA BẬT hạn mức cho hệ thống → không chặn ai.
        // Còn `rieng == 0` = quản trị viên CẤM người này. Cùng con số 0, hai nghĩa trái ngược, phân biệt
        // bằng NGUỒN của nó — đó là lý do cột trên `users` để null được.
        if (rieng == null && hanMucMacDinh <= 0) {
            return null;
        }
        return new HanMuc(rieng != null ? rieng : hanMucMacDinh);
    }

    private BusinessException hetLuot(int hanMuc) {
        return new BusinessException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                "Bạn đã dùng hết " + hanMuc + " lượt AI của hôm nay. Hạn mức đặt lại vào 00:00.");
    }

    /**
     * Dựng lại bộ đếm từ {@code ai_request_logs} — nguồn sự thật.
     * <p>
     * Chạy khi khoá Redis chưa có: ngày mới, hoặc Redis vừa khởi động lại. Không có bước này thì một lần
     * restart Redis là xoá sạch hạn mức của cả hệ thống, mà không ai nhận ra.
     */
    private long dungLai(UUID userId, String key) {
        OffsetDateTime dauNgay = LocalDate.now(muiGio).atStartOfDay(muiGio).toOffsetDateTime();
        long daCo = requestLogger.demTuThoiDiem(userId, dauNgay);

        redis.opsForValue().set(key, String.valueOf(daCo), TTL);
        log.debug("Dựng lại bộ đếm hạn mức AI cho {}: {} lượt từ đầu ngày", userId, daCo);
        return daCo;
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.now(muiGio);
    }
}
