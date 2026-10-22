package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.chat.application.ChatConversationService;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatAttachment;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversation;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversationDetail;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.UnknownConversationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatConversationController.class)
@Import(ChatWebMapperImpl.class)
class ChatConversationControllerTest {

	private static final UUID ID = UUID.randomUUID();
	private static final UUID JOB_ID = UUID.randomUUID();
	private static final String URL = "/api/v1/chat/conversations";
	private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChatConversationService service;

	@Test
	void listsTheConversations() throws Exception {
		when(service.list()).thenReturn(List.of(new ChatConversation(ID, "Entrevista", NOW, NOW)));

		mockMvc.perform(get(URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Entrevista"));
	}

	@Test
	void createsAConversationWithoutATitle() throws Exception {
		when(service.create(null)).thenReturn(new ChatConversation(ID, "Nova conversa", NOW, NOW));

		mockMvc.perform(post(URL).contentType("application/json").content("{}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Nova conversa"));
	}

	@Test
	void rejectsATitleOverTheLimit() throws Exception {
		String tooLong = "a".repeat(ConversationTitle.MAX_LENGTH + 1);

		mockMvc.perform(post(URL).contentType("application/json").content("{\"title\":\"" + tooLong + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsTheConversationWithAttachmentsAndMessages() throws Exception {
		UUID versionId = UUID.randomUUID();
		when(service.get(ID)).thenReturn(new ChatConversationDetail(ID, "Entrevista", NOW, NOW,
				List.of(new ChatAttachment(JOB_ID, versionId)),
				List.of(new ChatMessage(UUID.randomUUID(), ChatRole.USER, "oi", null, 0, 0, BigDecimal.ZERO, false, NOW, List.of()))));

		mockMvc.perform(get(URL + "/" + ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.attachments[0].jobPostingId").value(JOB_ID.toString()))
				.andExpect(jsonPath("$.messages[0].role").value("USER"))
				.andExpect(jsonPath("$.messages[0].content").value("oi"));
	}

	@Test
	void answers404ForAnUnknownConversation() throws Exception {
		when(service.get(ID)).thenThrow(new UnknownConversationException(ID));

		mockMvc.perform(get(URL + "/" + ID)).andExpect(status().isNotFound());
	}

	@Test
	void renamesTheConversation() throws Exception {
		when(service.rename(ID, "Fit cultural")).thenReturn(new ChatConversation(ID, "Fit cultural", NOW, NOW));

		mockMvc.perform(patch(URL + "/" + ID).contentType("application/json").content("{\"title\":\"Fit cultural\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Fit cultural"));
	}

	@Test
	void rejectsABlankTitleOnRename() throws Exception {
		mockMvc.perform(patch(URL + "/" + ID).contentType("application/json").content("{\"title\":\" \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deletesTheConversation() throws Exception {
		mockMvc.perform(delete(URL + "/" + ID)).andExpect(status().isNoContent());

		verify(service).delete(ID);
	}

	@Test
	void attachesAJobWithoutABody() throws Exception {
		when(service.attach(ID, JOB_ID, null)).thenReturn(new ChatAttachment(JOB_ID, null));

		mockMvc.perform(put(URL + "/" + ID + "/jobs/" + JOB_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.jobPostingId").value(JOB_ID.toString()));
	}

	@Test
	void attachesAJobWithAResumeVersion() throws Exception {
		UUID versionId = UUID.randomUUID();
		when(service.attach(ID, JOB_ID, versionId)).thenReturn(new ChatAttachment(JOB_ID, versionId));

		mockMvc.perform(put(URL + "/" + ID + "/jobs/" + JOB_ID).contentType("application/json")
						.content("{\"resumeVersionId\":\"" + versionId + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resumeVersionId").value(versionId.toString()));
	}

	@Test
	void detachesAJob() throws Exception {
		mockMvc.perform(delete(URL + "/" + ID + "/jobs/" + JOB_ID)).andExpect(status().isNoContent());

		verify(service).detach(ID, JOB_ID);
	}
}
