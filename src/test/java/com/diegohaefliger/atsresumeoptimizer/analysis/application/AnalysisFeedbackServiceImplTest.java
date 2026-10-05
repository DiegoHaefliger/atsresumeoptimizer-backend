package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisFeedbackServiceImplTest {

	@Mock
	private AnalysisRepository analysisRepository;
	@Mock
	private AnalysisFeedbackRepository feedbackRepository;

	private AnalysisFeedbackServiceImpl service;

	@Test
	void rejectsFeedbackForAnAnalysisThatDoesNotExist() {
		service = new AnalysisFeedbackServiceImpl(analysisRepository, feedbackRepository);
		AnalysisId id = AnalysisId.generate();
		when(analysisRepository.existsById(id.value())).thenReturn(false);

		assertThatThrownBy(() -> service.submit(id, 4, "comentario"))
				.isInstanceOf(AnalysisNotFoundException.class);
		verify(feedbackRepository, never()).save(any());
	}

	@Test
	void savesFeedbackForAnExistingAnalysis() {
		service = new AnalysisFeedbackServiceImpl(analysisRepository, feedbackRepository);
		AnalysisId id = AnalysisId.generate();
		when(analysisRepository.existsById(id.value())).thenReturn(true);

		service.submit(id, 5, "muito bom");

		verify(feedbackRepository).save(any());
	}
}
