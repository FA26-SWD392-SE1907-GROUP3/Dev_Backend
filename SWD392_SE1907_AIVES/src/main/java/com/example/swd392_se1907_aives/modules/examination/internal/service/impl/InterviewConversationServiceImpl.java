package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.State;
import com.example.swd392_se1907_aives.modules.examination.internal.service.*;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class InterviewConversationServiceImpl implements InterviewConversationService {
    private final InterviewStateStore store;
    private final ConversationAiClient ai;
    private final ExecutorService executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), r -> { var t = new Thread(r, "interview-conversation"); t.setDaemon(true); return t; });

    public InterviewConversationServiceImpl(InterviewStateStore store, ConversationAiClient ai) {
        this.store = store; this.ai = ai;
    }

    @Override public SseEmitter answer(UUID id, UUID promptId, String text) {
        var answer = store.reserveConversationAnswer(id, promptId, text);
        return resume(id, answer.turnId());
    }

    @Override public SseEmitter resume(UUID id, UUID answerId) {
        // Authorize and claim on the authenticated request thread, before any SSE bytes are sent.
        State before = store.state(id);
        if (before.turns().stream().noneMatch(t -> t.id().equals(answerId) && "STUDENT".equals(t.speaker())))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu trả lời.");
        if (before.busy()) throw new ResponseStatusException(HttpStatus.CONFLICT, "AI đang xử lý câu trả lời đã lưu.");
        var plan = before.needsContinuation() && answerId.equals(before.answerTurnId())
                ? store.claimDecision(id, answerId) : null;
        var emitter = new SseEmitter(75000L);
        var finished = new AtomicBoolean();
        var task = new FutureTask<Void>(new DelegatingSecurityContextRunnable(() -> {
            try {
                send(emitter, "saved", Map.of("answerTurnId", answerId));
                if (plan != null) {
                    var decision = ai.stream(plan.context(), delta -> send(emitter, "delta", Map.of("text", delta)));
                    store.decisionCompleted(id, plan, decision);
                }
                State state = store.state(id);
                if (state.prompt() != null && (plan == null || !state.prompt().kind().equals("FOLLOWUP_QUESTION")))
                    send(emitter, "delta", Map.of("text", state.prompt().text()));
                send(emitter, "complete", state);
            } catch (RuntimeException error) {
                String message = error instanceof ResponseStatusException status && status.getReason() != null
                        ? status.getReason() : "Hội thoại bị gián đoạn. Câu trả lời đã lưu, hãy thử tiếp tục.";
                if (plan != null) {
                    try { store.decisionFailed(id, plan.requestId(), message); } catch (RuntimeException ignored) {}
                }
                try { send(emitter, "error", Map.of("message", message)); } catch (RuntimeException ignored) {}
            } finally {
                finished.set(true);
                emitter.complete();
            }
        }), null);
        emitter.onCompletion(() -> { if (!finished.get()) task.cancel(true); });
        emitter.onTimeout(() -> task.cancel(true));
        emitter.onError(error -> task.cancel(true));
        try { executor.execute(task); }
        catch (RejectedExecutionException error) {
            if (plan != null) store.decisionFailed(id, plan.requestId(), "Máy chủ đang bận. Hãy thử tiếp tục.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Máy chủ đang bận. Câu trả lời đã lưu.");
        }
        return emitter;
    }

    @Override public State finish(UUID id) { return store.finishConversation(id); }

    private void send(SseEmitter emitter, String event, Object payload) {
        if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("Cancelled");
        try { emitter.send(SseEmitter.event().name(event).data(payload)); }
        catch (IOException error) { throw new UncheckedIOException(error); }
    }
    @PreDestroy public void close() { executor.shutdownNow(); }
}
