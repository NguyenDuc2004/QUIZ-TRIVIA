package com.datn.quizai.flashcard.controller;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.flashcard.domain.ReviewQuality;
import com.datn.quizai.flashcard.dto.DeckRequest;
import com.datn.quizai.flashcard.dto.DeckResponse;
import com.datn.quizai.flashcard.dto.FlashcardRequest;
import com.datn.quizai.flashcard.dto.FlashcardResponse;
import com.datn.quizai.flashcard.dto.ReviewResult;
import com.datn.quizai.flashcard.dto.ReviewStats;
import com.datn.quizai.flashcard.service.FlashcardService;
import com.datn.quizai.flashcard.service.ReviewService;
import com.datn.quizai.flashcard.service.WrongAnswerCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Flashcard và ôn tập ngắt quãng (features/11).
 * <p>
 * <b>Mọi endpoint đều làm việc trên dữ liệu của chính người gọi</b>, lấy từ token — không có tham số
 * {@code userId} nào trên đường dẫn. Nhận id người dùng từ client là mở đường đọc bộ thẻ của người khác chỉ
 * bằng cách đổi một tham số.
 * <p>
 * Không cần {@code @PreAuthorize} theo vai trò: đây là chức năng của <b>người học</b>, tức mọi tài khoản đã
 * đăng nhập. Quy tắc chung của dự án đã là `authenticated()` cho mọi thứ ngoài danh sách quiz công khai.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Flashcard", description = "Bộ thẻ ghi nhớ và ôn tập ngắt quãng (SM-2)")
@SecurityRequirement(name = "bearerAuth")
public class FlashcardController {

    private final FlashcardService flashcardService;
    private final ReviewService reviewService;
    private final WrongAnswerCardService wrongAnswerCardService;

    public FlashcardController(FlashcardService flashcardService,
                               ReviewService reviewService,
                               WrongAnswerCardService wrongAnswerCardService) {
        this.flashcardService = flashcardService;
        this.reviewService = reviewService;
        this.wrongAnswerCardService = wrongAnswerCardService;
    }

    // ----- Bộ thẻ (FR-37) -----

    @GetMapping("/decks")
    @Operation(summary = "Bộ thẻ của tôi, kèm số thẻ và số thẻ đến hạn ôn hôm nay")
    public PageResponse<DeckResponse> myDecks(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return flashcardService.listMyDecks(current.id(), keyword, pageable);
    }

    @PostMapping("/decks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo bộ thẻ mới")
    public DeckResponse createDeck(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                   @Valid @RequestBody DeckRequest request) {
        return flashcardService.createDeck(current.id(), request);
    }

    @PutMapping("/decks/{id}")
    @Operation(summary = "Sửa tên, mô tả hoặc chủ đề của bộ thẻ")
    public DeckResponse updateDeck(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody DeckRequest request) {
        return flashcardService.updateDeck(id, current.id(), request);
    }

    @DeleteMapping("/decks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xoá bộ thẻ cùng toàn bộ thẻ và tiến độ ôn của nó")
    public void deleteDeck(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                           @PathVariable UUID id) {
        flashcardService.deleteDeck(id, current.id());
    }

    // ----- Thẻ (FR-37) -----

    @GetMapping("/decks/{deckId}/cards")
    @Operation(summary = "Thẻ trong một bộ, kèm trạng thái ôn của tôi")
    public List<FlashcardResponse> cards(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                         @PathVariable UUID deckId) {
        return flashcardService.listCards(deckId, current.id());
    }

    @PostMapping("/decks/{deckId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm thẻ vào bộ. Thẻ mới đến hạn ôn ngay hôm nay.")
    public FlashcardResponse addCard(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                     @PathVariable UUID deckId,
                                     @Valid @RequestBody FlashcardRequest request) {
        return flashcardService.addCard(deckId, current.id(), request);
    }

    @PutMapping("/flashcards/{id}")
    @Operation(summary = "Sửa nội dung thẻ. KHÔNG đặt lại tiến độ ôn — sửa lỗi chính tả không đáng mất "
            + "tiến độ đã ôn.")
    public FlashcardResponse updateCard(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                        @PathVariable UUID id,
                                        @Valid @RequestBody FlashcardRequest request) {
        return flashcardService.updateCard(id, current.id(), request);
    }

    @DeleteMapping("/flashcards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xoá một thẻ")
    public void deleteCard(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                           @PathVariable UUID id) {
        flashcardService.deleteCard(id, current.id());
    }

    // ----- Sinh thẻ từ câu trả lời sai (FR-39) -----

    @PostMapping("/decks/{deckId}/cards/from-wrong-answers")
    @Operation(summary = "Sinh thẻ từ những câu tôi đã trả lời sai (tối đa 30 câu gần nhất). Bỏ qua câu "
            + "đã có thẻ trong bộ này. KHÔNG gọi mô hình AI — nội dung và đáp án đã có trong cơ sở dữ liệu.")
    public WrongAnswerCardService.KetQua fromWrongAnswers(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID deckId) {
        return wrongAnswerCardService.sinhVaoBo(deckId, current.id());
    }

    // ----- Phiên ôn tập (FR-40, FR-41, FR-42) -----

    @GetMapping("/flashcards/due")
    @Operation(summary = "Thẻ đến hạn ôn, quá hạn lâu nhất trước. Gồm cả thẻ quá hạn từ những ngày trước.")
    public List<FlashcardResponse> due(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                       @RequestParam(required = false) UUID deckId) {
        return reviewService.due(current.id(), deckId);
    }

    @PostMapping("/flashcards/{id}/review")
    @Operation(summary = "Gửi mức nhớ sau khi xem đáp án; trả về lịch ôn kế tiếp. AGAIN và HARD đưa thẻ "
            + "về ôn lại ngày mai, GOOD và EASY giãn lịch theo SM-2.")
    public ReviewResult review(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                               @PathVariable UUID id,
                               @RequestParam ReviewQuality quality) {
        return reviewService.review(id, current.id(), quality);
    }

    @GetMapping("/flashcards/stats")
    @Operation(summary = "Thống kê ôn tập: tổng số thẻ, số đã thuộc (khoảng ôn ≥ 21 ngày), số đến hạn "
            + "hôm nay và dự báo khối lượng 7 ngày tới.")
    public ReviewStats stats(@AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return reviewService.stats(current.id());
    }
}
