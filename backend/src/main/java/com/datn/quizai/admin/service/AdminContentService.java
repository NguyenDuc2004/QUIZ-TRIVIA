package com.datn.quizai.admin.service;

import com.datn.quizai.admin.dto.AdminCategoryResponse;
import com.datn.quizai.admin.dto.CategoryRequest;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.quiz.domain.Category;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.quiz.domain.Visibility;
import com.datn.quizai.quiz.dto.QuizSummaryResponse;
import com.datn.quizai.quiz.repository.CategoryRepository;
import com.datn.quizai.quiz.repository.QuizRepository;
import com.datn.quizai.quiz.service.QuizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Quản lý nội dung công khai cho quản trị viên (features/10, FR-79 và FR-80).
 * <p>
 * <b>Ẩn quiz, không xoá quiz.</b> Cùng lý do như khoá tài khoản thay vì xoá người dùng: một quiz công
 * khai có thể đang có người làm, và lượt làm bài của họ nằm trong lịch sử cùng bảng xếp hạng. Đưa quiz
 * về riêng tư thì nó biến khỏi trang khám phá, chủ của nó sửa lại rồi công khai lại được, và không ai
 * mất dữ liệu.
 */
@Service
public class AdminContentService {

    private static final Logger log = LoggerFactory.getLogger(AdminContentService.class);

    private final CategoryRepository categoryRepository;
    private final QuizRepository quizRepository;
    private final QuizService quizService;
    private final JdbcTemplate jdbc;

    public AdminContentService(CategoryRepository categoryRepository,
                              QuizRepository quizRepository,
                              QuizService quizService,
                              JdbcTemplate jdbc) {
        this.categoryRepository = categoryRepository;
        this.quizRepository = quizRepository;
        this.quizService = quizService;
        this.jdbc = jdbc;
    }

    /**
     * Quiz công khai để kiểm duyệt (FR-79).
     * <p>
     * Gọi lại đúng {@code QuizService.listPublic} mà trang khám phá của người học dùng, và đó là chủ ý:
     * thứ cần kiểm duyệt chính là thứ người học nhìn thấy, nên hai danh sách phải không bao giờ lệch nhau.
     * Quiz {@code PRIVATE} không nằm trong đây vì không ai ngoài chủ của nó xem được — không có gì để
     * kiểm duyệt, và đọc nội dung riêng tư của người khác không phải quyền mà vai trò quản trị cần.
     * <p>
     * Cũng vì gọi lại mà chỗ này không tự ghép mẫu {@code like}: {@code QuizService} đã xử lý cái bẫy
     * {@code lower(bytea)} khi từ khoá null, chép lại logic đó ở đây là chép lại cả rủi ro.
     */
    @Transactional(readOnly = true)
    public PageResponse<QuizSummaryResponse> publicQuizzes(String keyword, UUID categoryId,
                                                          Pageable pageable) {
        return quizService.listPublic(categoryId, null, keyword, pageable);
    }

    /**
     * Danh mục kèm số quiz đang dùng.
     * <p>
     * Đếm bằng một câu SQL gộp thay vì lặp từng danh mục rồi gọi {@code countByCategory}: số danh mục
     * nhỏ nhưng đó vẫn là N+1 lượt đi vòng tới cơ sở dữ liệu cho một trang chỉ hiện một bảng.
     */
    @Transactional(readOnly = true)
    public List<AdminCategoryResponse> categories() {
        return jdbc.query("""
                select c.id, c.name, c.slug, c.description, count(q.id) as so_quiz
                from categories c
                         left join quizzes q on q.category_id = c.id
                group by c.id, c.name, c.slug, c.description
                order by c.name
                """,
                (rs, i) -> new AdminCategoryResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getString("description"),
                        rs.getLong("so_quiz")));
    }

    @Transactional
    public AdminCategoryResponse createCategory(CategoryRequest request) {
        String slug = resolveSlug(request);
        categoryRepository.findBySlug(slug).ifPresent(existing -> {
            throw BusinessException.conflict("Đã có danh mục dùng đường dẫn \"" + slug + "\"");
        });

        Category category = new Category();
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(blankToNull(request.description()));
        categoryRepository.save(category);

        log.info("Thêm danh mục {} ({})", category.getName(), slug);
        return new AdminCategoryResponse(category.getId(), category.getName(), category.getSlug(),
                category.getDescription(), 0);
    }

    @Transactional
    public AdminCategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy danh mục"));

        String slug = resolveSlug(request);
        categoryRepository.findBySlug(slug)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw BusinessException.conflict("Đã có danh mục khác dùng đường dẫn \"" + slug + "\"");
                });

        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(blankToNull(request.description()));

        long soQuiz = countQuizzes(id);
        return new AdminCategoryResponse(category.getId(), category.getName(), category.getSlug(),
                category.getDescription(), soQuiz);
    }

    /**
     * Xoá danh mục, và <b>chặn nếu còn quiz đang dùng</b>.
     * <p>
     * Hai cách xử lý khác đều tệ hơn: xoá kèm cả quiz là phá nội dung người khác vì một thao tác dọn
     * dẹp; để quiz mồ côi (category_id = null) thì chúng lặng lẽ rơi vào nhóm "Chưa phân loại" mà chủ
     * của chúng không biết. Trả 409 kèm số lượng để quản trị viên tự quyết định chuyển chúng đi đâu.
     */
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy danh mục"));

        long soQuiz = countQuizzes(id);
        if (soQuiz > 0) {
            throw BusinessException.conflict("Còn " + soQuiz + " quiz đang dùng danh mục này. "
                    + "Hãy chuyển chúng sang danh mục khác trước khi xoá.");
        }

        categoryRepository.delete(category);
        log.info("Xoá danh mục {} ({})", category.getName(), category.getSlug());
    }

    /**
     * Ẩn một quiz công khai: đưa về {@code PRIVATE}.
     * <p>
     * Quiz vẫn thuộc chủ của nó và không mất dữ liệu nào. Trả về quiz đã cập nhật để giao diện biết
     * thao tác đã có hiệu lực.
     */
    @Transactional
    public void hideQuiz(UUID quizId, UUID adminId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz"));

        if (quiz.getVisibility() == Visibility.PRIVATE) {
            return;
        }
        quiz.setVisibility(Visibility.PRIVATE);
        log.info("Quản trị {} ẩn quiz {} của chủ {}", adminId, quizId,
                quiz.getOwner() == null ? "?" : quiz.getOwner().getId());
    }

    private long countQuizzes(UUID categoryId) {
        Long count = jdbc.queryForObject(
                "select count(*) from quizzes where category_id = ?", Long.class, categoryId);
        return count == null ? 0 : count;
    }

    private String resolveSlug(CategoryRequest request) {
        if (request.slug() != null && !request.slug().isBlank()) {
            return request.slug().trim();
        }
        return toSlug(request.name());
    }

    /**
     * Sinh slug từ tên tiếng Việt: bỏ dấu, hạ chữ thường, thay khoảng trắng bằng gạch ngang.
     * <p>
     * {@code Normalizer.Form.NFD} tách chữ và dấu thành hai ký tự riêng để xoá được phần dấu; riêng "đ"
     * không phải chữ có dấu tách rời nên phải thay tay, nếu không nó bị xoá mất và "đại số" thành "ai-so".
     */
    static String toSlug(String name) {
        String noMark = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("đ", "d").replace("Đ", "D");
        return noMark.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
