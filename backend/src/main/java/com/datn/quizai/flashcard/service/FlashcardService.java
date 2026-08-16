package com.datn.quizai.flashcard.service;

import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.flashcard.domain.Flashcard;
import com.datn.quizai.flashcard.domain.FlashcardDeck;
import com.datn.quizai.flashcard.domain.FlashcardReview;
import com.datn.quizai.flashcard.domain.FlashcardSource;
import com.datn.quizai.flashcard.dto.DeckRequest;
import com.datn.quizai.flashcard.dto.DeckResponse;
import com.datn.quizai.flashcard.dto.FlashcardRequest;
import com.datn.quizai.flashcard.dto.FlashcardResponse;
import com.datn.quizai.flashcard.repository.FlashcardDeckRepository;
import com.datn.quizai.flashcard.repository.FlashcardRepository;
import com.datn.quizai.flashcard.repository.FlashcardReviewRepository;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Quản lý bộ thẻ và thẻ ghi nhớ (features/11, FR-37).
 * <p>
 * <b>Bộ thẻ là riêng tư.</b> Bản này không có chia sẻ bộ thẻ: chia sẻ kéo theo cả một tầng quyền mới
 * (ai xem được, ai sửa được, sao chép hay dùng chung), và trạng thái ôn tập vốn đã là theo từng người nên
 * hai người dùng chung một bộ vẫn có lịch riêng — tức phần khó nhất của việc dùng chung đã có sẵn, chỉ
 * còn phần quyền là việc chưa làm.
 */
@Service
public class FlashcardService {

    private static final Logger log = LoggerFactory.getLogger(FlashcardService.class);

    private final FlashcardDeckRepository deckRepository;
    private final FlashcardRepository cardRepository;
    private final FlashcardReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public FlashcardService(FlashcardDeckRepository deckRepository,
                            FlashcardRepository cardRepository,
                            FlashcardReviewRepository reviewRepository,
                            UserRepository userRepository) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<DeckResponse> listMyDecks(UUID ownerId, String keyword, Pageable pageable) {
        var trang = deckRepository.findOwnedDecks(ownerId, likePattern(keyword), pageable);

        // Hai truy vấn gộp cho cả trang, không phải hai truy vấn cho MỖI bộ thẻ
        Map<UUID, Long> denHan = reviewRepository.demDenHanTheoBo(ownerId, LocalDate.now()).stream()
                .collect(java.util.stream.Collectors.toMap(
                        FlashcardReviewRepository.DenHanTheoBoRow::getDeckId,
                        FlashcardReviewRepository.DenHanTheoBoRow::getSoThe));
        Map<UUID, Long> soThe = cardRepository.demTheoBo(
                        trang.getContent().stream().map(FlashcardDeck::getId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(
                        FlashcardRepository.SoTheRow::getDeckId, FlashcardRepository.SoTheRow::getSoThe));

        return PageResponse.of(trang, deck -> DeckResponse.from(
                deck,
                soThe.getOrDefault(deck.getId(), 0L),
                denHan.getOrDefault(deck.getId(), 0L)));
    }

    @Transactional
    public DeckResponse createDeck(UUID ownerId, DeckRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));

        FlashcardDeck deck = new FlashcardDeck();
        deck.setOwner(owner);
        apply(deck, request);
        deckRepository.save(deck);

        log.info("Người dùng {} tạo bộ thẻ {} ({})", ownerId, deck.getId(), deck.getTitle());
        return DeckResponse.from(deck, 0, 0);
    }

    @Transactional
    public DeckResponse updateDeck(UUID deckId, UUID ownerId, DeckRequest request) {
        FlashcardDeck deck = requireOwnedDeck(deckId, ownerId);
        apply(deck, request);
        return DeckResponse.from(deck, cardRepository.countByDeckId(deckId),
                reviewRepository.demDenHanTrongBo(ownerId, deckId, LocalDate.now()));
    }

    /**
     * Xoá bộ thẻ, kéo theo thẻ và trạng thái ôn của nó (khoá ngoại {@code ON DELETE CASCADE}).
     * <p>
     * Ở đây <b>xoá thật</b>, khác với quiz — và khác biệt đó là có lý do: bộ thẻ chỉ thuộc một người và
     * không ai khác đang dùng nó, không có lượt làm bài hay bảng xếp hạng nào tham chiếu tới. Giữ lại một
     * bộ thẻ người ta đã bỏ chỉ làm bẩn danh sách của chính họ.
     */
    @Transactional
    public void deleteDeck(UUID deckId, UUID ownerId) {
        FlashcardDeck deck = requireOwnedDeck(deckId, ownerId);
        deckRepository.delete(deck);
        log.info("Người dùng {} xoá bộ thẻ {}", ownerId, deckId);
    }

    /**
     * Thẻ trong một bộ, kèm trạng thái ôn của người gọi.
     * <p>
     * Nạp toàn bộ trạng thái ôn của bộ này bằng <b>một</b> truy vấn rồi ghép trong bộ nhớ, thay vì gọi
     * {@code findByFlashcardIdAndUserId} cho từng thẻ — cách sau là N+1 lượt đi vòng tới cơ sở dữ liệu
     * cho một trang chỉ hiện một danh sách.
     */
    @Transactional(readOnly = true)
    public List<FlashcardResponse> listCards(UUID deckId, UUID ownerId) {
        requireOwnedDeck(deckId, ownerId);

        Map<UUID, FlashcardReview> theoThe = reviewRepository
                .findByUserIdAndFlashcardDeckId(ownerId, deckId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> r.getFlashcard().getId(), Function.identity()));

        return cardRepository.findByDeckIdOrderByCreatedAt(deckId).stream()
                .map(card -> FlashcardResponse.from(card, theoThe.get(card.getId())))
                .toList();
    }

    @Transactional
    public FlashcardResponse addCard(UUID deckId, UUID ownerId, FlashcardRequest request) {
        FlashcardDeck deck = requireOwnedDeck(deckId, ownerId);

        Flashcard card = new Flashcard();
        card.setDeck(deck);
        card.setFront(request.front().trim());
        card.setBack(request.back().trim());
        card.setHint(blankToNull(request.hint()));
        card.setSource(FlashcardSource.MANUAL);
        cardRepository.save(card);

        // Tạo luôn trạng thái ôn để thẻ mới đến hạn ngay hôm nay. Không tạo ở đây thì thẻ chỉ xuất hiện
        // trong danh sách mà không bao giờ vào phiên ôn, vì phiên ôn đọc từ bảng trạng thái.
        taoTrangThaiOn(card, ownerId);

        return FlashcardResponse.from(card, null);
    }

    @Transactional
    public FlashcardResponse updateCard(UUID cardId, UUID ownerId, FlashcardRequest request) {
        Flashcard card = requireOwnedCard(cardId, ownerId);
        card.setFront(request.front().trim());
        card.setBack(request.back().trim());
        card.setHint(blankToNull(request.hint()));

        // KHÔNG reset lịch ôn khi sửa nội dung: người dùng thường chỉ sửa lỗi chính tả hoặc diễn đạt lại,
        // và mất tiến độ ôn vì một lần sửa chữ là hình phạt không ai muốn. Muốn học lại từ đầu thì xoá
        // thẻ rồi thêm mới — đó là hành động rõ ràng hơn.
        return FlashcardResponse.from(card,
                reviewRepository.findByFlashcardIdAndUserId(cardId, ownerId).orElse(null));
    }

    @Transactional
    public void deleteCard(UUID cardId, UUID ownerId) {
        Flashcard card = requireOwnedCard(cardId, ownerId);
        cardRepository.delete(card);
    }

    /** Tạo trạng thái ôn mặc định cho một thẻ nếu người này chưa có. Trả về true nếu vừa tạo mới. */
    boolean taoTrangThaiOn(Flashcard card, UUID userId) {
        if (reviewRepository.findByFlashcardIdAndUserId(card.getId(), userId).isPresent()) {
            return false;
        }
        User user = userRepository.getReferenceById(userId);
        FlashcardReview review = new FlashcardReview();
        review.setFlashcard(card);
        review.setUser(user);
        review.setDueDate(LocalDate.now());
        reviewRepository.save(review);
        return true;
    }

    FlashcardDeck requireOwnedDeck(UUID deckId, UUID ownerId) {
        FlashcardDeck deck = deckRepository.findByIdWithOwner(deckId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy bộ thẻ"));
        // 404 chứ không 403 với bộ thẻ của người khác: trả 403 là xác nhận bộ thẻ đó tồn tại, tức tiết lộ
        // thông tin cho người không có quyền biết. Cùng cách `QuizService` đang làm.
        if (!deck.getOwner().getId().equals(ownerId)) {
            throw BusinessException.notFound("Không tìm thấy bộ thẻ");
        }
        return deck;
    }

    private Flashcard requireOwnedCard(UUID cardId, UUID ownerId) {
        Flashcard card = cardRepository.findByIdWithDeckOwner(cardId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy thẻ"));
        if (!card.getDeck().getOwner().getId().equals(ownerId)) {
            throw BusinessException.notFound("Không tìm thấy thẻ");
        }
        return card;
    }

    private static void apply(FlashcardDeck deck, DeckRequest request) {
        deck.setTitle(request.title().trim());
        deck.setDescription(blankToNull(request.description()));
        deck.setTopic(blankToNull(request.topic()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Ghép mẫu {@code like} ở đây thay vì trong JPQL.
     * <p>
     * Gọi {@code lower(concat(...))} trong JPQL với tham số null làm PostgreSQL không suy được kiểu và
     * báo {@code function lower(bytea) does not exist} — cái bẫy đã gặp hai lần trong dự án này.
     */
    private static String likePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }
}
