package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "resume_content")
class ResumeContentEntity {

	@Id
	private UUID id;

	@Column(name = "resume_id")
	private UUID resumeId;

	@Column(name = "docx_version_id")
	private UUID docxVersionId;

	@Column(name = "pdf_version_id")
	private UUID pdfVersionId;

	@Enumerated(EnumType.STRING)
	private ResumeTemplate template;

	@JdbcTypeCode(SqlTypes.JSON)
	private String document;

	@Column(name = "created_at")
	private Instant createdAt;

	protected ResumeContentEntity() {
	}

	ResumeContentEntity(
			UUID id, UUID resumeId, UUID docxVersionId, UUID pdfVersionId, ResumeTemplate template, String document) {
		this.id = id;
		this.resumeId = resumeId;
		this.docxVersionId = docxVersionId;
		this.pdfVersionId = pdfVersionId;
		this.template = template;
		this.document = document;
		this.createdAt = Instant.now();
	}

	ResumeTemplate template() {
		return template;
	}

	String document() {
		return document;
	}
}
