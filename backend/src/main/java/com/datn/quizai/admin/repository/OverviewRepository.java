package com.datn.quizai.admin.repository;

import com.datn.quizai.admin.dto.SystemOverview;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Map;

/**
 * Đọc số liệu tổng quan hệ thống cho trang đầu khu quản trị (features/10).
 * <p>
 * Dùng {@link JdbcTemplate}: đây là truy vấn phân tích trên nhiều bảng, kết quả là các con số gộp chứ
 * không map về thực thể nào. Cùng lý do như {@link AiUsageRepository}.
 * <p>
 * <b>Gộp các phép đếm vào ít câu truy vấn thay vì mỗi chỉ số một câu.</b> Trang này hiện 16 con số; mỗi
 * con số một lần đi vòng tới cơ sở dữ liệu là 16 lượt cho một lần mở trang, mà tất cả đều đọc cùng vài
 * bảng. Ba câu gộp theo nhóm bảng vừa nhanh hơn vừa cho số liệu nhất quán ở cùng một thời điểm.
 */
@Repository
public class OverviewRepository {

    private final JdbcTemplate jdbc;

    public OverviewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param days số ngày của biểu đồ tăng trưởng
     */
    public SystemOverview overview(int days) {
        Map<String, Object> nguoiDung = jdbc.queryForMap("""
                select count(*)                                              as tong,
                       count(*) filter (where role = 'LEARNER')              as nguoi_hoc,
                       count(*) filter (where role = 'CREATOR')              as nguoi_tao,
                       count(*) filter (where role = 'ADMIN')                as quan_tri,
                       count(*) filter (where locked)                        as bi_khoa,
                       count(*) filter (where created_at::date = current_date) as hom_nay
                from users
                """);

        Map<String, Object> noiDung = jdbc.queryForMap("""
                select (select count(*) from quizzes)                                as tong_quiz,
                       (select count(*) from quizzes where visibility = 'PUBLIC')    as quiz_cong_khai,
                       (select count(*) from questions)                             as tong_cau_hoi,
                       (select count(*) from learning_materials)                    as tong_hoc_lieu
                """);

        Map<String, Object> hoatDong = jdbc.queryForMap("""
                select (select count(*) from quiz_attempts)                              as tong_luot,
                       (select count(*) from quiz_attempts
                         where created_at::date = current_date)                          as luot_hom_nay,
                       (select count(*) from game_rooms where status = 'WAITING')        as phong_cho,
                       (select count(*) from game_rooms where status = 'PLAYING')        as phong_choi
                """);

        // Cắt theo tháng dương lịch, không phải "30 ngày gần nhất": hạn mức và chi phí của nhà cung cấp
        // cũng tính theo tháng, nên hai con số phải so sánh được với nhau
        Map<String, Object> ai = jdbc.queryForMap("""
                select count(*)                                              as luot,
                       coalesce(sum(tokens_in), 0) + coalesce(sum(tokens_out), 0) as token
                from ai_request_logs
                where date_trunc('month', created_at) = date_trunc('month', now())
                """);

        return new SystemOverview(
                asLong(nguoiDung.get("tong")),
                asLong(nguoiDung.get("nguoi_hoc")),
                asLong(nguoiDung.get("nguoi_tao")),
                asLong(nguoiDung.get("quan_tri")),
                asLong(nguoiDung.get("bi_khoa")),
                asLong(nguoiDung.get("hom_nay")),
                asLong(noiDung.get("tong_quiz")),
                asLong(noiDung.get("quiz_cong_khai")),
                asLong(noiDung.get("tong_cau_hoi")),
                asLong(noiDung.get("tong_hoc_lieu")),
                asLong(hoatDong.get("tong_luot")),
                asLong(hoatDong.get("luot_hom_nay")),
                asLong(hoatDong.get("phong_cho")),
                asLong(hoatDong.get("phong_choi")),
                asLong(ai.get("luot")),
                asLong(ai.get("token")),
                tangTruong(days),
                theoDanhMuc(),
                tiLeHoanThanh());
    }

    /**
     * Số người đăng ký và số lượt làm bài theo từng ngày.
     * <p>
     * Dùng {@code generate_series} để sinh đủ dãy ngày rồi {@code left join}: nếu chỉ {@code group by}
     * trên dữ liệu thật thì ngày không có hoạt động sẽ **biến mất khỏi kết quả**, và biểu đồ đường sẽ
     * nối thẳng qua khoảng trống — trông như hoạt động vẫn liên tục trong khi thực tế là đứng yên.
     */
    private List<SystemOverview.DiemTheoNgay> tangTruong(int days) {
        return jdbc.query("""
                with dai_ngay as (
                    select generate_series(current_date - make_interval(days => ? - 1),
                                           current_date, interval '1 day')::date as ngay
                )
                select d.ngay,
                       (select count(*) from users u
                         where u.created_at::date = d.ngay)          as nguoi_moi,
                       (select count(*) from quiz_attempts a
                         where a.created_at::date = d.ngay)          as luot_lam_bai
                from dai_ngay d
                order by d.ngay
                """,
                (rs, i) -> new SystemOverview.DiemTheoNgay(
                        rs.getObject("ngay", Date.class).toLocalDate(),
                        rs.getLong("nguoi_moi"),
                        rs.getLong("luot_lam_bai")),
                days);
    }

    private List<SystemOverview.PhanBoDanhMuc> theoDanhMuc() {
        return jdbc.query("""
                select coalesce(c.name, 'Chưa phân loại') as danh_muc, count(q.id) as so_quiz
                from quizzes q
                         left join categories c on c.id = q.category_id
                group by coalesce(c.name, 'Chưa phân loại')
                having count(q.id) > 0
                order by count(q.id) desc
                """,
                (rs, i) -> new SystemOverview.PhanBoDanhMuc(
                        rs.getString("danh_muc"), rs.getLong("so_quiz")));
    }

    /**
     * Số lượt làm bài theo trạng thái, kèm độ chính xác trung bình.
     * <p>
     * Độ chính xác chỉ tính trên bài <b>đã nộp</b> và có {@code max_score > 0}: bài đang làm dở chưa có
     * điểm, còn chia cho 0 thì ra vô nghĩa. Trả {@code null} thay vì 0 khi không có bài nào đủ điều kiện.
     */
    private List<SystemOverview.TiLeHoanThanh> tiLeHoanThanh() {
        return jdbc.query("""
                select status,
                       count(*) as so_luot,
                       round(avg(case when status = 'SUBMITTED' and max_score > 0
                                      then total_score * 100.0 / max_score end), 1) as do_chinh_xac
                from quiz_attempts
                group by status
                order by count(*) desc
                """,
                (rs, i) -> {
                    Object acc = rs.getObject("do_chinh_xac");
                    return new SystemOverview.TiLeHoanThanh(
                            rs.getString("status"),
                            rs.getLong("so_luot"),
                            acc == null ? null : ((Number) acc).doubleValue());
                });
    }

    private static long asLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
