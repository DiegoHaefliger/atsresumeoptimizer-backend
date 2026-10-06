package com.diegohaefliger.atsresumeoptimizer.job.application;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job_posting")
class JobPosting {

	private static final String BENEFITS_SEPARATOR = "\n";

	@Id
	private UUID id;

	private String title;

	private String company;

	@Column(name = "source_url")
	private String sourceUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "work_model")
	private WorkModel workModel;

	@Column(name = "interview_url")
	private String interviewUrl;

	private BigDecimal salary;

	private String benefits;

	@Column(name = "custom_title")
	private String customTitle;

	private String seniority;

	@Enumerated(EnumType.STRING)
	@Column(name = "contract_type")
	private ContractType contractType;

	@Column(name = "raw_text")
	private String rawText;

	@Column(name = "text_hash")
	private String textHash;

	@JdbcTypeCode(SqlTypes.JSON)
	private String structured;

	@Column(name = "custom_keywords")
	private String customKeywords;

	@Column(name = "selected_keywords")
	private String selectedKeywords;

	@Column(name = "code", insertable = false, updatable = false)
	private Long code;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "hidden_at")
	private Instant hiddenAt;

	private boolean synthetic;

	protected JobPosting() {
	}

	JobPosting(UUID id, String title, String rawText, String textHash, String structured) {
		this.id = id;
		this.title = title;
		this.rawText = rawText;
		this.textHash = textHash;
		this.structured = structured;
		this.createdAt = Instant.now();
	}

	static JobPosting unstructured(String rawText, String textHash) {
		return new JobPosting(UuidCreator.getTimeOrderedEpoch(), null, rawText, textHash, null);
	}

	void applyDetails(JobDetails details) {
		if (details.company() != null) {
			this.company = details.company();
		}
		if (details.sourceUrl() != null) {
			this.sourceUrl = details.sourceUrl();
		}
		if (details.workModel() != null) {
			this.workModel = details.workModel();
		}
		if (details.interviewUrl() != null) {
			this.interviewUrl = details.interviewUrl();
		}
		if (details.salary() != null) {
			this.salary = details.salary();
		}
		if (!details.benefits().isEmpty()) {
			this.benefits = String.join(BENEFITS_SEPARATOR, details.benefits());
		}
		if (details.title() != null) {
			this.customTitle = details.title();
		}
		if (details.seniority() != null) {
			this.seniority = details.seniority();
		}
		if (details.contractType() != null) {
			this.contractType = details.contractType();
		}
	}

	void replaceDetails(JobDetails details) {
		this.company = details.company();
		this.sourceUrl = details.sourceUrl();
		this.workModel = details.workModel();
		this.interviewUrl = details.interviewUrl();
		this.salary = details.salary();
		this.benefits = details.benefits().isEmpty() ? null : String.join(BENEFITS_SEPARATOR, details.benefits());
		this.customTitle = details.title();
		this.seniority = details.seniority();
		this.contractType = details.contractType();
	}

	void replaceText(String rawText, String textHash) {
		this.rawText = rawText;
		this.textHash = textHash;
		this.title = null;
		this.structured = null;
		this.customKeywords = null;
		this.selectedKeywords = null;
	}

	void replaceKeywords(List<String> keywords, List<String> selected) {
		this.customKeywords = keywords.isEmpty() ? null : String.join(BENEFITS_SEPARATOR, keywords);
		this.selectedKeywords = keywords.isEmpty() ? null : String.join(BENEFITS_SEPARATOR, selected);
	}

	Optional<List<String>> selectedKeywords() {
		return customKeywords == null ? Optional.empty()
				: Optional.of(selectedKeywords == null || selectedKeywords.isEmpty() ? List.of()
						: Arrays.asList(selectedKeywords.split(BENEFITS_SEPARATOR)));
	}

	Optional<List<String>> customKeywords() {
		return Optional.ofNullable(customKeywords).map(joined -> Arrays.asList(joined.split(BENEFITS_SEPARATOR)));
	}

	String textHash() {
		return textHash;
	}

	void structure(String title, String structured) {
		this.title = title;
		this.structured = structured;
	}

	void markSynthetic() {
		this.synthetic = true;
	}

	void hideFromRecent() {
		this.hiddenAt = Instant.now();
	}

	void showInRecent() {
		this.hiddenAt = null;
	}

	boolean isSynthetic() {
		return synthetic;
	}

	boolean isHiddenFromRecent() {
		return hiddenAt != null;
	}

	boolean isStructured() {
		return structured != null;
	}

	UUID id() {
		return id;
	}

	Long code() {
		return code;
	}

	String title() {
		return title;
	}

	String company() {
		return company;
	}

	String sourceUrl() {
		return sourceUrl;
	}

	WorkModel workModel() {
		return workModel;
	}

	String customTitle() {
		return customTitle;
	}

	ContractType contractType() {
		return contractType;
	}

	String seniority() {
		return seniority;
	}

	String interviewUrl() {
		return interviewUrl;
	}

	BigDecimal salary() {
		return salary;
	}

	List<String> benefits() {
		return benefits == null ? List.of() : Arrays.asList(benefits.split(BENEFITS_SEPARATOR));
	}

	String rawText() {
		return rawText;
	}

	Instant createdAt() {
		return createdAt;
	}

	String structured() {
		return structured;
	}
}
