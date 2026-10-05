package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.job.JobListing;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceMatchService;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecentJobServiceImplTest {

	private static final Instant MONDAY = Instant.parse("2026-09-28T10:00:00Z");
	private static final Instant WEDNESDAY = Instant.parse("2026-09-30T10:00:00Z");
	private static final Instant FRIDAY = Instant.parse("2026-10-02T10:00:00Z");

	@Mock
	private AnalysisRepository analysisRepository;

	@Mock
	private JobStructuringService jobStructuringService;

	@Mock
	private JobPreferenceMatchService preferenceMatchService;

	@InjectMocks
	private RecentJobServiceImpl service;

	@Test
	void listsEveryRegisteredJobEvenBeforeItIsUsedAndUsesTheTypedTargetRoleAsTitle() {
		UUID usedJob = UUID.randomUUID();
		UUID newJob = UUID.randomUUID();
		when(jobStructuringService.listJobs()).thenReturn(List.of(
				new JobListing(offer(newJob, "Engenheiro de Dados", null, null), WEDNESDAY),
				new JobListing(offer(usedJob, "Backend Sênior", "Acme", WorkModel.REMOTE), MONDAY)));
		when(analysisRepository.findLatestJobInputsByMode(AnalysisMode.JOB_MATCH))
				.thenReturn(List.of(new RecentJobInput(usedJob, "Backend Sênior (Billing)", "vaga", FRIDAY)));
		when(preferenceMatchService.matchAll(anyCollection()))
				.thenReturn(Map.of(usedJob, new PreferenceMatch(75, List.of())));

		List<RecentJobView> jobs = service.recentJobs();

		assertThat(jobs).extracting(RecentJobView::id, RecentJobView::title, RecentJobView::lastUsedAt,
				RecentJobView::company, RecentJobView::preferenceScore)
				.containsExactly(
						tuple(usedJob, "Backend Sênior (Billing)", FRIDAY, "Acme", 75),
						tuple(newJob, "Engenheiro de Dados", null, null, null));
	}

	@Test
	void removeHidesTheJobFromTheList() {
		UUID jobId = UUID.randomUUID();

		service.remove(jobId);

		verify(jobStructuringService).hideFromRecent(jobId);
	}

	private JobOffer offer(UUID id, String title, String company, WorkModel workModel) {
		return new JobOffer(id, title, company, null, null, workModel, null, null, null, List.of(), null, null, "vaga");
	}
}
