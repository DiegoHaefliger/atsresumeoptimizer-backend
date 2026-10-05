package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "scoring_profile")
class ScoringProfileEntity {

	@Id
	private UUID id;

	private String name;

	private int version;

	@Enumerated(EnumType.STRING)
	private AnalysisMode mode;

	@JdbcTypeCode(SqlTypes.JSON)
	private String weights;

	private boolean active;

	protected ScoringProfileEntity() {
	}

	UUID id() {
		return id;
	}

	String name() {
		return name;
	}

	int version() {
		return version;
	}

	String weights() {
		return weights;
	}
}
