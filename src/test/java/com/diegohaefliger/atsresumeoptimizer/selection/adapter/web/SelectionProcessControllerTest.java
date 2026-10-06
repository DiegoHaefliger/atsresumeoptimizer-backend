package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.selection.application.SelectionProcessService;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.InvalidStageMovementException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcessNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.StageMovement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SelectionProcessController.class)
@Import(SelectionProcessWebMapperImpl.class)
class SelectionProcessControllerTest {

	private static final UUID ID = UUID.randomUUID();
	private static final UUID JOB_ID = UUID.randomUUID();
	private static final String BASE = "/api/v1/selection-processes";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SelectionProcessService service;

	@Test
	void listsProcessesWithTheirLinks() throws Exception {
		when(service.list(Optional.of(SelectionStage.OFFER))).thenReturn(List.of(process(SelectionStage.OFFER)));

		mockMvc.perform(get(BASE).param("stage", "OFFER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].jobCode").value(12))
				.andExpect(jsonPath("$[0].company").value("Acme"))
				.andExpect(jsonPath("$[0].jobUrl").value("https://acme.com/vaga"))
				.andExpect(jsonPath("$[0].processUrl").value("https://acme.gupy.io/p/1"))
				.andExpect(jsonPath("$[0].stage").value("OFFER"));
	}

	@Test
	void createsStartingAtInterestedWhenNoStageIsGiven() throws Exception {
		when(service.create(any(), eq(SelectionStage.INTERESTED))).thenReturn(process(SelectionStage.INTERESTED));

		mockMvc.perform(post(BASE)
						.contentType("application/json")
						.content("{\"jobPostingId\":\"" + JOB_ID + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.stage").value("INTERESTED"));
	}

	@Test
	void rejectsCreationWithoutAJobOrWithInvalidLink() throws Exception {
		mockMvc.perform(post(BASE).contentType("application/json").content("{\"processUrl\":\"https://x.com\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post(BASE)
						.contentType("application/json")
						.content("{\"jobPostingId\":\"" + JOB_ID + "\",\"processUrl\":\"not-a-link\"}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void movesTheStageManually() throws Exception {
		when(service.moveTo(ID, SelectionStage.SCREENING, "ligaram")).thenReturn(process(SelectionStage.SCREENING));

		mockMvc.perform(post(BASE + "/" + ID + "/stage")
						.contentType("application/json")
						.content("{\"stage\":\"SCREENING\",\"note\":\"ligaram\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stage").value("SCREENING"))
				.andExpect(jsonPath("$.history[0].stage").value("APPLIED"));
	}

	@Test
	void rejectsMovingToTheSameStage() throws Exception {
		when(service.moveTo(ID, SelectionStage.OFFER, null))
				.thenThrow(new InvalidStageMovementException("O processo já está na etapa Proposta."));

		mockMvc.perform(post(BASE + "/" + ID + "/stage").contentType("application/json").content("{\"stage\":\"OFFER\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O processo já está na etapa Proposta."));
	}

	@Test
	void returnsNotFoundForAnUnknownProcess() throws Exception {
		when(service.get(ID)).thenThrow(new SelectionProcessNotFoundException(ID));

		mockMvc.perform(get(BASE + "/" + ID)).andExpect(status().isNotFound());
	}

	@Test
	void deletesAProcess() throws Exception {
		mockMvc.perform(delete(BASE + "/" + ID)).andExpect(status().isNoContent());

		verify(service).delete(ID);
	}

	@Test
	void removesAPreviousMovement() throws Exception {
		UUID movementId = UUID.randomUUID();

		mockMvc.perform(delete(BASE + "/" + ID + "/history/" + movementId)).andExpect(status().isNoContent());

		verify(service).removeMovement(ID, movementId);
	}

	@Test
	void refusesToRemoveTheCurrentMovement() throws Exception {
		UUID movementId = UUID.randomUUID();
		doThrow(new InvalidStageMovementException("A etapa atual não pode ser excluída. Mova o processo antes."))
				.when(service).removeMovement(ID, movementId);

		mockMvc.perform(delete(BASE + "/" + ID + "/history/" + movementId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("A etapa atual não pode ser excluída. Mova o processo antes."));
	}

	private SelectionProcess process(SelectionStage stage) {
		Instant now = Instant.now();
		return new SelectionProcess(ID, JOB_ID, 12L, "Acme", "Dev Java", "https://acme.com/vaga", "https://acme.gupy.io/p/1", stage,
				null, null, null, null, null, null, now, now, List.of(new StageMovement(UUID.randomUUID(), SelectionStage.APPLIED, null, now)));
	}
}
