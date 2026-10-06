package com.diegohaefliger.atsresumeoptimizer.analysis.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisCostReportService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisCreation;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisEventService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisFeedbackService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisHeaderView;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisQueryService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisReportView;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.CostReport;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.FindingsListView;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.KeywordsPanelView;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.RecentJobService;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.RecentJobView;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.ScoreSummaryView;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.resume.domain.EmptyResumeFileException;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeOrigin;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalysesController.class)
@Import(JobDetailsMapperImpl.class)
class AnalysesControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AnalysisService analysisService;

	@MockitoBean
	private AnalysisQueryService analysisQueryService;

	@MockitoBean
	private AnalysisFeedbackService analysisFeedbackService;

	@MockitoBean
	private AnalysisCostReportService analysisCostReportService;

	@MockitoBean
	private AnalysisEventService analysisEventService;

	@MockitoBean
	private RecentJobService recentJobService;

	@Test
	void delegatesToServiceAndReturns202WithAnalysisId() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", "conteudo".getBytes());
		AnalysisId id = AnalysisId.generate();
		when(analysisService.create(any(), any(), eq("vaga"), eq("Backend Java"), eq(JobDetails.NONE)))
				.thenReturn(new AnalysisCreation(id, AnalysisStatus.PENDING));

		mockMvc.perform(multipart("/api/v1/analyses")
						.file(file)
						.param("jobDescription", "vaga")
						.param("targetRole", "Backend Java"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.id").value(id.value().toString()))
				.andExpect(jsonPath("$.status").value("PENDING"));

		verify(analysisService).create(any(), any(), eq("vaga"), eq("Backend Java"), eq(JobDetails.NONE));
	}

	@Test
	void rejectsMissingFile() throws Exception {
		mockMvc.perform(multipart("/api/v1/analyses"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void translatesBusinessExceptionIntoProblemDetail() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", new byte[0]);
		when(analysisService.create(any(), any(), eq((String) null), eq((String) null), any()))
				.thenThrow(new EmptyResumeFileException());

		mockMvc.perform(multipart("/api/v1/analyses").file(file))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsTheAnalysisResultByIdView() throws Exception {
		AnalysisId id = AnalysisId.generate();
		var result = new AnalysisReportView(
				new AnalysisHeaderView(id.value(), AnalysisStatus.COMPLETED, AnalysisMode.JOB_MATCH, true),
				new ScoreSummaryView(71, List.of()),
				new KeywordsPanelView(List.of(), List.of(), List.of(), List.of(), List.of(), false),
				new FindingsListView(List.of()),
				List.of(),
				null,
				null,
				null);
		when(analysisQueryService.get(id)).thenReturn(result);

		mockMvc.perform(get("/api/v1/analyses/{id}", id.value()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.header.status").value("COMPLETED"))
				.andExpect(jsonPath("$.score.overall").value(71));
	}

	@Test
	void acceptsFeedbackAndReturns204() throws Exception {
		AnalysisId id = AnalysisId.generate();

		mockMvc.perform(post("/api/v1/analyses/{id}/feedback", id.value())
						.contentType("application/json")
						.content("{\"rating\":4,\"comment\":\"score fez sentido\"}"))
				.andExpect(status().isNoContent());

		verify(analysisFeedbackService).submit(id, 4, "score fez sentido");
	}

	@Test
	void rejectsARatingOutsideOneToFive() throws Exception {
		AnalysisId id = AnalysisId.generate();
		mockMvc.perform(post("/api/v1/analyses/{id}/feedback", id.value())
						.contentType("application/json")
						.content("{\"rating\":9,\"comment\":null}"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(analysisFeedbackService);
	}

	@Test
	void translatesFeedbackForMissingAnalysisInto404() throws Exception {
		AnalysisId id = AnalysisId.generate();
		doThrow(new AnalysisNotFoundException(id))
				.when(analysisFeedbackService).submit(eq(id), any(Integer.class), any());

		mockMvc.perform(post("/api/v1/analyses/{id}/feedback", id.value())
						.contentType("application/json")
						.content("{\"rating\":5,\"comment\":null}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void returnsTheCostReport() throws Exception {
		when(analysisCostReportService.report())
				.thenReturn(new CostReport(3, BigDecimal.valueOf(1.5), BigDecimal.valueOf(0.5), List.of()));

		mockMvc.perform(get("/api/v1/analyses/cost-report"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.analysesWithAiUsage").value(3))
				.andExpect(jsonPath("$.avgCostUsd").value(0.5));
	}

	@Test
	void listsTheRecentJobsWithTitleAndId() throws Exception {
		UUID jobId = UUID.randomUUID();
		when(recentJobService.recentJobs()).thenReturn(List.of(new RecentJobView(jobId, 7L, "Backend Java", "Backend Java",
				"Vaga de backend", Instant.parse("2026-10-03T13:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"), "Acme", "https://acme.com/vaga", WorkModel.REMOTE,
				"https://meet.example.com/1", new java.math.BigDecimal("8000"), List.of("VR"), "Pleno", com.diegohaefliger.atsresumeoptimizer.job.ContractType.CLT, 80)));

		mockMvc.perform(get("/api/v1/analyses/recent-jobs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(jobId.toString()))
				.andExpect(jsonPath("$[0].title").value("Backend Java"))
				.andExpect(jsonPath("$[0].code").value(7))
				.andExpect(jsonPath("$[0].jobDescription").value("Vaga de backend"))
				.andExpect(jsonPath("$[0].company").value("Acme"))
				.andExpect(jsonPath("$[0].interviewUrl").value("https://meet.example.com/1"))
				.andExpect(jsonPath("$[0].salary").value(8000))
				.andExpect(jsonPath("$[0].benefits[0]").value("VR"))
				.andExpect(jsonPath("$[0].contractType").value("CLT"))
				.andExpect(jsonPath("$[0].workModel").value("REMOTE"))
				.andExpect(jsonPath("$[0].preferenceScore").value(80));
	}

	@Test
	void removesAJobFromTheRecentList() throws Exception {
		UUID jobId = UUID.randomUUID();

		mockMvc.perform(delete("/api/v1/analyses/recent-jobs/{jobId}", jobId))
				.andExpect(status().isNoContent());

		verify(recentJobService).remove(jobId);
	}

	@Test
	void passesTheJobDetailsTypedByTheUserToTheService() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", "conteudo".getBytes());
		var details = new JobDetails("Acme", "https://acme.com/vagas/1", WorkModel.HYBRID, null, null, null, null, null, null);
		when(analysisService.create(any(), any(), eq("vaga"), eq((String) null), eq(details)))
				.thenReturn(new AnalysisCreation(AnalysisId.generate(), AnalysisStatus.PENDING));

		mockMvc.perform(multipart("/api/v1/analyses")
						.file(file)
						.param("jobDescription", "vaga")
						.param("jobCompany", "Acme")
						.param("jobUrl", "https://acme.com/vagas/1")
						.param("jobWorkModel", "HYBRID"))
				.andExpect(status().isAccepted());

		verify(analysisService).create(any(), any(), eq("vaga"), eq((String) null), eq(details));
	}

	@Test
	void rejectsAJobUrlThatIsNotAnHttpLink() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", "conteudo".getBytes());

		mockMvc.perform(multipart("/api/v1/analyses")
						.file(file)
						.param("jobDescription", "vaga")
						.param("jobUrl", "javascript:alert(1)"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listsTheResumesGeneratedForAJob() throws Exception {
		UUID jobId = UUID.randomUUID();
		UUID resumeId = UUID.randomUUID();
		when(analysisQueryService.generatedResumes(jobId)).thenReturn(List.of(new ResumeSummary(resumeId,
				"Currículo - Vaga A", Instant.now(), ResumeOrigin.ADAPTED, UUID.randomUUID(), false, 83, List.of())));

		mockMvc.perform(get("/api/v1/jobs/{jobId}/resumes", jobId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(resumeId.toString()))
				.andExpect(jsonPath("$[0].origin").value("ADAPTED"))
				.andExpect(jsonPath("$[0].atsScore").value(83));
	}
}
