package com.datn.quizai.admin.repository;

import com.datn.quizai.admin.dto.AiUsageSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Đọc tổng hợp từ {@code ai_request_logs} cho trang quản trị (features/10).
 * <p>
 * Dùng {@link JdbcTemplate} thay vì JPA vì đây là <b>truy vấn phân tích</b>, không phải CRUD thực thể:
 * kết quả là các con số gộp theo nhóm, không map về một hàng bảng nào. Map thành entity rồi gộp ở Java
 * sẽ tải toàn bộ nhật ký lên bộ nhớ để đếm — số bản ghi này chỉ tăng theo thời gian.
 */
@Repository
public class AiUsageRepository {

    private final JdbcTemplate jdbc;

    public AiUsageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param soNgay số ngày gần nhất cần tổng hợp; giới hạn cửa sổ thời gian để trang quản trị không
     *               chậm dần theo tuổi hệ thống, và vì chi phí AI là thứ người ta xem theo kỳ
     */
    public AiUsageSummary summary(int soNgay) {
        String window = " where created_at > now() - make_interval(days => ?) ";

        Map<String, Object> tong = jdbc.queryForMap("""
                select count(*)                                                        as tong,
                       count(*) filter (where status = 'SUCCESS')                       as thanh_cong,
                       count(*) filter (where status <> 'SUCCESS')                      as that_bai,
                       -- Lượt dùng dự phòng = lời gọi thành công bởi nhà cung cấp KHÔNG phải chính.
                       -- Đếm ở đây thay vì đếm số lần chuyển: một request có thể thử nhiều lần, còn
                       -- điều quản trị viên cần biết là "bao nhiêu kết quả đến từ đường dự phòng".
                       count(*) filter (where provider <> 'gemini' and status = 'SUCCESS') as du_phong,
                       coalesce(sum(tokens_in), 0)                                      as token_vao,
                       coalesce(sum(tokens_out), 0)                                     as token_ra,
                       round(avg(latency_ms))                                           as do_tre_tb,
                       percentile_disc(0.95) within group (order by latency_ms)          as do_tre_p95
                from ai_request_logs
                """ + window, soNgay);

        List<AiUsageSummary.TheoChucNang> theoChucNang = jdbc.query("""
                select feature,
                       count(*)                        as luot,
                       coalesce(sum(tokens_in), 0)     as token_vao,
                       coalesce(sum(tokens_out), 0)    as token_ra,
                       round(avg(latency_ms))          as do_tre_tb
                from ai_request_logs
                """ + window + """
                group by feature
                order by count(*) desc
                """,
                (rs, i) -> new AiUsageSummary.TheoChucNang(
                        rs.getString("feature"),
                        rs.getLong("luot"),
                        rs.getLong("token_vao"),
                        rs.getLong("token_ra"),
                        intOrNull(rs.getObject("do_tre_tb"))),
                soNgay);

        List<AiUsageSummary.TheoNhaCungCap> theoNhaCungCap = jdbc.query("""
                select provider,
                       count(*)                                    as luot,
                       count(*) filter (where status <> 'SUCCESS')  as that_bai
                from ai_request_logs
                """ + window + """
                group by provider
                order by count(*) desc
                """,
                (rs, i) -> new AiUsageSummary.TheoNhaCungCap(
                        rs.getString("provider"),
                        rs.getLong("luot"),
                        rs.getLong("that_bai")),
                soNgay);

        return new AiUsageSummary(
                asLong(tong.get("tong")),
                asLong(tong.get("thanh_cong")),
                asLong(tong.get("that_bai")),
                asLong(tong.get("du_phong")),
                asLong(tong.get("token_vao")),
                asLong(tong.get("token_ra")),
                intOrNull(tong.get("do_tre_tb")),
                intOrNull(tong.get("do_tre_p95")),
                theoChucNang,
                theoNhaCungCap);
    }

    private static long asLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    /**
     * Trả về {@code null} thay vì 0 khi chưa có dữ liệu: 0 ms là một con số có nghĩa (nhanh bất
     * thường), còn "chưa gọi lần nào" là không có số. Gộp hai thứ thành 0 làm giao diện hiển thị một
     * độ trễ không tồn tại.
     */
    private static Integer intOrNull(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }
}
