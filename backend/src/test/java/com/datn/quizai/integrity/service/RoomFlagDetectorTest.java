package com.datn.quizai.integrity.service;

import com.datn.quizai.integrity.domain.RoomProctoringType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.datn.quizai.integrity.domain.RoomProctoringType.TAB_HIDDEN;
import static com.datn.quizai.integrity.domain.RoomProctoringType.TAB_VISIBLE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm khuôn lặp của cảnh báo live trong phòng đấu (features/12).
 * <p>
 * Phần lớn test ở đây kiểm <b>khi nào KHÔNG gắn cờ</b>. Đó là chủ ý: một cờ thiếu chỉ làm host không biết,
 * còn một cờ oan làm một người chơi bị nghi giữa cuộc thi trước mặt người điều hành. Hai loại sai này không
 * cùng giá.
 */
class RoomFlagDetectorTest {

    private static RoomFlagDetector.TinHieu th(RoomProctoringType loai, int cau) {
        return new RoomFlagDetector.TinHieu(loai, cau);
    }

    @Test
    @DisplayName("Không tín hiệu nào thì không có cờ")
    void khongTinHieu() {
        assertThat(RoomFlagDetector.danhGia(List.of()).biGanCo()).isFalse();
        assertThat(RoomFlagDetector.danhGia(null).biGanCo()).isFalse();
        assertThat(RoomFlagDetector.danhGia(null).soCauLap()).isZero();
    }

    @Test
    @DisplayName("Rời rồi về ở MỘT câu: chưa gắn cờ — một lần là ngẫu nhiên")
    void motCauThiChuaGanCo() {
        RoomFlagDetector.KetQua kq = RoomFlagDetector.danhGia(List.of(
                th(TAB_HIDDEN, 0),
                th(TAB_VISIBLE, 0)));

        assertThat(kq.biGanCo()).isFalse();
        // Vẫn đếm được 1 câu: bản tổng kết sau ván cần con số này để hiện cả người chưa đủ khuôn
        assertThat(kq.soCauLap()).isEqualTo(1);
        assertThat(kq.lyDo()).isNull();
    }

    @Test
    @DisplayName("Rời rồi về ở HAI câu khác nhau: gắn cờ, và lý do nói rõ số câu")
    void haiCauThiGanCo() {
        RoomFlagDetector.KetQua kq = RoomFlagDetector.danhGia(List.of(
                th(TAB_HIDDEN, 0), th(TAB_VISIBLE, 0),
                th(TAB_HIDDEN, 1), th(TAB_VISIBLE, 1)));

        assertThat(kq.biGanCo()).isTrue();
        assertThat(kq.soCauLap()).isEqualTo(2);
        assertThat(kq.lyDo()).contains("2 câu");
    }

    @Test
    @DisplayName("Rời ở nhiều câu nhưng KHÔNG quay lại câu nào: không gắn cờ — đây là người bị gián đoạn thật")
    void chiRoiKhongVeThiKhongGanCo() {
        // Người nghe một cuộc gọi 30 giây: lúc quay lại thì server đã sang câu sau, nên tín hiệu TAB_VISIBLE
        // của họ mang số câu KHÁC. Mỗi câu chỉ có một nửa cặp.
        RoomFlagDetector.KetQua kq = RoomFlagDetector.danhGia(List.of(
                th(TAB_HIDDEN, 0),
                th(TAB_VISIBLE, 1),
                th(TAB_HIDDEN, 2),
                th(TAB_VISIBLE, 3),
                th(TAB_HIDDEN, 4),
                th(TAB_VISIBLE, 5)));

        assertThat(kq.biGanCo()).isFalse();
        assertThat(kq.soCauLap()).isZero();
    }

    @Test
    @DisplayName("Rời rồi về NHIỀU LẦN trong cùng một câu: vẫn chỉ là một câu, không gắn cờ")
    void nhieuLanTrongMotCauVanLaMotCau() {
        // Đây là điểm khác then chốt với bài thi cá nhân: ở đó ngưỡng là "chuyển tab 3 lần" nên chuỗi này bị
        // gắn cờ. Ở phòng đấu, ba lần trong một câu vẫn có thể là một cuộc gọi đến dai dẳng.
        RoomFlagDetector.KetQua kq = RoomFlagDetector.danhGia(List.of(
                th(TAB_HIDDEN, 2), th(TAB_VISIBLE, 2),
                th(TAB_HIDDEN, 2), th(TAB_VISIBLE, 2),
                th(TAB_HIDDEN, 2), th(TAB_VISIBLE, 2),
                th(TAB_HIDDEN, 2), th(TAB_VISIBLE, 2)));

        assertThat(kq.biGanCo()).isFalse();
        assertThat(kq.soCauLap()).isEqualTo(1);
    }

    @Test
    @DisplayName("TAB_VISIBLE đứng trước TAB_HIDDEN thì không thành khuôn")
    void quayLaiTruocKhiRoiThiKhongTinh() {
        // Người mở trang phòng đấu ở một tab đang ẩn sẵn: tín hiệu đầu tiên họ gửi là TAB_VISIBLE. Bỏ điều
        // kiện thứ tự đi thì họ bị tính oan ngay từ câu đầu.
        RoomFlagDetector.KetQua kq = RoomFlagDetector.danhGia(List.of(
                th(TAB_VISIBLE, 0),
                th(TAB_VISIBLE, 1)));

        assertThat(kq.soCauLap()).isZero();
        assertThat(kq.biGanCo()).isFalse();
    }

    @Test
    @DisplayName("Tín hiệu ở phòng chờ (chưa vào câu nào) không được tính")
    void phongChoKhongTinh() {
        RoomFlagDetector.KetQua kq = RoomFlagDetector.danhGia(List.of(
                th(TAB_HIDDEN, -1), th(TAB_VISIBLE, -1),
                th(TAB_HIDDEN, -1), th(TAB_VISIBLE, -1),
                th(TAB_HIDDEN, -1), th(TAB_VISIBLE, -1)));

        // Ở phòng chờ chưa có câu hỏi nào nên không có "còn thời gian trả lời" để mà tra cứu. Đếm nó là gắn
        // cờ cho người vào phòng sớm rồi đi làm việc khác trong lúc chờ host bấm bắt đầu.
        assertThat(kq.soCauLap()).isZero();
        assertThat(kq.biGanCo()).isFalse();
    }

    @Test
    @DisplayName("Chuỗi trộn: chỉ những câu có ĐỦ cặp được tính")
    void chuoiTronChiTinhCauDuCap() {
        List<RoomFlagDetector.TinHieu> chuoi = List.of(
                th(TAB_HIDDEN, 0), th(TAB_VISIBLE, 0),   // đủ cặp
                th(TAB_HIDDEN, 1),                        // chỉ rời, mất câu 1
                th(TAB_VISIBLE, 2),                       // quay lại ở câu 2 mà chưa từng rời câu 2
                th(TAB_HIDDEN, 3), th(TAB_VISIBLE, 3));   // đủ cặp

        // Kiểm ĐÚNG những câu nào được tính, không chỉ kiểm số lượng: đếm đúng 2 mà tính sai câu nào thì
        // assertion trên số lượng vẫn xanh, và lý do host đọc được sẽ chỉ tới câu không liên quan.
        assertThat(RoomFlagDetector.cauCoKhuonLap(chuoi)).containsExactly(0, 3);

        RoomFlagDetector.KetQua kq = RoomFlagDetector.danhGia(chuoi);
        assertThat(kq.soCauLap()).isEqualTo(2);
        assertThat(kq.biGanCo()).isTrue();
    }

    @Test
    @DisplayName("Ngưỡng là 2 câu — đổi hằng số thì test này phải đỏ")
    void nguongLaHaiCau() {
        // Chốt ngưỡng bằng test thay vì để nó trôi: 1 thì mọi người chơi điện thoại bị gắn cờ, 3 thì một ván
        // nhanh 3–5 câu gần như không bao giờ báo.
        assertThat(RoomFlagDetector.NGUONG_SO_CAU).isEqualTo(2);
    }
}
