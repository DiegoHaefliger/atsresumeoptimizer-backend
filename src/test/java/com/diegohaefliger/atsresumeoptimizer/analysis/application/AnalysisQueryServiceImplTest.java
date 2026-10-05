package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.CriterionMatch;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceMatchService;
import com.diegohaefliger.atsresumeoptimizer.preference.MatchStatus;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceCriterion;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionText;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisQueryServiceImplTest {

	@Mock
	private AnalysisRepository analysisRepository;
	@Mock
	private AnalysisDimensionRepository dimensionRepository;
	@Mock
	private FindingRepository findingRepository;
	@Mock
	private KeywordMatchRepository keywordMatchRepository;
	@Mock
	private ResumeService resumeService;
	@Mock
	private JobStructuringService jobStructuringService;
	@Mock
	private JobPreferenceMatchService preferenceMatchService;

	@Test
	void returns404StyleExceptionWhenTheAnalysisDoesNotExistAtAll() {
		var service = newService();
		AnalysisId id = AnalysisId.generate();
		when(analysisRepository.findById(id.value())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(id)).isInstanceOf(AnalysisNotFoundException.class);
	}

	@Test
	void includesTheJobDetailsAndThePreferenceMatchOfAJobMatchAnalysis() {
		var service = newService();
		AnalysisId id = AnalysisId.generate();
		UUID jobPostingId = UUID.randomUUID();
		AnalysisEntity analysis = mock(AnalysisEntity.class);
		when(analysis.id()).thenReturn(id.value());
		when(analysis.mode()).thenReturn(AnalysisMode.JOB_MATCH);
		when(analysis.jobPostingId()).thenReturn(jobPostingId);
		when(analysisRepository.findById(id.value())).thenReturn(Optional.of(analysis));
		JobOffer offer = new JobOffer(jobPostingId, "Backend Java", "Acme", "https://acme.com/vaga", null, WorkModel.REMOTE,
				null, null, null, List.of(), null, null, "vaga");
		when(jobStructuringService.offer(jobPostingId)).thenReturn(Optional.of(offer));
		var match = new PreferenceMatch(100,
				List.of(new CriterionMatch(PreferenceCriterion.WORK_MODEL, MatchStatus.MATCH, "Vaga: Remoto")));
		when(preferenceMatchService.match(offer)).thenReturn(Optional.of(match));

		AnalysisReportView view = service.get(id);

		assertThat(view.job()).isEqualTo(new AnalysisJobView("Backend Java", "Acme", "https://acme.com/vaga", null, WorkModel.REMOTE));
		assertThat(view.preferenceMatch()).isEqualTo(match);
	}

	@Test
	void leavesJobAndPreferenceMatchEmptyForAGeneralAnalysis() {
		var service = newService();
		AnalysisId id = AnalysisId.generate();
		AnalysisEntity analysis = mock(AnalysisEntity.class);
		when(analysis.mode()).thenReturn(AnalysisMode.GENERAL);
		when(analysisRepository.findById(id.value())).thenReturn(Optional.of(analysis));

		AnalysisReportView view = service.get(id);

		assertThat(view.job()).isNull();
		assertThat(view.preferenceMatch()).isNull();
	}

	@Test
	void returnsTheRawAndStructuredTextOfTheResumeVersionUsedInTheAnalysis() {
		var service = newService();
		AnalysisId id = AnalysisId.generate();
		UUID resumeVersionId = UUID.randomUUID();
		AnalysisEntity analysis = mock(AnalysisEntity.class);
		when(analysis.resumeVersionId()).thenReturn(resumeVersionId);
		when(analysisRepository.findById(id.value())).thenReturn(Optional.of(analysis));
		when(resumeService.getVersionText(resumeVersionId)).thenReturn(new ResumeVersionText("cru", "estruturado"));

		AnalysisAtsView view = service.getAtsView(id);

		assertThat(view.rawText()).isEqualTo("cru");
		assertThat(view.structuredText()).isEqualTo("estruturado");
	}

	@Test
	void returns404StyleExceptionForAtsViewWhenTheAnalysisDoesNotExistAtAll() {
		var service = newService();
		AnalysisId id = AnalysisId.generate();
		when(analysisRepository.findById(id.value())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getAtsView(id)).isInstanceOf(AnalysisNotFoundException.class);
	}

	private AnalysisQueryServiceImpl newService() {
		return new AnalysisQueryServiceImpl(analysisRepository, dimensionRepository, findingRepository,
				keywordMatchRepository, resumeService, jobStructuringService, preferenceMatchService,
				new AnalysisJobViewMapperImpl());
	}
}
