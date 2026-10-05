package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisNotFoundException;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Service;

@Service
class AnalysisFeedbackServiceImpl implements AnalysisFeedbackService {

	private final AnalysisRepository analysisRepository;
	private final AnalysisFeedbackRepository feedbackRepository;

	AnalysisFeedbackServiceImpl(AnalysisRepository analysisRepository, AnalysisFeedbackRepository feedbackRepository) {
		this.analysisRepository = analysisRepository;
		this.feedbackRepository = feedbackRepository;
	}

	@Override
	public void submit(AnalysisId analysisId, int rating, String comment) {
		if (!analysisRepository.existsById(analysisId.value())) {
			throw new AnalysisNotFoundException(analysisId);
		}
		feedbackRepository.save(new AnalysisFeedbackEntity(UuidCreator.getTimeOrderedEpoch(), analysisId.value(), rating, comment));
	}
}
