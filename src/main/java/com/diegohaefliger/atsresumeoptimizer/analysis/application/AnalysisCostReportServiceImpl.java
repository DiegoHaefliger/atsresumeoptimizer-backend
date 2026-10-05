package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AnalysisCostReportServiceImpl implements AnalysisCostReportService {

	private final AnalysisRepository analysisRepository;

	AnalysisCostReportServiceImpl(AnalysisRepository analysisRepository) {
		this.analysisRepository = analysisRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public CostReport report() {
		List<CostByModel> byModel = analysisRepository.aggregateCostByModel().stream()
				.map(projection -> new CostByModel(
						projection.getModel(), projection.getAnalysisCount(), projection.getTotalCostUsd(),
						projection.getAvgCostUsd()))
				.toList();

		long totalAnalyses = byModel.stream().mapToLong(CostByModel::analysisCount).sum();
		BigDecimal totalCostUsd = byModel.stream().map(CostByModel::totalCostUsd).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal avgCostUsd = totalAnalyses == 0
				? BigDecimal.ZERO
				: totalCostUsd.divide(BigDecimal.valueOf(totalAnalyses), 4, RoundingMode.HALF_UP);

		return new CostReport(totalAnalyses, totalCostUsd, avgCostUsd, byModel);
	}
}
