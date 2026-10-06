package com.diegohaefliger.atsresumeoptimizer.ai.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.ai.application.PromptTemplateService;
import com.diegohaefliger.atsresumeoptimizer.ai.application.PromptTemplateSummary;
import com.diegohaefliger.atsresumeoptimizer.ai.application.PromptTemplateUpdate;
import com.diegohaefliger.atsresumeoptimizer.ai.application.PromptTemplateView;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PromptTemplateController.class)
@Import(PromptTemplateRequestMapperImpl.class)
class PromptTemplateControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PromptTemplateService promptTemplateService;

	@Test
	void listsTheLatestVersionOfEachInstruction() throws Exception {
		when(promptTemplateService.listLatest())
				.thenReturn(List.of(new PromptTemplateSummary("resume-structuring", 10, "gpt-4o", Instant.now())));

		mockMvc.perform(get("/api/v1/prompt-templates"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].key").value("resume-structuring"))
				.andExpect(jsonPath("$[0].version").value(10));
	}

	@Test
	void savingPublishesANewVersion() throws Exception {
		when(promptTemplateService.publish(eq("resume-structuring"), any(PromptTemplateUpdate.class)))
				.thenReturn(new PromptTemplateView("resume-structuring", 11, "gpt-4o", "Novo", Instant.now()));

		mockMvc.perform(put("/api/v1/prompt-templates/resume-structuring").contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"Novo\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value(11));
	}

	@Test
	void rejectsBlankContent() throws Exception {
		mockMvc.perform(put("/api/v1/prompt-templates/resume-structuring").contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"  \"}"))
				.andExpect(status().isBadRequest());
	}
}
