package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "analysis_dimension")
class AnalysisDimensionEntity {

	@Id
	private UUID id;

	@Column(name = "analysis_id")
	private UUID analysisId;

	@Enumerated(EnumType.STRING)
	private Dimension dimension;

	private int score;

	private BigDecimal weight;

	protected AnalysisDimensionEntity() {
	}

	AnalysisDimensionEntity(UUID id, UUID analysisId, Dimension dimension, int score, BigDecimal weight) {
		this.id = id;
		this.analysisId = analysisId;
		this.dimension = dimension;
		this.score = score;
		this.weight = weight;
	}

	Dimension dimension() {
		return dimension;
	}

	int score() {
		return score;
	}

	BigDecimal weight() {
		return weight;
	}
}
