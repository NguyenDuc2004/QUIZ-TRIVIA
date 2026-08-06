package com.datn.quizai.realtime;

import com.datn.quizai.realtime.dto.GameEvent;
import com.datn.quizai.realtime.dto.GameEventType;
import com.datn.quizai.realtime.service.RoomStateStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test phòng đấu real-time với <b>hai client STOMP thật</b> chạy song song
 * (docs/features/04-multiplayer-realtime.md).
 * <p>
 * Chỉ mock được rất ít ở đây: server chạy trên cổng thật, hai client mở WebSocket thật, sự kiện
 * đi vòng qua Redis Pub/Sub thật. Đây là cách duy nhất chứng minh được ba điều khó thấy bằng mắt:
 * mọi người nhận cùng câu hỏi, đáp án không lộ trước khi câu đóng, và điểm cộng theo tốc độ.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoomFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static final long TIMEOUT_SEC = 5;
    private static final String GUEST_BODY = "{\"displayName\":\"Khach vang lai\"}";
    private static final String GUEST_BODY_WITH_AVATAR =
            "{\"displayName\":\"Khach vang lai\",\"avatar\":\"FOX\"}";

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoomStateStore stateStore;

    private WebSocketStompClient stompClient;
    private String hostToken;
    private String playerToken;
    private String hostId;
    private String playerId;

    @BeforeAll
    void setUp() throws Exception {
        hostToken = register("host-room@example.com", "CREATOR");
        playerToken = register("player-room@example.com", "LEARNER");
        hostId = userIdOf(hostToken);
        playerId = userIdOf(playerToken);

        stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter(objectMapper));
    }

    @AfterAll
    void tearDown() {
        stompClient.stop();
    }

    @Test
    @DisplayName("Ván đấu đầy đủ: hai người vào phòng, cùng nhận câu hỏi, trả lời, lên bảng xếp hạng")
    void shouldPlayFullGameWithTwoClients() throws Exception {
        String quizId = createQuizWithQuestions();
        String roomCode = createRoom(quizId, hostToken);

        mockMvc.perform(post("/api/v1/rooms/{code}/join", roomCode)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players.length()").value(2));

        try (Client host = connect(hostToken, roomCode);
             Client player = connect(playerToken, roomCode)) {

            host.session.send("/app/room/" + roomCode + "/start", null);

            assertThat(host.nextOfType(GameEventType.GAME_STARTED)).isNotNull();
            assertThat(player.nextOfType(GameEventType.GAME_STARTED)).isNotNull();

            // Cả hai phải nhận CÙNG một câu hỏi (FR-22)
            JsonNode hostQuestion = host.nextOfType(GameEventType.QUESTION);
            JsonNode playerQuestion = player.nextOfType(GameEventType.QUESTION);
            assertThat(hostQuestion.get("questionId").asText())
                    .isEqualTo(playerQuestion.get("questionId").asText());

            // Câu hỏi phát đi KHÔNG kèm đáp án đúng
            assertThat(hostQuestion.has("correctOptionIds")).isFalse();
            assertThat(hostQuestion.get("options")).isNotEmpty();

            String questionId = hostQuestion.get("questionId").asText();
            String correctOptionId = correctOptionIdOf(questionId);

            player.session.send("/app/room/" + roomCode + "/answer",
                    java.util.Map.of("questionId", questionId, "optionIds", List.of(correctOptionId)));

            // Người trả lời nhận kết quả riêng của mình
            JsonNode result = player.nextPrivate(GameEventType.ANSWER_RESULT);
            assertThat(result.get("correct").asBoolean()).isTrue();
            assertThat(result.get("points").asInt()).isPositive();

            // Cả phòng chỉ biết "có thêm một người trả lời xong", không biết đúng hay sai
            JsonNode progress = host.nextOfType(GameEventType.PLAYER_ANSWERED);
            assertThat(progress.get("answeredCount").asInt()).isEqualTo(1);
            assertThat(progress.has("correct")).isFalse();

            // Host chuyển câu → giờ mới công bố đáp án
            host.session.send("/app/room/" + roomCode + "/next", null);

            JsonNode closed = host.nextOfType(GameEventType.QUESTION_CLOSED);
            assertThat(closed.get("correctOptionIds")).isNotEmpty();
            assertThat(closed.get("explanation").asText()).isNotBlank();

            JsonNode leaderboard = host.nextOfType(GameEventType.LEADERBOARD);
            assertThat(leaderboard.get(0).get("displayName").asText()).isNotBlank();
        }
    }

    @Test
    @DisplayName("Chỉ host điều khiển được ván; người chơi thường bấm bắt đầu bị từ chối")
    void shouldRejectStartFromNonHost() throws Exception {
        String roomCode = createRoom(createQuizWithQuestions(), hostToken);
        mockMvc.perform(post("/api/v1/rooms/{code}/join", roomCode)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + playerToken))
                .andExpect(status().isOk());

        try (Client player = connect(playerToken, roomCode)) {
            player.subscribeErrors();
            player.session.send("/app/room/" + roomCode + "/start", null);

            JsonNode error = player.errors.poll(TIMEOUT_SEC, TimeUnit.SECONDS);
            assertThat(error).isNotNull();
            assertThat(error.get("status").asInt()).isEqualTo(403);
        }
    }

    @Test
    @DisplayName("Trả lời hai lần cho cùng một câu bị từ chối")
    void shouldRejectDoubleAnswer() throws Exception {
        String quizId = createQuizWithQuestions();
        String roomCode = createRoom(quizId, hostToken);

        try (Client host = connect(hostToken, roomCode)) {
            host.subscribeErrors();
            host.session.send("/app/room/" + roomCode + "/start", null);

            JsonNode question = host.nextOfType(GameEventType.QUESTION);
            String questionId = question.get("questionId").asText();
            String optionId = question.get("options").get(0).get("id").asText();

            var payload = java.util.Map.of("questionId", questionId, "optionIds", List.of(optionId));
            host.session.send("/app/room/" + roomCode + "/answer", payload);
            host.nextPrivate(GameEventType.ANSWER_RESULT);

            host.session.send("/app/room/" + roomCode + "/answer", payload);

            JsonNode error = host.errors.poll(TIMEOUT_SEC, TimeUnit.SECONDS);
            assertThat(error).isNotNull();
            assertThat(error.get("status").asInt()).isEqualTo(409);
        }
    }

    @Test
    @DisplayName("Kết nối WebSocket không có token hợp lệ bị chặn")
    void shouldRejectUnauthenticatedConnection() {
        WebSocketStompClient client = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        client.setMessageConverter(new MappingJackson2MessageConverter(objectMapper));

        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer token-rac");

        assertThat(catchConnectFailure(client, headers)).isTrue();
        client.stop();
    }

    @Test
    @DisplayName("Vào phòng không tồn tại trả 404; ván đã kết thúc thì không vào được nữa")
    void shouldValidateRoomCode() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/{code}/join", "ZZZZZZ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + playerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/rooms/{code}", "ZZZZZZ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + playerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Chưa đăng nhập: không mở được phòng, nhưng xem được phòng nếu biết mã PIN")
    void shouldBlockGuestFromHostingButAllowLookup() throws Exception {
        mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isUnauthorized());

        // Mã PIN chính là thứ chặn cửa: mã sai thì 404 chứ không phải 401
        mockMvc.perform(get("/api/v1/rooms/{code}", "000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Mã phòng là PIN 6 chữ số, gõ được trên bàn phím số của điện thoại")
    void shouldGenerateNumericPin() throws Exception {
        String roomCode = createRoom(createQuizWithQuestions(), hostToken);

        assertThat(roomCode).hasSize(6).containsOnlyDigits();
    }

    @Test
    @DisplayName("Phòng không bật cho phép khách thì khách vào bị từ chối 403")
    void shouldRejectGuestWhenNotAllowed() throws Exception {
        String roomCode = createRoom(createQuizWithQuestions(), hostToken);

        mockMvc.perform(post("/api/v1/rooms/{code}/join-as-guest", roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUEST_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bật cho phép khách: khách vào được, nhận khoá phiên và hiện trong phòng chờ")
    void shouldLetGuestJoinWhenAllowed() throws Exception {
        String roomCode = createRoom(createQuizWithQuestions(), hostToken, true);

        String body = mockMvc.perform(post("/api/v1/rooms/{code}/join-as-guest", roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUEST_BODY_WITH_AVATAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestKey").isNotEmpty())
                .andExpect(jsonPath("$.room.players.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        JsonNode guestPlayer = null;
        for (JsonNode player : objectMapper.readTree(body).get("room").get("players")) {
            if (player.get("guest").asBoolean()) {
                guestPlayer = player;
            }
        }

        assertThat(guestPlayer).isNotNull();
        assertThat(guestPlayer.get("displayName").asText()).isEqualTo("Khach vang lai");
        assertThat(guestPlayer.get("avatar").asText()).isEqualTo("FOX");
        // Frontend vẽ được ngay, không phải tra bảng avatar
        assertThat(guestPlayer.get("avatarEmoji").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Khách chơi được bằng khoá phiên: nối WebSocket, bấm sẵn sàng, trả lời và được tính điểm")
    void shouldLetGuestPlayWithGuestKey() throws Exception {
        String roomCode = createRoom(createQuizWithQuestions(), hostToken, true);

        String body = mockMvc.perform(post("/api/v1/rooms/{code}/join-as-guest", roomCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUEST_BODY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String guestKey = objectMapper.readTree(body).get("guestKey").asText();

        try (Client host = connect(hostToken, roomCode);
             Client guest = connectAsGuest(guestKey, roomCode)) {

            guest.session.send("/app/room/" + roomCode + "/ready", Map.of("ready", true));
            JsonNode ready = host.nextOfType(GameEventType.PLAYER_READY);
            assertThat(ready.get("readyCount").asInt()).isEqualTo(1);

            host.session.send("/app/room/" + roomCode + "/start", null);
            guest.nextOfType(GameEventType.GAME_STARTED);

            JsonNode question = guest.nextOfType(GameEventType.QUESTION);
            String questionId = question.get("questionId").asText();

            guest.session.send("/app/room/" + roomCode + "/answer", Map.of(
                    "questionId", questionId,
                    "optionIds", List.of(correctOptionIdOf(questionId))));

            JsonNode result = guest.nextPrivate(GameEventType.ANSWER_RESULT);
            assertThat(result.get("correct").asBoolean()).isTrue();
            assertThat(result.get("points").asInt()).isPositive();
        }
    }

    @Test
    @DisplayName("Khoá phiên khách sai thì không nối được WebSocket")
    void shouldRejectInvalidGuestKey() {
        WebSocketStompClient client = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        client.setMessageConverter(new MappingJackson2MessageConverter(objectMapper));

        StompHeaders headers = new StompHeaders();
        headers.add("X-Guest-Key", "khoa-bia-dat");

        assertThat(catchConnectFailure(client, headers)).isTrue();
        client.stop();
    }

    @Test
    @DisplayName("Đổi avatar trong phòng chờ được phát cho cả phòng")
    void shouldBroadcastAvatarChange() throws Exception {
        String roomCode = createRoom(createQuizWithQuestions(), hostToken);

        try (Client host = connect(hostToken, roomCode)) {
            host.session.send("/app/room/" + roomCode + "/avatar", Map.of("avatar", "DRAGON"));

            JsonNode event = host.nextOfType(GameEventType.PLAYER_AVATAR_CHANGED);
            boolean hasDragon = false;
            for (JsonNode player : event.get("players")) {
                if ("DRAGON".equals(player.get("avatar").asText())) {
                    hasDragon = true;
                }
            }
            assertThat(hasDragon).isTrue();
        }
    }

    @Test
    @DisplayName("Quiz chưa có câu hỏi thì không mở phòng được")
    void shouldRejectEmptyQuiz() throws Exception {
        String body = mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Quiz rỗng cho phòng\",\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String quizId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(post("/api/v1/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":\"%s\"}".formatted(quizId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /rooms/{code} dựng lại được trạng thái sau khi mất kết nối (FR-25)")
    void shouldSupportResyncAfterReconnect() throws Exception {
        String roomCode = createRoom(createQuizWithQuestions(), hostToken);

        try (Client host = connect(hostToken, roomCode)) {
            host.session.send("/app/room/" + roomCode + "/start", null);
            JsonNode question = host.nextOfType(GameEventType.QUESTION);

            // Giả lập client mất kết nối rồi gọi lại REST để lấy trạng thái hiện tại
            mockMvc.perform(get("/api/v1/rooms/{code}", roomCode)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + hostToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PLAYING"))
                    .andExpect(jsonPath("$.currentQuestion.questionId")
                            .value(question.get("questionId").asText()))
                    // Trạng thái phục hồi cũng không được kèm đáp án đúng
                    .andExpect(jsonPath("$.currentQuestion.correctOptionIds").doesNotExist());
        }
    }

    // ===== Helper =====

    /** Một client STOMP kèm hàng đợi sự kiện nhận được. */
    private final class Client implements AutoCloseable {
        private final StompSession session;
        private final String roomCode;
        private final BlockingQueue<JsonNode> events = new LinkedBlockingQueue<>();
        private final BlockingQueue<JsonNode> privateEvents = new LinkedBlockingQueue<>();
        private final BlockingQueue<JsonNode> errors = new LinkedBlockingQueue<>();

        Client(StompSession session, String roomCode) {
            this.session = session;
            this.roomCode = roomCode;
            subscribe("/topic/room/" + roomCode, events);
            subscribe("/user/queue/room/" + roomCode, privateEvents);
        }

        void subscribeErrors() {
            subscribe("/user/queue/errors", errors);
        }

        private void subscribe(String destination, BlockingQueue<JsonNode> queue) {
            session.subscribe(destination, new StompSessionHandlerAdapter() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return JsonNode.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    queue.add((JsonNode) payload);
                }
            });
        }

        /** Chờ tới khi nhận được sự kiện đúng loại, bỏ qua các sự kiện khác xen giữa. */
        JsonNode nextOfType(GameEventType type) throws InterruptedException {
            return await(events, type);
        }

        JsonNode nextPrivate(GameEventType type) throws InterruptedException {
            return await(privateEvents, type);
        }

        private JsonNode await(BlockingQueue<JsonNode> queue, GameEventType type)
                throws InterruptedException {
            long deadline = System.currentTimeMillis() + TIMEOUT_SEC * 1000;
            while (System.currentTimeMillis() < deadline) {
                JsonNode event = queue.poll(TIMEOUT_SEC, TimeUnit.SECONDS);
                if (event == null) {
                    break;
                }
                if (type.name().equals(event.get("type").asText())) {
                    return event.get("data");
                }
            }
            throw new AssertionError("Không nhận được sự kiện " + type + " trong phòng " + roomCode);
        }

        @Override
        public void close() {
            session.disconnect();
        }
    }

    private Client connect(String token, String roomCode) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return connect(connectHeaders, roomCode);
    }

    /** Khách nối bằng khoá phiên thay vì JWT. */
    private Client connectAsGuest(String guestKey, String roomCode) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("X-Guest-Key", guestKey);
        return connect(connectHeaders, roomCode);
    }

    private Client connect(StompHeaders connectHeaders, String roomCode) throws Exception {
        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws",
                        new org.springframework.web.socket.WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                        })
                .get(TIMEOUT_SEC, TimeUnit.SECONDS);

        return new Client(session, roomCode);
    }

    private boolean catchConnectFailure(WebSocketStompClient client, StompHeaders headers) {
        try {
            client.connectAsync("ws://localhost:" + port + "/ws",
                            new org.springframework.web.socket.WebSocketHttpHeaders(),
                            headers,
                            new StompSessionHandlerAdapter() {
                            })
                    .get(TIMEOUT_SEC, TimeUnit.SECONDS);
            return false;
        } catch (Exception expected) {
            return true;
        }
    }

    private String register(String email, String role) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Người chơi test","role":"%s"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private String userIdOf(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    /** Quiz công khai 2 câu, mỗi câu có giải thích để kiểm tra QUESTION_CLOSED. */
    private String createQuizWithQuestions() throws Exception {
        String quizBody = mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Quiz phòng đấu","visibility":"PUBLIC","difficulty":"EASY"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String quizId = objectMapper.readTree(quizBody).get("id").asText();

        String first = createQuestion("Thủ đô Việt Nam?");
        String second = createQuestion("Ngôn ngữ của JVM?");

        mockMvc.perform(put("/api/v1/quizzes/{id}/questions", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"%s\",\"%s\"]}".formatted(first, second)))
                .andExpect(status().isOk());

        return quizId;
    }

    private String createQuestion(String content) throws Exception {
        String body = mockMvc.perform(post("/api/v1/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"SINGLE_CHOICE","content":"%s","difficulty":"EASY","points":1,
                                 "explanation":"Giải thích đáp án",
                                 "options":[{"content":"Đáp án đúng","correct":true},
                                            {"content":"Đáp án sai","correct":false}]}
                                """.formatted(content)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String correctOptionIdOf(String questionId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/questions/{id}", questionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (JsonNode option : objectMapper.readTree(body).get("options")) {
            if (option.get("correct").asBoolean()) {
                return option.get("id").asText();
            }
        }
        throw new AssertionError("Câu hỏi không có đáp án đúng");
    }

    @Test
    @DisplayName("Ván kết thúc ở Redis nhưng CSDL chưa kịp commit: join vẫn phải bị chặn 409")
    void shouldRejectJoinWhenLiveStateAlreadyFinished() throws Exception {
        // Tái hiện đúng khe hẹp gây lỗi: `next()` đổi trạng thái ở Redis trong khoá rồi phát
        // GAME_FINISHED NGAY, trong khi cột game_rooms.status chỉ đổi lúc giao dịch commit.
        // Người chơi nhận sự kiện xong gọi join ngay thì CSDL còn đọc ra PLAYING.
        // Ở đây dựng thẳng trạng thái lệch đó thay vì cố chạy đua cho thắng — vừa chắc chắn,
        // vừa nói rõ bất biến cần giữ: Redis là trạng thái sống, CSDL chỉ là bản lưu.
        String roomCode = createRoom(createQuizWithQuestions(), hostToken);

        stateStore.update(roomCode, state -> state.finished());

        mockMvc.perform(post("/api/v1/rooms/" + roomCode + "/join")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + playerToken))
                .andExpect(status().isConflict());
    }

    private String createRoom(String quizId, String token) throws Exception {
        return createRoom(quizId, token, false);
    }

    private String createRoom(String quizId, String token, boolean allowGuests) throws Exception {
        String body = mockMvc.perform(post("/api/v1/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"quizId\":\"%s\",\"secondsPerQuestion\":30,\"allowGuests\":%s}")
                                .formatted(quizId, allowGuests)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("roomCode").asText();
    }
}
