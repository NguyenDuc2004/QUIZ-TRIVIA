package com.datn.quizai.common.exception;

import com.datn.quizai.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Chuyển mọi exception thành response lỗi chuẩn (docs/api.md §10). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        String traceId = newTraceId();
        log.warn("[{}] {} {} → {}: {}", traceId, request.getMethod(), request.getRequestURI(),
                ex.getStatus().value(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(
                ex.getStatus().value(), ex.getStatus().getReasonPhrase(), ex.getMessage(),
                request.getRequestURI(), traceId));
    }

    /** Lỗi validate DTO request → 400 kèm chi tiết từng field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

        String traceId = newTraceId();
        log.warn("[{}] Dữ liệu không hợp lệ tại {}: {}", traceId, request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(ApiError.validation(
                "Dữ liệu gửi lên không hợp lệ", request.getRequestURI(), traceId, fieldErrors));
    }

    /** Body không đọc được (JSON sai cú pháp, sai encoding, thiếu body) → 400 chứ không phải 500. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                        HttpServletRequest request) {
        String traceId = newTraceId();
        log.warn("[{}] Không đọc được body tại {}: {}", traceId, request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(ApiError.of(
                400, "Bad Request", "Nội dung gửi lên không đọc được (JSON không hợp lệ hoặc sai bảng mã UTF-8)",
                request.getRequestURI(), traceId));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex,
                                                        HttpServletRequest request) {
        String traceId = newTraceId();
        log.warn("[{}] Đăng nhập thất bại tại {}", traceId, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(
                401, "Unauthorized", "Email hoặc mật khẩu không đúng",
                request.getRequestURI(), traceId));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                      HttpServletRequest request) {
        String traceId = newTraceId();
        log.warn("[{}] Truy cập bị từ chối tại {}", traceId, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(
                403, "Forbidden", "Bạn không có quyền thực hiện hành động này",
                request.getRequestURI(), traceId));
    }

    /**
     * Đường dẫn không tồn tại → <b>404</b>.
     *
     * <h3>Vì sao phải bắt CẢ HAI kiểu ngoại lệ</h3>
     * Bản đầu chỉ bắt {@code NoHandlerFoundException}, và đó là một <b>nhánh chết</b>: Spring Boot 3
     * chỉ ném nó khi bật {@code spring.mvc.throw-exception-if-no-handler-found}, mà dự án không bật.
     * Thực tế request không khớp controller nào sẽ rơi xuống bộ xử lý tài nguyên tĩnh, và chỗ đó ném
     * {@code NoResourceFoundException} — một kiểu khác hẳn, không có quan hệ kế thừa với kiểu trên.
     *
     * Hệ quả trước khi sửa: gõ sai một đường dẫn thì nhận <b>500 "Đã có lỗi xảy ra"</b> kèm một
     * stack trace đầy đủ ghi ở mức {@code ERROR}. Hai cái giá phải trả:
     * <ul>
     *   <li>Client không phân biệt được "gõ sai địa chỉ" với "server hỏng" — hai chuyện xử lý khác
     *       hẳn nhau.</li>
     *   <li>Log đầy stack trace của những đường dẫn gõ sai, làm loãng đúng thứ mức {@code ERROR}
     *       sinh ra để đánh dấu.</li>
     * </ul>
     *
     * Giữ lại {@code NoHandlerFoundException} thay vì xoá: nếu sau này ai bật cấu hình kia thì nó
     * thành nhánh sống, và lúc đó không phải nhớ ra chuyện này lần nữa.
     *
     * <b>Không ghi log</b>: đường dẫn không tồn tại là chuyện thường của Internet (bot quét, link cũ),
     * không phải sự kiện cần ai đọc.
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest request) {
        String traceId = newTraceId();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
                404, "Not Found", "Không tìm thấy tài nguyên", request.getRequestURI(), traceId));
    }

    /**
     * Gọi đúng đường dẫn nhưng sai phương thức HTTP (ví dụ {@code PUT} vào endpoint chỉ nhận
     * {@code POST}) → <b>405</b> kèm danh sách phương thức được phép, chứ không phải 500.
     * <p>
     * Không có handler này thì lỗi rơi xuống chốt cuối và client nhận "Đã có lỗi xảy ra" — người
     * gọi API không có cách nào biết mình chỉ dùng sai động từ.
     */
    /**
     * Tham số sai kiểu — ví dụ chỗ chờ UUID lại nhận chuỗi thường.
     *
     * <h3>404 hay 400 tuỳ tham số nằm ở ĐÂU</h3>
     * Không có handler này thì mọi trường hợp rơi xuống chốt cuối và trả <b>500 "Đã có lỗi xảy ra"</b>
     * — client không phân biệt được mình gõ sai với server hỏng, còn log thì đầy stack trace của
     * những lần gõ sai địa chỉ.
     *
     * <ul>
     *   <li><b>Biến đường dẫn → 404.</b> {@code GET /attempts/me} khớp route {@code /attempts/{id}}
     *       với {@code id="me"}, và "me" không phải UUID. Nhìn từ phía client thì <i>cả đường dẫn</i>
     *       không trỏ tới tài nguyên nào — đúng nghĩa 404, và cùng cách trả lời với một id đúng định
     *       dạng nhưng không tồn tại. Trả 400 ở đây sẽ tiết lộ rằng route có tồn tại và chỉ sai định
     *       dạng, tức nói nhiều hơn cần thiết.</li>
     *   <li><b>Tham số truy vấn hay biểu mẫu → 400.</b> Ở đó địa chỉ đúng, chỉ dữ liệu vào sai
     *       ({@code ?page=abc}), nên người gọi cần biết chính xác trường nào hỏng để sửa.</li>
     * </ul>
     *
     * Ghi log mức {@code debug}: gõ sai địa chỉ là chuyện thường của Internet, không phải sự kiện cần
     * ai đọc — đẩy lên {@code warn} thì mức đó loãng đi đúng bằng lượng bot quét đường dẫn.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        String traceId = newTraceId();
        boolean laBienDuongDan = ex.getParameter().hasParameterAnnotation(PathVariable.class);

        log.debug("[{}] {} {} → tham số '{}' sai kiểu", traceId, request.getMethod(),
                request.getRequestURI(), ex.getName());

        if (laBienDuongDan) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
                    404, "Not Found", "Không tìm thấy tài nguyên", request.getRequestURI(), traceId));
        }

        return ResponseEntity.badRequest().body(ApiError.of(
                400, "Bad Request",
                "Giá trị của tham số '" + ex.getName() + "' không hợp lệ",
                request.getRequestURI(), traceId));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest request) {
        String traceId = newTraceId();
        String allowed = ex.getSupportedHttpMethods() == null ? "" : ex.getSupportedHttpMethods().toString();

        log.warn("[{}] {} {} → 405, chỉ nhận {}", traceId, request.getMethod(),
                request.getRequestURI(), allowed);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiError.of(
                405, "Method Not Allowed",
                "Phương thức " + ex.getMethod() + " không dùng được ở đây"
                        + (allowed.isBlank() ? "" : ", chỉ nhận " + allowed),
                request.getRequestURI(), traceId));
    }

    /** Chốt cuối: không để lộ stack trace ra client, nhưng phải log đầy đủ kèm traceId. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        String traceId = newTraceId();
        log.error("[{}] Lỗi không lường trước tại {} {}", traceId, request.getMethod(),
                request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
                500, "Internal Server Error", "Đã có lỗi xảy ra, vui lòng thử lại sau",
                request.getRequestURI(), traceId));
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
