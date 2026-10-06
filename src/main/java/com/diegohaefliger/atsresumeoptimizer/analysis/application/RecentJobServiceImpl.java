package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.job.JobListing;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceMatchService;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class RecentJobServiceImpl implements RecentJobService {

	private final AnalysisRepository analysisRepository;
	private final JobStructuringService jobStructuringService;
	private final JobPreferenceMatchService preferenceMatchService;
	private final ResumeService resumeService;

	RecentJobServiceImpl(
			AnalysisRepository analysisRepository,
			JobStructuringService jobStructuringService,
			JobPreferenceMatchService preferenceMatchService,
			ResumeService resumeService) {
		this.analysisRepository = analysisRepository;
		this.jobStructuringService = jobStructuringService;
		this.preferenceMatchService = preferenceMatchService;
		this.resumeService = resumeService;
	}

	@Override
	@Transactional(readOnly = true)
	public List<RecentJobView> recentJobs() {
		List<JobListing> listings = jobStructuringService.listJobs();
		Map<UUID, RecentJobInput> lastUses = analysisRepository.findLatestJobInputsByMode(AnalysisMode.JOB_MATCH).stream()
				.collect(Collectors.toMap(RecentJobInput::jobPostingId, Function.identity(), (first, second) -> first));
		Map<UUID, PreferenceMatch> matches =
				preferenceMatchService.matchAll(listings.stream().map(JobListing::offer).toList());
		Map<UUID, Integer> atsScores = latestAtsScoreByJob();
		return listings.stream()
				.map(listing -> view(listing, Optional.ofNullable(lastUses.get(listing.offer().jobPostingId())),
						matches.get(listing.offer().jobPostingId()), atsScores.get(listing.offer().jobPostingId())))
				.sorted(Comparator.comparing(RecentJobServiceImpl::lastActivity).reversed())
				.toList();
	}

	@Override
	public void remove(UUID jobPostingId) {
		jobStructuringService.hideFromRecent(jobPostingId);
	}

	private RecentJobView view(JobListing listing, Optional<RecentJobInput> lastUse, PreferenceMatch match,
			Integer atsScore) {
		JobOffer offer = listing.offer();
		String targetRole = lastUse.map(RecentJobInput::targetRole).orElse(null);
		return new RecentJobView(offer.jobPostingId(), offer.code(), title(targetRole, offer), targetRole, offer.rawText(),
				lastUse.map(RecentJobInput::lastUsedAt).orElse(null), listing.registeredAt(), offer.company(),
				offer.sourceUrl(), offer.workModel(), offer.interviewUrl(),
				offer.salaryMax() != null ? offer.salaryMax() : offer.salaryMin(), offer.benefits(), offer.seniority(), offer.contractType(),
				match != null ? match.score() : null, atsScore);
	}

	private Map<UUID, Integer> latestAtsScoreByJob() {
		Map<UUID, UUID> jobByAnalysis = analysisRepository.findAllJobRefs().stream()
				.collect(Collectors.toMap(AnalysisJobRef::analysisId, AnalysisJobRef::jobPostingId));
		Map<UUID, Integer> scores = new HashMap<>();
		for (ResumeSummary resume : resumeService.listAdaptedFromAnalyses(jobByAnalysis.keySet())) {
			if (resume.atsScore() != null) {
				scores.putIfAbsent(jobByAnalysis.get(resume.sourceAnalysisId()), resume.atsScore());
			}
		}
		return scores;
	}

	private static Instant lastActivity(RecentJobView view) {
		return view.lastUsedAt() != null ? view.lastUsedAt() : view.registeredAt();
	}

	private String title(String targetRole, JobOffer offer) {
		return StringUtils.hasText(targetRole) ? targetRole : offer.title();
	}
}
