---
name: backend-testing
description: Dùng khi viết test cho một chức năng backend Spring Boot (sau khi code xong, trước khi sang chức năng kế). Test theo tầng — Service (Mockito), JPA (@DataJpaTest + Testcontainers), Controller (@WebMvcTest), Security (JWT/@PreAuthorize), WebSocket STOMP, SSE — đúng tháp kiểm thử.
---

# Viết test backend theo tầng

Áp dụng cho **mỗi chức năng** trong quy trình lát cắt dọc: code xong → viết test tầng phù hợp → chạy pass → cập nhật báo cáo. Mock LLM ở unit test, KHÔNG gọi API thật.

## Chọn loại test theo thứ mình vừa code

| Vừa code gì | Loại test | Annotation/công cụ chính |
|---|---|---|
| Logic Service | Unit | `@ExtendWith(MockitoExtension)`, `@Mock`, BDDMockito (`given/willReturn`), `ArgumentCaptor` |
| Repository/JPA (Postgres, pgvector) | Slice | `@DataJpaTest` + **Testcontainers** `@ServiceConnection`; test query, ràng buộc, lazy/N+1 |
| Controller REST | Slice | `@WebMvcTest` + `MockMvc` + `jsonPath`; test validation, `@RestControllerAdvice`, mã lỗi |
| Bảo mật (JWT/RBAC) | Slice | `spring-security-test`: `@WithMockUser`, `jwt()` post-processor, CSRF, `@PreAuthorize` |
| Phòng đấu (STOMP) | Integration | `WebSocketStompClient` + `BlockingQueue` nhận message; kiểm sự kiện `PLAYER_JOINED`… |
| Chatbot (SSE stream) | Integration | `WebTestClient` + `StepVerifier`, virtual time; kiểm token stream `text/event-stream` |
| Luồng end-to-end 1 feature | Integration | `@SpringBootTest` + Testcontainers (Postgres/Neo4j/Redis) |

## Quy trình
1. **Đọc** file feature liên quan để biết hành vi kỳ vọng + đường biên.
2. **Liệt kê ca test trước khi viết** (happy path + biên + lỗi + phân quyền).
3. Chọn tầng test theo bảng trên; đặt tên test mô tả kịch bản (`shouldReturn409WhenEmailExists`).
4. Với integration cần DB thật → **Testcontainers**, không mock DB. Sinh dữ liệu test bằng builder/Instancio, không hardcode lộn xộn.
5. **Mock `AiProvider`/`AiOrchestrator`** trong unit/slice — không gọi Gemini/Grok thật (việc gọi thật thuộc skill `eval-and-load-test`).
6. Chạy `mvn test` (hoặc `-Dtest=...`) → pass mới sang chức năng kế.
7. Cập nhật kết quả vào **mục 3.4** báo cáo (bảng kịch bản + tỉ lệ pass).

## Checklist
- [ ] Test hành vi & biên, không test getter/setter.
- [ ] Slice test đúng annotation (không bê nguyên `@SpringBootTest` cho mọi thứ).
- [ ] Integration dùng Testcontainers, không phụ thuộc DB local.
- [ ] LLM được mock ở unit/slice.
- [ ] Có test phân quyền cho endpoint có `@PreAuthorize`.
- [ ] Tên test mô tả rõ kịch bản; assert bằng AssertJ.

## Chống mẫu (tránh)
- `@SpringBootTest` cho mọi test (chậm, mất mục đích slice). Gọi API LLM thật trong test đơn vị. Mock DB thay vì Testcontainers ở integration. Test phụ thuộc thứ tự chạy / dữ liệu để sẵn trong DB.

> Pattern chi tiết tham khảo (Apache-2.0, copy-adapt): spring-ai-community/spring-testing-skills — spring-jpa-testing, spring-mvc-testing, spring-security-testing, spring-websocket-testing, spring-webflux-testing, spring-testing-fundamentals.
