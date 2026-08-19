package com.datn.quizai.classroom.dto;

/**
 * Trạng thái một bài tập <b>đối với một học sinh cụ thể</b> (features/14, FR-56).
 * <p>
 * Không lưu trong cơ sở dữ liệu — tính từ ba thứ đã có: hạn nộp, lượt làm bài của người đó, và thời điểm
 * hiện tại. Lưu thành cột thì phải có ai đó cập nhật nó lúc quá hạn, tức một job nữa có thể chết, để giữ
 * một giá trị suy ra được trong một dòng.
 */
public enum TrangThaiBaiTap {

    CHUA_LAM("Chưa làm"),
    DANG_LAM("Đang làm dở"),
    DA_NOP("Đã nộp"),

    /**
     * Nộp sau hạn.
     * <p>
     * Hệ thống <b>vẫn cho nộp muộn</b> thay vì khoá cứng lúc hết hạn: khoá cứng thì một học sinh mất mạng
     * mười phút là mất trắng bài, và giáo viên không còn cách nào biết em ấy có làm hay không. Nộp muộn được
     * đánh dấu rõ, và quyết định trừ điểm hay không là của giáo viên — cùng nguyên tắc với chống gian lận:
     * hệ thống đưa dữ kiện, người thật quyết định.
     */
    NOP_TRE("Nộp muộn"),

    /** Quá hạn mà chưa nộp. Vẫn nộp được, và khi đó chuyển thành {@link #NOP_TRE}. */
    QUA_HAN("Quá hạn, chưa nộp");

    private final String nhan;

    TrangThaiBaiTap(String nhan) {
        this.nhan = nhan;
    }

    public String nhan() {
        return nhan;
    }
}
