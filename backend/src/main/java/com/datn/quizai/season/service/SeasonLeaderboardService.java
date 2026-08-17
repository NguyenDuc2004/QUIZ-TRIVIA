package com.datn.quizai.season.service;

import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.season.domain.Season;
import com.datn.quizai.season.domain.SeasonStatus;
import com.datn.quizai.season.repository.SeasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bảng xếp hạng mùa hiện tại (features/15, FR-61 và FR-62).
 *
 * <h3>Redis là chỉ mục, PostgreSQL là nguồn sự thật</h3>
 * Đặc tả gợi ý giữ điểm mùa trong Redis Sorted Set. Ở đây ZSET chỉ là <b>chỉ mục nhanh</b>: điểm thật của một
 * người là tổng {@code xp_events.xp} trong khoảng thời gian mùa. Lý do là Redis trong dự án chạy không bật
 * AOF — một lần restart mất dữ liệu là mất sạch bảng xếp hạng, và không có cách nào dựng lại nếu ZSET là nơi
 * duy nhất giữ điểm. Có {@code xp_events} thì {@link #dungLaiTuCoSoDuLieu} dựng lại toàn bộ bằng một câu SQL,
 * nên mất Redis chỉ là chậm một lần.
 * <p>
 * Nhờ vậy cũng không cần lo ZSET và cơ sở dữ liệu lệch nhau: lệch thì dựng lại, không phải đi đối chiếu.
 *
 * <h3>Chỉ có phạm vi toàn hệ thống</h3>
 * FR-62 nêu ba phạm vi: toàn hệ thống, theo lớp, theo bạn bè. Bản này chỉ làm <b>toàn hệ thống</b> vì hai
 * phạm vi kia phụ thuộc thứ chưa tồn tại: lớp học là features/14 (chưa làm), còn <i>bạn bè</i> không có ở bất
 * kỳ đâu trong toàn bộ docs — không có bảng, không có API, không có yêu cầu chức năng nào. Làm hai bộ lọc
 * luôn trả về cùng một danh sách chỉ để đủ ba tuỳ chọn là hứa với người dùng một thứ không có.
 */
@Service
public class SeasonLeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(SeasonLeaderboardService.class);

    /** Số người tối đa trả về mỗi lần. Bảng xếp hạng dài hơn thế không ai đọc, mà truyền thì tốn. */
    public static final int TOP_TOI_DA = 100;

    private final SeasonRepository seasonRepository;
    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;

    public SeasonLeaderboardService(SeasonRepository seasonRepository,
                                    StringRedisTemplate redis,
                                    JdbcTemplate jdbc) {
        this.seasonRepository = seasonRepository;
        this.redis = redis;
        this.jdbc = jdbc;
    }

    /** Một dòng trên bảng xếp hạng. */
    public record Dong(int rank, UUID userId, String displayName, String avatarUrl, int score) {
    }

    /** Khoá ZSET của một mùa. Công khai để test kiểm được hành vi khi Redis mất dữ liệu. */
    public static String key(UUID seasonId) {
        return "leaderboard:season:" + seasonId;
    }

    @Transactional(readOnly = true)
    public Season muaHienTai() {
        return seasonRepository.findByStatus(SeasonStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("Hiện không có mùa nào đang diễn ra"));
    }

    /**
     * Cộng điểm mùa cho một người.
     * <p>
     * Gọi từ {@code GamificationService} mỗi lần cộng XP, nên đã được chặn trùng ở tầng đó bằng
     * {@code xp_events}. {@code ZINCRBY} là thao tác nguyên tử của Redis — hai luồng cùng cộng vẫn đúng tổng.
     * <p>
     * Lỗi Redis <b>không</b> được làm vỡ việc cộng XP: XP đã ghi vào cơ sở dữ liệu rồi, và ZSET dựng lại được.
     */
    public void congDiem(UUID userId, int diem) {
        if (diem <= 0) {
            return;
        }
        try {
            Season mua = seasonRepository.findByStatus(SeasonStatus.ACTIVE).orElse(null);
            if (mua == null) {
                return;
            }
            redis.opsForZSet().incrementScore(key(mua.getId()), userId.toString(), diem);
        } catch (Exception e) {
            // Ghi log rồi đi tiếp. Không dựng lại ngay ở đây: một lỗi Redis lúc này thường là Redis đang
            // xuống, và thử dựng lại sẽ hỏng tiếp. Lần đọc bảng xếp hạng sau sẽ tự dựng lại nếu ZSET rỗng.
            log.warn("Không cộng được điểm mùa cho {} vào Redis: {}", userId, e.getMessage());
        }
    }

    /**
     * Top N của mùa hiện tại.
     * <p>
     * ZSET rỗng thì <b>tự dựng lại</b> từ {@code xp_events} rồi đọc tiếp. Đây là chỗ khiến việc mất Redis trở
     * thành một lần chậm chứ không phải mất dữ liệu.
     */
    @Transactional(readOnly = true)
    public List<Dong> top(int gioiHan) {
        Season mua = muaHienTai();
        String key = key(mua.getId());

        Long soLuong = redis.opsForZSet().zCard(key);
        if (soLuong == null || soLuong == 0) {
            dungLaiTuCoSoDuLieu(mua);
        }

        int n = Math.min(Math.max(gioiHan, 1), TOP_TOI_DA);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(key, 0, n - 1L);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        // Giữ đúng thứ tự Redis trả về (đã xếp theo điểm giảm dần), rồi nạp tên và avatar bằng MỘT truy vấn
        List<UUID> ids = new ArrayList<>();
        List<Integer> diems = new ArrayList<>();
        for (var t : tuples) {
            if (t.getValue() == null) {
                continue;
            }
            ids.add(UUID.fromString(t.getValue()));
            diems.add(t.getScore() == null ? 0 : t.getScore().intValue());
        }
        var nguoiDung = napNguoiDung(ids);

        List<Dong> ketQua = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            var nd = nguoiDung.get(ids.get(i));
            ketQua.add(new Dong(i + 1, ids.get(i),
                    nd == null ? "Người dùng đã xoá" : nd.ten(),
                    nd == null ? null : nd.avatar(),
                    diems.get(i)));
        }
        return ketQua;
    }

    /**
     * Thứ hạng của một người trong mùa hiện tại.
     *
     * @return {@code null} khi người này chưa có điểm nào trong mùa — khác hẳn với "hạng cuối". Giao diện cần
     *         phân biệt để nói "bạn chưa có điểm mùa này" thay vì hiện một con số hạng sai
     */
    @Transactional(readOnly = true)
    public Dong thuHangCuaToi(UUID userId) {
        Season mua = muaHienTai();
        String key = key(mua.getId());

        Long soLuong = redis.opsForZSet().zCard(key);
        if (soLuong == null || soLuong == 0) {
            dungLaiTuCoSoDuLieu(mua);
        }

        Double diem = redis.opsForZSet().score(key, userId.toString());
        Long hang = redis.opsForZSet().reverseRank(key, userId.toString());
        if (diem == null || hang == null) {
            return null;
        }
        var nd = napNguoiDung(List.of(userId)).get(userId);
        return new Dong(hang.intValue() + 1, userId,
                nd == null ? "?" : nd.ten(), nd == null ? null : nd.avatar(), diem.intValue());
    }

    /** Tổng số người có điểm trong mùa hiện tại. */
    @Transactional(readOnly = true)
    public long soNguoiThamGia() {
        Long n = redis.opsForZSet().zCard(key(muaHienTai().getId()));
        return n == null ? 0 : n;
    }

    /**
     * Dựng lại ZSET từ {@code xp_events} trong khoảng thời gian mùa.
     * <p>
     * Đây là lý do điểm mùa không lưu ở Redis: câu SQL này là <b>định nghĩa</b> của điểm mùa, còn ZSET chỉ là
     * bản sao để đọc nhanh. Gọi được bất cứ lúc nào mà kết quả không đổi.
     */
    public void dungLaiTuCoSoDuLieu(Season mua) {
        List<Object[]> rows = jdbc.query("""
                select user_id::text, sum(xp)::int
                from xp_events
                where created_at >= ? and created_at < ?
                group by user_id
                """,
                (rs, i) -> new Object[]{rs.getString(1), rs.getInt(2)},
                mua.getStartAt(), mua.getEndAt());

        if (rows.isEmpty()) {
            return;
        }
        try {
            String key = key(mua.getId());
            Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
            for (Object[] row : rows) {
                tuples.add(ZSetOperations.TypedTuple.of((String) row[0], ((Integer) row[1]).doubleValue()));
            }
            // Ghi một lần bằng ZADD nhiều thành viên thay vì lặp từng người: dựng lại là lúc Redis vừa trống,
            // và hàng nghìn lượt đi vòng mạng ở đúng lúc đó là cách chậm nhất có thể chọn.
            redis.opsForZSet().add(key, tuples);
            log.info("Đã dựng lại bảng xếp hạng mùa {} từ cơ sở dữ liệu: {} người", mua.getName(), rows.size());
        } catch (Exception e) {
            log.warn("Không dựng lại được bảng xếp hạng mùa {}: {}", mua.getName(), e.getMessage());
        }
    }

    private record NguoiDung(String ten, String avatar) {
    }

    /** Nạp tên và avatar của nhiều người bằng một truy vấn. */
    private java.util.Map<UUID, NguoiDung> napNguoiDung(List<UUID> ids) {
        if (ids.isEmpty()) {
            return java.util.Map.of();
        }
        String cho = String.join(",", ids.stream().map(x -> "?").toList());
        java.util.Map<UUID, NguoiDung> ketQua = new java.util.HashMap<>();
        // So `id::text` chứ không `id`: tham số truyền vào là chuỗi, và PostgreSQL không suy được kiểu cho
        // `uuid in (?,?)` — cùng họ với cái bẫy `lower(bytea)` đã gặp. Cách khác là cast từng tham số
        // `?::uuid`, nhưng như vậy phải sinh chuỗi placeholder phức tạp hơn mà không được gì thêm.
        jdbc.query("select id::text, display_name, avatar_url from users where id::text in (" + cho + ")",
                rs -> {
                    ketQua.put(UUID.fromString(rs.getString(1)),
                            new NguoiDung(rs.getString(2), rs.getString(3)));
                },
                ids.stream().map(UUID::toString).toArray());
        return ketQua;
    }
}
