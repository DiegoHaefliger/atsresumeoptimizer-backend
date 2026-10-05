package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AnalysisEventService {

	SseEmitter subscribe(AnalysisId id);
}
