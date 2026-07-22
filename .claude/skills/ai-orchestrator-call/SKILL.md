---
name: ai-orchestrator-call
description: Dùng khi cần gọi LLM trong dự án (sinh đề, chấm, chatbot, tóm tắt). Bắt buộc đi qua AiOrchestrator với fallback Gemini→Grok, structured output, validate, cache, quota và logging.
---

# Gọi LLM qua AiOrchestrator (fallback Gemini → Grok)

Mọi lời gọi LLM phải qua lớp trừu tượng — KHÔNG gọi trực tiếp SDK provider trong service nghiệp vụ.

## Kiến trúc
```
Service nghiệp vụ → AiOrchestrator.generate(prompt, options)
   1) GeminiProvider (primary)
      thành công → trả AiResponse(source=gemini)
      lỗi 429/5xx/timeout ↓ (circuit breaker mở → bỏ qua)
   2) GrokProvider (fallback)
      thành công → AiResponse(source=grok)
      lỗi ↓
   3) ném AiUnavailableException (hoặc dùng cache/template dự phòng)
```

## Bắt buộc
1. **Interface chung** `AiProvider { AiResponse generate(...); Flux<String> stream(...); }`.
2. **Resilience4j:** circuit breaker + retry cho từng provider; timeout rõ ràng.
3. **Structured output:** khi cần JSON, đưa schema vào prompt; **validate** kết quả; **retry có giới hạn** nếu sai định dạng; loại phần tử không hợp lệ.
4. **Cache** theo `hash(prompt+options)` (Redis `ai:cache:{hash}`) cho tác vụ tất định (sinh đề).
5. **Quota** theo user (`quota:ai:{userId}`) → chặn khi vượt (HTTP 429).
6. **Logging** vào `ai_request_logs`: feature, provider, model, tokens_in/out, latency_ms, status.
7. **Bảo mật:** không gửi PII/mật khẩu; tách system/user prompt (chống injection).
8. **Streaming:** dùng `stream(...)` trả token qua SSE cho chatbot.

## Checklist trước khi merge
- [ ] Không có `GeminiClient`/`GrokClient` gọi trực tiếp ngoài package `ai.provider`.
- [ ] Có validate + retry cho JSON.
- [ ] Có ghi `ai_request_logs`.
- [ ] Có xử lý khi cả 2 provider lỗi.

## Tham chiếu
`docs/architecture.md` mục 5, `docs/features/05,06,08`, `docs/security.md` mục 3.
