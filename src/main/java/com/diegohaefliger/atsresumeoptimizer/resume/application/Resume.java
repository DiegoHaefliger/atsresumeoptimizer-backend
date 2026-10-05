package com.diegohaefliger.atsresumeoptimizer.resume.application;

import com.diegohaefliger.atsresumeoptimizer.resume.ResumeOrigin;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume")
class Resume {

	static final String SCRUBBED_PLACEHOLDER = ResumeVersion.SCRUBBED_PLACEHOLDER;

	@Id
	private UUID id;

	private String title;

	@Enumerated(EnumType.STRING)
	private ResumeOrigin origin;

	@Column(name = "source_analysis_id")
	private UUID sourceAnalysisId;

	private boolean favorite;

	@Column(name = "created_at")
	private Instant createdAt;

	protected Resume() {
	}

	private Resume(UUID id, String title, ResumeOrigin origin, UUID sourceAnalysisId) {
		this.id = id;
		this.title = title;
		this.origin = origin;
		this.sourceAnalysisId = sourceAnalysisId;
		this.createdAt = Instant.now();
	}

	static Resume base(UUID id, String title) {
		return new Resume(id, title, ResumeOrigin.BASE, null);
	}

	static Resume adapted(UUID id, String title, UUID sourceAnalysisId) {
		return new Resume(id, title, ResumeOrigin.ADAPTED, sourceAnalysisId);
	}

	UUID id() {
		return id;
	}

	String title() {
		return title;
	}

	ResumeOrigin origin() {
		return origin;
	}

	UUID sourceAnalysisId() {
		return sourceAnalysisId;
	}

	boolean favorite() {
		return favorite;
	}

	void markFavorite(boolean favorite) {
		this.favorite = favorite;
	}

	Instant createdAt() {
		return createdAt;
	}

	void rename(String newTitle) {
		this.title = newTitle;
	}

	void scrub() {
		this.title = SCRUBBED_PLACEHOLDER;
	}
}
