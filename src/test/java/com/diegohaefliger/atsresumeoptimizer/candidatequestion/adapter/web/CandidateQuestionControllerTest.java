package com.diegohaefliger.atsresumeoptimizer.candidatequestion.adapter.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.candidatequestion.application.CandidateQuestionService;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestion;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionSet;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionsUnavailableException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CandidateQuestionController.class)
@Import(CandidateQuestionWebMapperImpl.class)
class CandidateQuestionControllerTest {

	private static final UUID JOB_ID = UUID.randomUUID();
	private static final String BASE = "/api/v1/jobs/" + JOB_ID + "/candidate-questions";
	private static final CandidateQuestionSet SET = new CandidateQuestionSet(UUID.randomUUID(), JOB_ID, "gpt-4o-mini",
			Instant.parse("2026-10-12T12:00:00Z"),
			List.of(new CandidateQuestion("Como funciona o home office?", "Remoto", "A vaga é híbrida")));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CandidateQuestionService service;

	@Test
	void listsTheQuestionHistoryOfTheJob() throws Exception {
		when(service.list(JOB_ID)).thenReturn(List.of(SET));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].questions[0].question").value("Como funciona o home office?"))
				.andExpect(jsonPath("$[0].questions[0].topic").value("Remoto"));
	}

	@Test
	void generatesTheQuestions() throws Exception {
		when(service.generate(JOB_ID)).thenReturn(SET);

		mockMvc.perform(post(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions[0].reason").value("A vaga é híbrida"));
	}

	@Test
	void explainsWhyTheQuestionsCannotBeGenerated() throws Exception {
		when(service.generate(JOB_ID)).thenThrow(new CandidateQuestionsUnavailableException());

		mockMvc.perform(post(BASE)).andExpect(status().isBadGateway());
	}
}
