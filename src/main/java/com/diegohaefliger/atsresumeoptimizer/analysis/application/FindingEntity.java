package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Finding;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "finding")
class FindingEntity {

	@Id
	private UUID id;

	@Column(name = "analysis_id")
	private UUID analysisId;

	@Enumerated(EnumType.STRING)
	private Dimension dimension;

	@Enumerated(EnumType.STRING)
	private Severity severity;

	private String code;

	private String message;

	private String suggestion;

	@Column(name = "start_offset")
	private Integer startOffset;

	@Column(name = "end_offset")
	private Integer endOffset;

	private String excerpt;

	protected FindingEntity() {
	}

	FindingEntity(UUID id, UUID analysisId, Dimension dimension, Finding finding) {
		this.id = id;
		this.analysisId = analysisId;
		this.dimension = dimension;
		this.severity = finding.severity();
		this.code = finding.code().name();
		this.message = finding.message();
		this.suggestion = finding.suggestion();
		this.startOffset = finding.startOffset();
		this.endOffset = finding.endOffset();
		this.excerpt = finding.excerpt();
	}

	Dimension dimension() {
		return dimension;
	}

	Severity severity() {
		return severity;
	}

	String code() {
		return code;
	}

	String message() {
		return message;
	}

	String suggestion() {
		return suggestion;
	}

	Integer startOffset() {
		return startOffset;
	}

	Integer endOffset() {
		return endOffset;
	}
}
