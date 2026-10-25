package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;

@Entity
@Table(name = "job_suggestion")
class SuggestionEntity {

	@Id
	private UUID id;

	private String source;

	@Column(name = "external_id")
	private String externalId;

	private String url;

	private String title;

	private String company;

	private String location;

	@Column(name = "posted_on")
	private LocalDate postedOn;

	private String description;

	@Column(name = "dedup_key")
	private String dedupKey;

	private Integer score;

	@Enumerated(EnumType.STRING)
	private SuggestionStatus status;

	@Column(name = "found_at")
	private Instant foundAt;

	@Column(name = "decided_at")
	private Instant decidedAt;

	@Column(name = "job_posting_id")
	private UUID jobPostingId;

	protected SuggestionEntity() {
	}

	SuggestionEntity(UUID id) {
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public LocalDate getPostedOn() {
		return postedOn;
	}

	public void setPostedOn(LocalDate postedOn) {
		this.postedOn = postedOn;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDedupKey() {
		return dedupKey;
	}

	public void setDedupKey(String dedupKey) {
		this.dedupKey = dedupKey;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}

	public SuggestionStatus getStatus() {
		return status;
	}

	public void setStatus(SuggestionStatus status) {
		this.status = status;
	}

	public Instant getFoundAt() {
		return foundAt;
	}

	public void setFoundAt(Instant foundAt) {
		this.foundAt = foundAt;
	}

	public Instant getDecidedAt() {
		return decidedAt;
	}

	public void setDecidedAt(Instant decidedAt) {
		this.decidedAt = decidedAt;
	}

	public UUID getJobPostingId() {
		return jobPostingId;
	}

	public void setJobPostingId(UUID jobPostingId) {
		this.jobPostingId = jobPostingId;
	}
}
