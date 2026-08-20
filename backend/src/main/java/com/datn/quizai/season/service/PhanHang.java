package com.datn.quizai.season.service;

/**
 * Phân hạng Đồng / Bạc / Vàng theo mùa (features/15, FR-64).
 *
 * <h3>Vì sao mục này từng bị hoãn, và cách làm này gỡ được nút thắt đó</h3>
 * Lý do hoãn ghi trong đặc tả: *"cần chọn ngưỡng, mà chọn ngưỡng khi chưa có dữ liệu thật thì chỉ là số
 * bịa"*. Đúng — nếu ngưỡng là <b>điểm tuyệt đối</b> ("1000 điểm là Vàng"), thì con số 1000 không dựa trên
 * gì cả, và nó sai theo hai chiều cùng lúc: mùa ít người thì không ai đạt, mùa đông người thì ai cũng đạt.
 * <p>
 * Nên hạng ở đây tính theo <b>vị trí tương đối</b> trong chính mùa đó. Ngưỡng không do ai nghĩ ra mà rút ra
 * từ phân bố thật của người chơi mùa ấy — thứ luôn tồn tại và luôn đúng với quy mô hiện có.
 *
 * <h3>Ít người thì KHÔNG phân hạng</h3>
 * "Top 10% của 3 người" là một câu vô nghĩa: người đứng đầu trong ba người không nói lên điều gì, và trao
 * cho họ huy hiệu Vàng làm mất giá đúng cái huy hiệu đó ở những mùa thật sự đông.
 * <p>
 * {@link #TOI_THIEU_NGUOI} cũng là một ngưỡng, nhưng nó khác hẳn về bản chất với ngưỡng bị bác ở trên: nó
 * nói về <i>khi nào một tỉ lệ phần trăm bắt đầu có nghĩa</i>, không nói về <i>bao nhiêu điểm thì giỏi</i>.
 */
public enum PhanHang {
    DONG("Đồng"),
    BAC("Bạc"),
    VANG("Vàng");

    /**
     * Dưới mức này thì không phân hạng ai cả.
     * <p>
     * Mười người là mức mà "top 10%" bắt đầu tương ứng với đúng một người thật, chứ không phải một phần
     * của người.
     */
    public static final int TOI_THIEU_NGUOI = 10;

    /** Top 10% đầu bảng. */
    private static final double NGUONG_VANG = 0.10;

    /** 25% tiếp theo — tức tới 35% đầu bảng. */
    private static final double NGUONG_BAC = 0.35;

    private final String nhan;

    PhanHang(String nhan) {
        this.nhan = nhan;
    }

    public String nhan() {
        return nhan;
    }

    /**
     * Hạng của một người theo vị trí trong mùa.
     *
     * @param thuHang       hạng bắt đầu từ 1
     * @param tongSoNguoi   tổng số người có điểm trong mùa
     * @return hạng, hoặc {@code null} khi chưa đủ người để việc phân hạng có nghĩa, hoặc khi người này
     *         chưa có điểm ({@code thuHang <= 0})
     */
    public static PhanHang cua(int thuHang, long tongSoNguoi) {
        if (thuHang <= 0 || tongSoNguoi < TOI_THIEU_NGUOI) {
            return null;
        }
        // Tỉ lệ vị trí: hạng 1 trong 100 người → 0,01. Chia cho tổng chứ không phải (tổng - 1), để người
        // đứng cuối luôn ra đúng 1.0 và không lọt vào nhóm trên vì một phép làm tròn.
        double viTri = (double) thuHang / tongSoNguoi;

        if (viTri <= NGUONG_VANG) {
            return VANG;
        }
        return viTri <= NGUONG_BAC ? BAC : DONG;
    }
}
