package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshotPort;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionOrigin;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeExportNameTest {

	private static final UUID RESUME_ID = UUID.randomUUID();
	private static final UUID VERSION_ID = UUID.randomUUID();
	private static final UUID ANALYSIS_ID = UUID.randomUUID();
	private static final UUID JOB_ID = UUID.randomUUID();

	@Mock
	private ResumeService resumeService;
	@Mock
	private AnalysisSnapshotPort analysisSnapshots;
	@Mock
	private JobStructuringService jobStructuringService;

	@InjectMocks
	private ResumeExportName exportName;

	@Test
	void joinsCandidateCompanyJobCodeAndVersionInCamelCase() {
		adaptedFor(job(1L, "Cielo S.A."), 1);

		assertThat(exportName.baseName(RESUME_ID, VERSION_ID, "Diego Haefliger"))
				.isEqualTo("DiegoHaefliger_CieloSA_V0001_1");
	}

	@Test
	void leavesCompanyAndCodeOutWhenTheResumeWasNotAdaptedForAJob() {
		when(resumeService.origin(RESUME_ID, VERSION_ID)).thenReturn(new ResumeVersionOrigin(3, Optional.empty()));

		assertThat(exportName.baseName(RESUME_ID, VERSION_ID, "Ana Silva")).isEqualTo("AnaSilva_3");
		verifyNoInteractions(analysisSnapshots, jobStructuringService);
	}

	@Test
	void skipsTheMissingPartsOfTheJob() {
		adaptedFor(job(null, null), 2);

		assertThat(exportName.baseName(RESUME_ID, VERSION_ID, "Ana Silva")).isEqualTo("AnaSilva_2");
	}

	@Test
	void fallsBackToTheResumeTitleWithoutTheCandidateName() {
		when(resumeService.origin(RESUME_ID, VERSION_ID)).thenReturn(new ResumeVersionOrigin(1, Optional.empty()));
		when(resumeService.title(RESUME_ID)).thenReturn("meu currículo.pdf");

		assertThat(exportName.baseName(RESUME_ID, VERSION_ID, " ")).isEqualTo("MeuCurriculo_1");
	}

	@Test
	void writesWordsWithoutSpacesAccentsOrSymbols() {
		assertThat(ResumeExportName.camelCase("João da Conceição")).isEqualTo("JoaoDaConceicao");
		assertThat(ResumeExportName.camelCase("DIEGO HAEFLIGER")).isEqualTo("DiegoHaefliger");
		assertThat(ResumeExportName.camelCase("IBM")).isEqualTo("IBM");
		assertThat(ResumeExportName.camelCase("iFood / Brasil")).isEqualTo("IFoodBrasil");
		assertThat(ResumeExportName.camelCase(null)).isEmpty();
	}

	private void adaptedFor(JobOffer job, int versionNumber) {
		when(resumeService.origin(RESUME_ID, VERSION_ID))
				.thenReturn(new ResumeVersionOrigin(versionNumber, Optional.of(ANALYSIS_ID)));
		when(analysisSnapshots.jobPostingId(new AnalysisId(ANALYSIS_ID))).thenReturn(Optional.of(JOB_ID));
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.of(job));
	}

	private static JobOffer job(Long code, String company) {
		return new JobOffer(JOB_ID, code, "Dev Java", company, null, null, null, null, null, null, List.of(), null, null,
				null);
	}
}
