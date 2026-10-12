package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.CoverLetterBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.CoverLetterDraft;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisQueryService;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetter;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterJobNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterRequiresJobResumeException;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionText;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoverLetterServiceImplTest {

	private static final UUID ANALYSIS_ID = UUID.randomUUID();
	private static final UUID JOB_ID = UUID.randomUUID();
	private static final UUID VERSION_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");

	@Mock
	private CoverLetterRepository repository;
	@Mock
	private AnalysisQueryService analysisQueryService;
	@Mock
	private JobStructuringService jobStructuringService;
	@Mock
	private ResumeService resumeService;
	@Mock
	private AiPort aiPort;

	private CoverLetterServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CoverLetterServiceImpl(repository, new CoverLetterEntityMapperImpl(), analysisQueryService,
				jobStructuringService, resumeService, aiPort, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void writesANewCoverLetterFromTheChosenResumeKeepingThePreviousOnes() {
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.of(job()));
		when(analysisQueryService.analysisOfGeneratedVersion(JOB_ID, VERSION_ID)).thenReturn(Optional.of(ANALYSIS_ID));
		when(resumeService.getVersionText(VERSION_ID)).thenReturn(new ResumeVersionText("currículo adaptado", null));
		when(jobStructuringService.selectedKeywords(JOB_ID)).thenReturn(List.of("Java", "Kafka"));
		when(aiPort.writeCoverLetter(any())).thenReturn(new AiResult<>(new CoverLetterDraft("  Texto da apresentação.  "),
				new AiUsage("OpenAI", "gpt-4o-mini", 300, 120, new BigDecimal("0.0010"), false)));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CoverLetter saved = service.generate(JOB_ID, VERSION_ID);

		ArgumentCaptor<CoverLetterBrief> brief = ArgumentCaptor.forClass(CoverLetterBrief.class);
		verify(aiPort).writeCoverLetter(brief.capture());
		assertThat(brief.getValue()).isEqualTo(new CoverLetterBrief("Dev Java", "Cielo", "texto da vaga",
				List.of("Java", "Kafka"), "currículo adaptado"));
		assertThat(saved.jobPostingId()).isEqualTo(JOB_ID);
		assertThat(saved.analysisId()).isEqualTo(ANALYSIS_ID);
		assertThat(saved.resumeVersionId()).isEqualTo(VERSION_ID);
		assertThat(saved.content()).isEqualTo("Texto da apresentação.");
		assertThat(saved.aiModel()).isEqualTo("gpt-4o-mini");
		assertThat(saved.createdAt()).isEqualTo(NOW);
		verify(repository, never()).findByIdAndJobPostingId(any(), any());
	}

	@Test
	void refusesAResumeThatWasNotGeneratedForTheJob() {
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.of(job()));
		when(analysisQueryService.analysisOfGeneratedVersion(JOB_ID, VERSION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.generate(JOB_ID, VERSION_ID))
				.isInstanceOf(CoverLetterRequiresJobResumeException.class);
		verifyNoInteractions(aiPort, repository);
	}

	@Test
	void refusesAnUnknownJob() {
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.generate(JOB_ID, VERSION_ID)).isInstanceOf(CoverLetterJobNotFoundException.class);
		verifyNoInteractions(aiPort);
	}

	@Test
	void editsOneCoverLetterOfTheJob() {
		CoverLetterEntity existing = new CoverLetterEntity(JOB_ID, Instant.parse("2026-10-01T00:00:00Z"));
		existing.setContent("antiga");
		when(repository.findByIdAndJobPostingId(existing.getId(), JOB_ID)).thenReturn(Optional.of(existing));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CoverLetter saved = service.update(JOB_ID, existing.getId(), "Texto editado\n");

		assertThat(saved.id()).isEqualTo(existing.getId());
		assertThat(saved.content()).isEqualTo("Texto editado");
		assertThat(saved.updatedAt()).isEqualTo(NOW);
	}

	@Test
	void failsToEditACoverLetterOfAnotherJob() {
		UUID id = UUID.randomUUID();
		when(repository.findByIdAndJobPostingId(id, JOB_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(JOB_ID, id, "Texto")).isInstanceOf(CoverLetterNotFoundException.class);
	}

	@Test
	void listsTheHistoryOfTheJob() {
		CoverLetterEntity newest = new CoverLetterEntity(JOB_ID, NOW);
		newest.setContent("nova");
		CoverLetterEntity oldest = new CoverLetterEntity(JOB_ID, Instant.parse("2026-10-01T00:00:00Z"));
		oldest.setContent("antiga");
		when(repository.findByJobPostingIdOrderByCreatedAtDesc(JOB_ID)).thenReturn(List.of(newest, oldest));

		assertThat(service.list(JOB_ID)).extracting(CoverLetter::content).containsExactly("nova", "antiga");
	}

	private static JobOffer job() {
		return new JobOffer(JOB_ID, 1L, "Dev Java", "Cielo", null, null, null, null, null, null, List.of(), null, null,
				"texto da vaga");
	}
}
