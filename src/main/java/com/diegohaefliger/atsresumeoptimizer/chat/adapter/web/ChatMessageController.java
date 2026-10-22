package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.chat.application.ChatReplyListener;
import com.diegohaefliger.atsresumeoptimizer.chat.application.ChatReplyService;
import com.diegohaefliger.atsresumeoptimizer.chat.application.PendingReply;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/chat/conversations/{conversationId}/messages")
class ChatMessageController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessageController.class);
	private static final long STREAM_TIMEOUT_MS = 10 * 60 * 1000L;
	private static final String UNEXPECTED_ERROR = "Não deu para concluir a resposta. Tente de novo.";

	private final ChatReplyService service;
	private final ChatWebMapper mapper;
	private final TaskExecutor executor;

	ChatMessageController(ChatReplyService service, ChatWebMapper mapper,
			@Qualifier("applicationTaskExecutor") TaskExecutor executor) {
		this.service = service;
		this.mapper = mapper;
		this.executor = executor;
	}

	@PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter send(@PathVariable UUID conversationId, @Valid @RequestBody SendMessageRequest request) {
		PendingReply pending = service.send(conversationId, request.content());
		SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
		executor.execute(() -> stream(pending, emitter));
		return emitter;
	}

	private void stream(PendingReply pending, SseEmitter emitter) {
		try {
			emit(emitter, "user", mapper.toResponse(pending.userMessage()));
			pending.run(new ChatReplyListener() {
				@Override
				public boolean onToken(String token) {
					return emit(emitter, "token", new TokenEvent(token));
				}

				@Override
				public void onDone(ChatMessage assistantMessage) {
					emit(emitter, "done", mapper.toResponse(assistantMessage));
				}

				@Override
				public void onError(String message) {
					emit(emitter, "error", new ErrorEvent(message));
				}
			});
		} catch (RuntimeException failure) {
			LOGGER.error("Erro inesperado no stream do chat", failure);
			emit(emitter, "error", new ErrorEvent(UNEXPECTED_ERROR));
		} finally {
			emitter.complete();
		}
	}

	private static boolean emit(SseEmitter emitter, String name, Object data) {
		try {
			emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
			return true;
		} catch (IOException | IllegalStateException clientGone) {
			return false;
		}
	}
}
