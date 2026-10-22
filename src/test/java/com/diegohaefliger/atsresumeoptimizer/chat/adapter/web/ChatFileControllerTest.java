package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.chat.application.ChatFileService;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFileContent;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.UnknownChatFileException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatFileController.class)
class ChatFileControllerTest {

	private static final UUID ID = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChatFileService service;

	@Test
	void servesTheImageInlineWithItsOwnTypeAndNoSniffing() throws Exception {
		when(service.image(ID)).thenReturn(new ChatFileContent("image/png", "tela.png", new byte[] {1, 2, 3}));

		mockMvc.perform(get("/api/v1/chat/files/" + ID))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/png"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline")))
				.andExpect(content().bytes(new byte[] {1, 2, 3}));
	}

	@Test
	void answers404ForAnUnknownFile() throws Exception {
		when(service.image(ID)).thenThrow(new UnknownChatFileException(ID));

		mockMvc.perform(get("/api/v1/chat/files/" + ID)).andExpect(status().isNotFound());
	}
}
