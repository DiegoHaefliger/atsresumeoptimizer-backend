package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisCostReportServiceImplTest {

	@Mock
	private AnalysisRepository analysisRepository;

	@Test
	void aggregatesCostAndCountAcrossModels() {
		AnalysisCostReportServiceImpl service = new AnalysisCostReportServiceImpl(analysisRepository);

		CostByModelProjection gptMini = mock(CostByModelProjection.class);
		when(gptMini.getModel()).thenReturn("gpt-4o-mini");
		when(gptMini.getAnalysisCount()).thenReturn(3L);
		when(gptMini.getTotalCostUsd()).thenReturn(BigDecimal.valueOf(1.50));
		when(gptMini.getAvgCostUsd()).thenReturn(BigDecimal.valueOf(0.50));
		when(analysisRepository.aggregateCostByModel()).thenReturn(List.of(gptMini));

		CostReport report = service.report();

		assertThat(report.analysesWithAiUsage()).isEqualTo(3);
		assertThat(report.totalCostUsd()).isEqualByComparingTo("1.50");
		assertThat(report.avgCostUsd()).isEqualByComparingTo("0.5000");
		assertThat(report.byModel()).hasSize(1);
	}

	@Test
	void returnsZeroedReportWhenNoAnalysisHasAiUsageYet() {
		AnalysisCostReportServiceImpl service = new AnalysisCostReportServiceImpl(analysisRepository);
		when(analysisRepository.aggregateCostByModel()).thenReturn(List.of());

		CostReport report = service.report();

		assertThat(report.analysesWithAiUsage()).isZero();
		assertThat(report.avgCostUsd()).isEqualByComparingTo("0");
	}
}
