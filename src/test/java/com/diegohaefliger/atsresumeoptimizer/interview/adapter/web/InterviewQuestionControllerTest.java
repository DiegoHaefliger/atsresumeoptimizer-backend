package com.diegohaefliger.atsresumeoptimizer.interview.adapter.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.interview.application.InterviewQuestionService;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestion;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestionSet;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewRequiresJobResumeException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterviewQuestionController.class)
@Import(InterviewQuestionWebMapperImpl.class)
class InterviewQuestionControllerTest {

	private static final UUID JOB_ID = UUID.randomUUID();
	private static final UUID VERSION_ID = UUID.randomUUID();
	private static final String BASE = "/api/v1/jobs/" + JOB_ID + "/interview-questions";
	private static final InterviewQuestionSet SET = new InterviewQuestionSet(UUID.randomUUID(), JOB_ID, VERSION_ID,
			"gpt-4o-mini", Instant.parse("2026-10-08T12:00:00Z"),
			List.of(new InterviewQuestion("Como o Kafka garante ordem?", "Kafka", "A vaga pede Kafka", "Cite o projeto X")));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InterviewQuestionService service;

	@Test
	void listsTheQuestionHistoryOfTheJob() throws Exception {
		when(service.list(JOB_ID)).thenReturn(List.of(SET));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].questions[0].question").value("Como o Kafka garante ordem?"))
				.andExpect(jsonPath("$[0].questions[0].answerTip").value("Cite o projeto X"));
	}

	@Test
	void generatesTheQuestionsWithTheChosenResume() throws Exception {
		when(service.generate(JOB_ID, VERSION_ID)).thenReturn(SET);

		mockMvc.perform(post(BASE).contentType("application/json").content("{\"resumeVersionId\":\"" + VERSION_ID + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions[0].requirement").value("Kafka"));
	}

	@Test
	void generatesTheQuestionsWithoutAResume() throws Exception {
		when(service.generate(JOB_ID, null)).thenReturn(SET);

		mockMvc.perform(post(BASE).contentType("application/json").content("{}")).andExpect(status().isOk());
	}

	@Test
	void explainsWhyTheQuestionsCannotBeGenerated() throws Exception {
		when(service.generate(JOB_ID, VERSION_ID)).thenThrow(new InterviewRequiresJobResumeException(VERSION_ID));

		mockMvc.perform(post(BASE).contentType("application/json").content("{\"resumeVersionId\":\"" + VERSION_ID + "\"}"))
				.andExpect(status().isUnprocessableEntity());
	}
}
