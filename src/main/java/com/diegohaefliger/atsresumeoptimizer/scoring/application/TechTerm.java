package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tech_term")
class TechTerm {

	@Id
	private String canonical;

	@JdbcTypeCode(SqlTypes.ARRAY)
	private List<String> aliases;

	private String category;

	protected TechTerm() {
	}

	String canonical() {
		return canonical;
	}

	List<String> aliases() {
		return aliases == null ? List.of() : aliases;
	}
}
