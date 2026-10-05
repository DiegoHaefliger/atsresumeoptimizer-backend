package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.scoring.domain.KeywordMatch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "keyword_match")
class KeywordMatchEntity {

	@Id
	private UUID id;

	@Column(name = "analysis_id")
	private UUID analysisId;

	private String term;

	private String category;

	private boolean required;

	@Column(name = "found_exact")
	private boolean foundExact;

	@Column(name = "found_semantic")
	private boolean foundSemantic;

	private int occurrences;

	@JdbcTypeCode(SqlTypes.ARRAY)
	private List<String> sections;

	protected KeywordMatchEntity() {
	}

	KeywordMatchEntity(UUID id, UUID analysisId, KeywordMatch match) {
		this.id = id;
		this.analysisId = analysisId;
		this.term = match.term();
		this.category = match.category();
		this.required = match.required();
		this.foundExact = match.foundExact();
		this.foundSemantic = match.foundSemantic();
		this.occurrences = match.occurrences();
		this.sections = match.sections();
	}

	String term() {
		return term;
	}

	boolean foundExact() {
		return foundExact;
	}

	boolean foundSemantic() {
		return foundSemantic;
	}
}
