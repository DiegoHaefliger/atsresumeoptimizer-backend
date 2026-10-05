package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
class AnalysisEventServiceImpl implements AnalysisEventService {

	private final AnalysisRepository analysisRepository;
	private final AnalysisEventEmitterRegistry emitterRegistry;

	AnalysisEventServiceImpl(AnalysisRepository analysisRepository, AnalysisEventEmitterRegistry emitterRegistry) {
		this.analysisRepository = analysisRepository;
		this.emitterRegistry = emitterRegistry;
	}

	@Override
	public SseEmitter subscribe(AnalysisId id) {
		if (!analysisRepository.existsById(id.value())) {
			throw new AnalysisNotFoundException(id);
		}
		return emitterRegistry.register(id.value());
	}
}
