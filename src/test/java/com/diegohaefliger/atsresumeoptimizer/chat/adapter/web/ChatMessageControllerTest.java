package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.chat.application.ChatReplyListener;
import com.diegohaefliger.atsresumeoptimizer.chat.application.ChatReplyService;
import com.diegohaefliger.atsresumeoptimizer.chat.application.PendingReply;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.UnknownConversationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ChatMessageController.class)
@Import({ChatWebMapperImpl.class, ChatMessageControllerTest.InlineExecutor.class})
class ChatMessageControllerTest {

	private static final UUID CONVERSATION = UUID.randomUUID();
	private static final String URL = "/api/v1/chat/conversations/" + CONVERSATION + "/messages";
	private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");

	@TestConfiguration
	static class InlineExecutor {

		@Bean
		TaskExecutor applicationTaskExecutor() {
			return Runnable::run;
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChatReplyService service;

	private static ChatMessage message(ChatRole role, String content) {
		return new ChatMessage(UUID.randomUUID(), role, content, null, 0, 0, BigDecimal.ZERO, false, NOW);
	}

	private void givenReply(Consumer<ChatReplyListener> script) {
		when(service.send(any(), any())).thenReturn(new PendingReply() {
			@Override
			public ChatMessage userMessage() {
				return message(ChatRole.USER, "oi");
			}

			@Override
			public void run(ChatReplyListener listener) {
				script.accept(listener);
			}
		});
	}

	private String streamBody(String json) throws Exception {
		MvcResult started = mockMvc.perform(post(URL).contentType("application/json").content(json))
				.andExpect(request().asyncStarted())
				.andReturn();
		return mockMvc.perform(asyncDispatch(started)).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
	}

	@Test
	void streamsTheQuestionTheTokensAndTheFinalMessage() throws Exception {
		givenReply(listener -> {
			listener.onToken("Olá");
			listener.onToken(" mundo");
			listener.onDone(message(ChatRole.ASSISTANT, "Olá mundo"));
		});

		String body = streamBody("{\"content\":\"oi\"}");

		assertThat(body).contains("event:user").contains("event:token").contains("\"text\":\"Olá\"")
				.contains("\"text\":\" mundo\"").contains("event:done").contains("\"content\":\"Olá mundo\"");
		assertThat(body.indexOf("event:user")).isLessThan(body.indexOf("event:token"));
		assertThat(body.indexOf("event:token")).isLessThan(body.indexOf("event:done"));
	}

	@Test
	void streamsAnErrorEvent() throws Exception {
		givenReply(listener -> listener.onError("Nenhum provedor de IA configurado."));

		assertThat(streamBody("{\"content\":\"oi\"}")).contains("event:error").contains("Nenhum provedor");
	}

	@Test
	void turnsAnUnexpectedFailureIntoAnErrorEvent() throws Exception {
		givenReply(listener -> {
			throw new IllegalStateException("banco caiu");
		});

		assertThat(streamBody("{\"content\":\"oi\"}")).contains("event:error").doesNotContain("banco caiu");
	}

	@Test
	void rejectsABlankQuestion() throws Exception {
		mockMvc.perform(post(URL).contentType("application/json").content("{\"content\":\"  \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsAQuestionOverTheLimit() throws Exception {
		String tooLong = "a".repeat(SendMessageRequest.MAX_CONTENT + 1);

		mockMvc.perform(post(URL).contentType("application/json").content("{\"content\":\"" + tooLong + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void answers404BeforeOpeningTheStreamForAnUnknownConversation() throws Exception {
		when(service.send(any(), any())).thenThrow(new UnknownConversationException(CONVERSATION));

		mockMvc.perform(post(URL).contentType("application/json").content("{\"content\":\"oi\"}"))
				.andExpect(status().isNotFound());
	}
}
