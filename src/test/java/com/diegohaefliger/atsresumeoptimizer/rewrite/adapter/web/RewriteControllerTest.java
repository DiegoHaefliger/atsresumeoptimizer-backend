package com.diegohaefliger.atsresumeoptimizer.rewrite.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.rewrite.BulletRewriteView;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ComposedResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.OriginalSection;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RemovedSkillView;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteEditService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteResult;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewritePhase;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteProgressService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteQueryService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.JobHighlight;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.RewriteRateLimitExceededException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RewriteController.class)
@Import(RewriteWebMapperImpl.class)
class RewriteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RewriteService rewriteService;

	@MockitoBean
	private RewriteEditService rewriteEditService;

	@MockitoBean
	private RewriteProgressService rewriteProgressService;

	@MockitoBean
	private RewriteQueryService rewriteQueryService;

	@Test
	void reportsTheCurrentPhaseOfARewriteInProgress() throws Exception {
		AnalysisId analysisId = AnalysisId.generate();
		when(rewriteProgressService.currentPhase(analysisId)).thenReturn(Optional.of(RewritePhase.EXPORTING));

		mockMvc.perform(get("/api/v1/analyses/{id}/rewrite/progress", analysisId.value()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.phase").value("EXPORTING"));
	}

	@Test
	void returnsTheGeneratedVersionsAndTheRewrittenBulletsDefaultingToTheClassicTemplate() throws Exception {
		AnalysisId analysisId = AnalysisId.generate();
		UUID resumeId = UUID.randomUUID();
		UUID docxVersionId = UUID.randomUUID();
		UUID pdfVersionId = UUID.randomUUID();
		when(rewriteService.rewrite(eq(analysisId), eq(ResumeTemplate.CLASSIC), eq(JobHighlight.AUTO))).thenReturn(
				new RewriteResult(
						resumeId, docxVersionId, pdfVersionId,
						List.of(new BulletRewriteView("original", "reescrito", false)), 60, 85, true,
						List.of(new RemovedSkillView("Preparar café", "Sem relação com a vaga")),
						new StructuredResume("Ana Silva", "Backend Java", List.of()),
						new ResumeContact("ana@email.com", null, null, null, null, "Panambi, RS"),
						List.of(new OriginalSection("RESUMO", "Desenvolvedora backend.")),
						List.of(new RequirementEvidence("mensageria", "Kafka")),
						"OpenAI", "gpt-4o-mini", ResumeTemplate.CLASSIC));

		mockMvc.perform(post("/api/v1/analyses/{id}/rewrite", analysisId.value()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resumeId").value(resumeId.toString()))
				.andExpect(jsonPath("$.documents.docxVersionId").value(docxVersionId.toString()))
				.andExpect(jsonPath("$.documents.pdfVersionId").value(pdfVersionId.toString()))
				.andExpect(jsonPath("$.bullets.items[0].original").value("original"))
				.andExpect(jsonPath("$.bullets.items[0].rewritten").value("reescrito"))
				.andExpect(jsonPath("$.bullets.items[0].needsConfirmation").value(false))
				.andExpect(jsonPath("$.scoreComparison.before").value(60))
				.andExpect(jsonPath("$.scoreComparison.after").value(85))
				.andExpect(jsonPath("$.jobHighlighted").value(true))
				.andExpect(jsonPath("$.removedSkills[0].skill").value("Preparar café"))
				.andExpect(jsonPath("$.removedSkills[0].reason").value("Sem relação com a vaga"))
				.andExpect(jsonPath("$.content.name").value("Ana Silva"))
				.andExpect(jsonPath("$.contact.location").value("Panambi, RS"))
				.andExpect(jsonPath("$.originalSections[0].text").value("Desenvolvedora backend."))
				.andExpect(jsonPath("$.evidencedRequirements[0].requirement").value("mensageria"))
				.andExpect(jsonPath("$.aiProvider").value("OpenAI"))
				.andExpect(jsonPath("$.aiModel").value("gpt-4o-mini"));
	}

	@Test
	void usesTheTemplateChosenInTheRequestBody() throws Exception {
		AnalysisId analysisId = AnalysisId.generate();
		when(rewriteService.rewrite(eq(analysisId), eq(ResumeTemplate.MODERN_BLUE), eq(JobHighlight.AUTO))).thenReturn(
				new RewriteResult(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), List.of(), null, null, false, List.of(), null, null, List.of(), List.of(), null, null, null));

		mockMvc.perform(post("/api/v1/analyses/{id}/rewrite", analysisId.value())
						.contentType("application/json")
						.content("{\"template\":\"MODERN_BLUE\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void turnsTheJobHighlightOffWhenTheRequestAsksForIt() throws Exception {
		AnalysisId analysisId = AnalysisId.generate();
		when(rewriteService.rewrite(eq(analysisId), eq(ResumeTemplate.CLASSIC), eq(JobHighlight.DISABLED))).thenReturn(
				new RewriteResult(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), List.of(), null, null, false, List.of(), null, null, List.of(), List.of(), null, null, null));

		mockMvc.perform(post("/api/v1/analyses/{id}/rewrite", analysisId.value())
						.contentType("application/json")
						.content("{\"highlightJob\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.jobHighlighted").value(false));
	}

	@Test
	void translatesRateLimitIntoTooManyRequests() throws Exception {
		AnalysisId analysisId = AnalysisId.generate();
		when(rewriteService.rewrite(eq(analysisId), eq(ResumeTemplate.CLASSIC), eq(JobHighlight.AUTO)))
				.thenThrow(new RewriteRateLimitExceededException());

		mockMvc.perform(post("/api/v1/analyses/{id}/rewrite", analysisId.value())).andExpect(status().isTooManyRequests());
	}

	@Test
	void generatesNewVersionsFromTheContentTheUserEdited() throws Exception {
		AnalysisId analysisId = AnalysisId.generate();
		UUID resumeId = UUID.randomUUID();
		UUID docxVersionId = UUID.randomUUID();
		UUID pdfVersionId = UUID.randomUUID();
		when(rewriteEditService.saveEdited(eq(analysisId), eq(ResumeTemplate.MODERN_BLUE), any(), any()))
				.thenReturn(new ComposedResume(resumeId, docxVersionId, pdfVersionId));

		mockMvc.perform(post("/api/v1/analyses/{id}/rewrite/documents", analysisId.value())
						.contentType("application/json")
						.content("""
								{"template":"MODERN_BLUE",
								 "content":{"name":"Ana Silva","headline":"Backend","sections":[]},
								 "contact":{"email":"ana@email.com","location":"Panambi, RS"}}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resumeId").value(resumeId.toString()))
				.andExpect(jsonPath("$.documents.docxVersionId").value(docxVersionId.toString()))
				.andExpect(jsonPath("$.documents.pdfVersionId").value(pdfVersionId.toString()));
	}

	@Test
	void rejectsAnEditWithoutContent() throws Exception {
		mockMvc.perform(post("/api/v1/analyses/{id}/rewrite/documents", AnalysisId.generate().value())
						.contentType("application/json")
						.content("{\"template\":\"CLASSIC\",\"contact\":{}}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsTheSavedAdaptationWithItsTemplate() throws Exception {
		AnalysisId analysisId = new AnalysisId(UUID.randomUUID());
		UUID resumeId = UUID.randomUUID();
		when(rewriteQueryService.latest(analysisId)).thenReturn(Optional.of(new RewriteResult(resumeId, UUID.randomUUID(),
				UUID.randomUUID(), List.of(), 60, 85, true, List.of(), new StructuredResume("Ana Silva", null, List.of()),
				null, List.of(), List.of(), null, "gpt-4o-mini", ResumeTemplate.MODERN_BLUE)));

		mockMvc.perform(get("/api/v1/analyses/{id}/rewrite", analysisId.value()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resumeId").value(resumeId.toString()))
				.andExpect(jsonPath("$.template").value("MODERN_BLUE"))
				.andExpect(jsonPath("$.content.name").value("Ana Silva"))
				.andExpect(jsonPath("$.scoreComparison.after").value(85));
	}

	@Test
	void answersNoContentWhenTheResumeWasNotAdaptedYet() throws Exception {
		AnalysisId analysisId = new AnalysisId(UUID.randomUUID());
		when(rewriteQueryService.latest(analysisId)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/analyses/{id}/rewrite", analysisId.value())).andExpect(status().isNoContent());
	}
}
