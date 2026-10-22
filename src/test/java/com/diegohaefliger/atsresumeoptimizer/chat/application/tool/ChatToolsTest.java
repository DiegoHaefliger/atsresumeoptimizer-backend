package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisQueryService;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.application.CandidateQuestionService;
import com.diegohaefliger.atsresumeoptimizer.interview.application.InterviewQuestionService;
import com.diegohaefliger.atsresumeoptimizer.job.JobListing;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionSummary;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionText;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeOrigin;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.application.SelectionProcessService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatToolsTest {

	private static final UUID JOB_ID = UUID.randomUUID();
	private static final UUID VERSION_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");
	private static final JobOffer OFFER = new JobOffer(JOB_ID, 12L, "Dev Java Sênior", "Acme", null, null, null, null,
			null, null, List.of("VR"), null, "Sênior", "Procuramos pessoa com Java e Spring.");

	private final JobStructuringService jobService = mock(JobStructuringService.class);
	private final ResumeService resumeService = mock(ResumeService.class);
	private final AnalysisQueryService analyses = mock(AnalysisQueryService.class);
	private final SelectionProcessService processes = mock(SelectionProcessService.class);
	private final ChatJobLookup lookup = new ChatJobLookup(jobService);

	ChatToolsTest() {
		when(jobService.listJobs()).thenReturn(List.of(new JobListing(OFFER, NOW)));
		when(jobService.keywords(JOB_ID)).thenReturn(List.of("Java", "Spring"));
		when(jobService.selectedKeywords(JOB_ID)).thenReturn(List.of("Java"));
	}

	@Test
	void listsTheJobsWithTheirStage() {
		SelectionProcess process = new SelectionProcess(UUID.randomUUID(), JOB_ID, 12L, "Acme", "Dev Java Sênior", null,
				null, SelectionStage.TECHNICAL_INTERVIEW, null, null, "Maria", "maria@acme.com", null, null,
				"nota privada", NOW, NOW, List.of(), List.of(), null);
		when(processes.list(Optional.empty())).thenReturn(List.of(process));

		String result = new ListJobsTool(lookup, processes).run(Map.of());

		assertThat(result).isEqualTo("#12 | Dev Java Sênior | Acme | Sênior | etapa: Entrevista técnica");
	}

	@Test
	void findsAJobByCodeWithOrWithoutTheHash() {
		GetJobTool tool = new GetJobTool(lookup, new JobDescription(jobService, resumeService));

		assertThat(tool.run(Map.of("codigo", "12"))).contains("Vaga #12 — Dev Java Sênior").contains("Java, Spring")
				.contains("Procuramos pessoa com Java e Spring.");
		assertThat(tool.run(Map.of("codigo", "#12"))).contains("Acme");
		assertThat(tool.label(Map.of("codigo", "12"))).isEqualTo("Lendo a vaga #12 — Dev Java Sênior");
	}

	@Test
	void pointsToListJobsWhenTheCodeDoesNotExist() {
		GetJobTool tool = new GetJobTool(lookup, new JobDescription(jobService, resumeService));

		assertThat(tool.run(Map.of("codigo", "99"))).contains("não encontrada").contains("list_jobs");
		assertThat(tool.run(Map.of("codigo", "abc"))).contains("não encontrada");
	}

	@Test
	void returnsTheLatestGeneratedResume() {
		UUID olderVersion = UUID.randomUUID();
		ResumeSummary resume = new ResumeSummary(UUID.randomUUID(), "Currículo", NOW, ResumeOrigin.ADAPTED, UUID.randomUUID(),
				false, 80, List.of(new ResumeVersionSummary(olderVersion, 1, "a.pdf", "application/pdf", 1, 1, NOW.minusSeconds(60)),
						new ResumeVersionSummary(VERSION_ID, 2, "b.pdf", "application/pdf", 1, 1, NOW)));
		when(analyses.generatedResumes(JOB_ID)).thenReturn(List.of(resume));
		when(resumeService.getVersionText(VERSION_ID)).thenReturn(new ResumeVersionText("Fulano, 8 anos de Java", null));

		String result = new GetJobResumeTool(lookup, analyses, resumeService).run(Map.of("codigo", "12"));

		assertThat(result).contains("Fulano, 8 anos de Java");
	}

	@Test
	void saysWhenThereIsNoGeneratedResume() {
		when(analyses.generatedResumes(JOB_ID)).thenReturn(List.of());

		assertThat(new GetJobResumeTool(lookup, analyses, resumeService).run(Map.of("codigo", "12")))
				.contains("Ainda não existe currículo gerado");
	}

	@Test
	void theProcessToolNeverExposesNotesOrContactData() {
		SelectionProcess process = new SelectionProcess(UUID.randomUUID(), JOB_ID, 12L, "Acme", "Dev Java Sênior", null,
				null, SelectionStage.SCREENING, null, null, "Maria Recrutadora", "maria@acme.com", "11999990000", null,
				"anotação privada do usuário", NOW, NOW, List.of(), List.of(), null);
		when(processes.list(Optional.empty())).thenReturn(List.of(process));

		String result = new GetJobProcessTool(lookup, processes).run(Map.of("codigo", "12"));

		assertThat(result).contains("Triagem").doesNotContain("Maria").doesNotContain("maria@acme.com")
				.doesNotContain("11999990000").doesNotContain("anotação privada");
	}

	@Test
	void declaresTheCodeAsTheOnlyParameterOfEveryJobTool() {
		assertThat(new GetJobTool(lookup, new JobDescription(jobService, resumeService)).parameters())
				.extracting(parameter -> parameter.name()).containsExactly("codigo");
		assertThat(new GetJobProcessTool(lookup, processes).parameters()).hasSize(1);
		assertThat(new ListJobsTool(lookup, processes).parameters()).isEmpty();
	}

	@Test
	void interviewPrepSaysWhenNothingWasGeneratedYet() {
		InterviewQuestionService interview = mock(InterviewQuestionService.class);
		CandidateQuestionService candidate = mock(CandidateQuestionService.class);
		when(interview.list(JOB_ID)).thenReturn(List.of());
		when(candidate.list(JOB_ID)).thenReturn(List.of());

		assertThat(new GetInterviewPrepTool(lookup, interview, candidate).run(Map.of("codigo", "12")))
				.contains("Ainda não foram geradas");
	}
}
