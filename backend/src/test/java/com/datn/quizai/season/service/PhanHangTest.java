package com.datn.quizai.season.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phân hạng Đồng / Bạc / Vàng (features/15, FR-64).
 *
 * <h3>Mục này từng bị hoãn vì "chọn ngưỡng khi chưa có dữ liệu là số bịa"</h3>
 * Lý do đó đúng — với ngưỡng <b>điểm tuyệt đối</b>. Một con số như "1000 điểm là Vàng" không dựa trên gì, và
 * nó sai theo hai chiều cùng lúc: mùa ít người thì không ai đạt, mùa đông người thì ai cũng đạt.
 * <p>
 * Cách gỡ: tính theo <b>vị trí tương đối</b> trong chính mùa đó. Ngưỡng rút ra từ phân bố thật của người
 * chơi mùa ấy, thứ luôn tồn tại và luôn đúng với quy mô hiện có. Test ở đây chốt đúng tính chất đó.
 */
class PhanHangTest {

    @ParameterizedTest
    @CsvSource({
            // hạng, tổng, hạng mong đợi
            "1,   100, VANG",     // top 1%
            "10,  100, VANG",     // đúng mốc 10%
            "11,  100, BAC",      // vừa qua mốc
            "35,  100, BAC",      // đúng mốc 35%
            "36,  100, DONG",
            "100, 100, DONG",     // người cuối cùng
    })
    @DisplayName("Hạng theo VỊ TRÍ trong mùa, không theo điểm tuyệt đối")
    void shouldRankByRelativePosition(int thuHang, long tong, PhanHang mongDoi) {
        assertThat(PhanHang.cua(thuHang, tong)).isEqualTo(mongDoi);
    }

    @Test
    @DisplayName("Cùng một thứ hạng cho hạng KHÁC NHAU tuỳ quy mô mùa — đó chính là điểm mấu chốt")
    void sameRankDifferentTierBySeasonSize() {
        // Hạng 5 trong 10 người là nửa dưới bảng; hạng 5 trong 100 người là top 5%. Ngưỡng điểm tuyệt đối
        // không phân biệt được hai chuyện này, và đó là lý do nó bị bác.
        assertThat(PhanHang.cua(5, 10)).isEqualTo(PhanHang.DONG);
        assertThat(PhanHang.cua(5, 100)).isEqualTo(PhanHang.VANG);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 3, 9})
    @DisplayName("Mùa ít người thì KHÔNG phân hạng ai cả")
    void shouldNotRankTinySeasons(long tong) {
        // "Top 10% của 3 người" là câu vô nghĩa. Trao huy hiệu Vàng cho người đứng đầu trong ba người làm
        // mất giá đúng cái huy hiệu đó ở những mùa thật sự đông.
        assertThat(PhanHang.cua(1, tong)).isNull();
    }

    @Test
    @DisplayName("Đủ 10 người thì bắt đầu phân hạng")
    void shouldStartRankingAtThreshold() {
        assertThat(PhanHang.cua(1, PhanHang.TOI_THIEU_NGUOI)).isEqualTo(PhanHang.VANG);
        assertThat(PhanHang.cua(1, PhanHang.TOI_THIEU_NGUOI - 1)).isNull();
    }

    @Test
    @DisplayName("Chưa có điểm (hạng <= 0) thì không có hạng — khác hẳn hạng Đồng")
    void shouldReturnNullForNoScore() {
        // Người chưa chơi mùa này mà hiện "hạng Đồng" là nói sai về họ: Đồng nghĩa là đã tham gia và đang
        // ở nhóm dưới, còn chưa có điểm nghĩa là chưa tham gia.
        assertThat(PhanHang.cua(0, 100)).isNull();
        assertThat(PhanHang.cua(-1, 100)).isNull();
    }

    @Test
    @DisplayName("Người đứng cuối luôn là Đồng, không lọt lên nhóm trên vì làm tròn")
    void lastPlaceIsAlwaysBronze() {
        for (long tong : new long[]{10, 11, 37, 100, 1000}) {
            assertThat(PhanHang.cua((int) tong, tong))
                    .as("hạng cuối của mùa %d người", tong)
                    .isEqualTo(PhanHang.DONG);
        }
    }

    @Test
    @DisplayName("Mọi hạng đều có nhãn tiếng Việt để hiện thẳng lên giao diện")
    void everyTierHasLabel() {
        assertThat(PhanHang.VANG.nhan()).isEqualTo("Vàng");
        assertThat(PhanHang.BAC.nhan()).isEqualTo("Bạc");
        assertThat(PhanHang.DONG.nhan()).isEqualTo("Đồng");
    }
}
