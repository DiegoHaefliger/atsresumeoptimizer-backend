package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class AnalysisEventServiceImplTest {

	@Mock
	private AnalysisRepository analysisRepository;
	@Mock
	private AnalysisEventEmitterRegistry emitterRegistry;

	private AnalysisEventServiceImpl service;

	@Test
	void registersAnEmitterForTheAnalysis() {
		service = new AnalysisEventServiceImpl(analysisRepository, emitterRegistry);
		UUID analysisId = UUID.randomUUID();
		when(analysisRepository.existsById(analysisId)).thenReturn(true);
		SseEmitter expectedEmitter = new SseEmitter();
		when(emitterRegistry.register(analysisId)).thenReturn(expectedEmitter);

		SseEmitter emitter = service.subscribe(new AnalysisId(analysisId));

		assertThat(emitter).isSameAs(expectedEmitter);
	}

	@Test
	void returns404StyleExceptionWhenTheAnalysisDoesNotExist() {
		service = new AnalysisEventServiceImpl(analysisRepository, emitterRegistry);
		when(analysisRepository.existsById(any())).thenReturn(false);

		assertThatThrownBy(() -> service.subscribe(new AnalysisId(UUID.randomUUID())))
				.isInstanceOf(AnalysisNotFoundException.class);
	}
}
