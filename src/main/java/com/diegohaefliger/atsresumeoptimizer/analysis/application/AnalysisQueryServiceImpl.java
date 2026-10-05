package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceMatchService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionText;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class AnalysisQueryServiceImpl implements AnalysisQueryService {

	private final AnalysisRepository analysisRepository;
	private final AnalysisDimensionRepository dimensionRepository;
	private final FindingRepository findingRepository;
	private final KeywordMatchRepository keywordMatchRepository;
	private final ResumeService resumeService;
	private final JobStructuringService jobStructuringService;
	private final JobPreferenceMatchService preferenceMatchService;
	private final AnalysisJobViewMapper jobViewMapper;

	AnalysisQueryServiceImpl(
			AnalysisRepository analysisRepository,
			AnalysisDimensionRepository dimensionRepository,
			FindingRepository findingRepository,
			KeywordMatchRepository keywordMatchRepository,
			ResumeService resumeService,
			JobStructuringService jobStructuringService,
			JobPreferenceMatchService preferenceMatchService,
			AnalysisJobViewMapper jobViewMapper) {
		this.analysisRepository = analysisRepository;
		this.dimensionRepository = dimensionRepository;
		this.findingRepository = findingRepository;
		this.keywordMatchRepository = keywordMatchRepository;
		this.resumeService = resumeService;
		this.jobStructuringService = jobStructuringService;
		this.preferenceMatchService = preferenceMatchService;
		this.jobViewMapper = jobViewMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public AnalysisReportView get(AnalysisId id) {
		AnalysisEntity analysis = find(id);

		List<DimensionView> dimensions = dimensionRepository.findByAnalysisId(id.value()).stream()
				.map(dimension -> new DimensionView(dimension.dimension().name(), dimension.score(), dimension.weight()))
				.toList();

		List<FindingEntity> findingEntities = findingRepository.findByAnalysisId(id.value());
		List<FindingView> findings = findingEntities.stream()
				.map(finding -> new FindingView(finding.code(), finding.severity().name(), finding.dimension().name(),
						finding.message(), finding.suggestion(), new OffsetRange(finding.startOffset(), finding.endOffset())))
				.toList();
		List<String> blockers = findingEntities.stream()
				.map(FindingEntity::code)
				.filter(code -> FindingCode.valueOf(code).blocker())
				.distinct()
				.toList();

		List<KeywordMatchEntity> keywordMatches = keywordMatchRepository.findByAnalysisId(id.value());

		var header = new AnalysisHeaderView(analysis.id(), analysis.status(), analysis.mode(),
				StringUtils.hasText(analysis.jobDescription()) || StringUtils.hasText(analysis.targetRole()));
		ScoreSummaryView score = analysis.overallScore() == null && dimensions.isEmpty()
				? null
				: new ScoreSummaryView(analysis.overallScore(), dimensions);
		KeywordsPanelView keywords = keywordMatches.isEmpty()
				? null
				: new KeywordsPanelView(
						keywordMatches.stream().filter(KeywordMatchEntity::foundExact).map(KeywordMatchEntity::term).toList(),
						keywordMatches.stream().filter(match -> !match.foundExact() && !match.foundSemantic())
								.map(KeywordMatchEntity::term).toList(),
						keywordMatches.stream().filter(match -> !match.foundExact() && match.foundSemantic())
								.map(KeywordMatchEntity::term).toList());

		Optional<JobOffer> offer = jobOffer(analysis);
		return new AnalysisReportView(header, score, keywords, new FindingsListView(findings), blockers, analysis.error(),
				offer.map(jobViewMapper::toView).orElse(null),
				offer.flatMap(preferenceMatchService::match).orElse(null));
	}

	private Optional<JobOffer> jobOffer(AnalysisEntity analysis) {
		if (analysis.mode() != AnalysisMode.JOB_MATCH || analysis.jobPostingId() == null) {
			return Optional.empty();
		}
		return jobStructuringService.offer(analysis.jobPostingId());
	}

	@Override
	@Transactional(readOnly = true)
	public AnalysisAtsView getAtsView(AnalysisId id) {
		AnalysisEntity analysis = find(id);
		ResumeVersionText text = resumeService.getVersionText(analysis.resumeVersionId());
		return new AnalysisAtsView(text.rawText(), text.structuredText());
	}

	private AnalysisEntity find(AnalysisId id) {
		return analysisRepository.findById(id.value()).orElseThrow(() -> new AnalysisNotFoundException(id));
	}
}
