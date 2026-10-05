package com.diegohaefliger.atsresumeoptimizer.resume.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume_version")
class ResumeVersion {

	static final String SCRUBBED_PLACEHOLDER = "[removido]";

	@Id
	private UUID id;

	@Column(name = "resume_id")
	private UUID resumeId;

	@Column(name = "storage_key")
	private String storageKey;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "mime_type")
	private String mimeType;

	@Column(name = "size_bytes")
	private long sizeBytes;

	private String sha256;

	@Column(name = "page_count")
	private Integer pageCount;

	@Column(name = "raw_text")
	private String rawText;

	@Column(name = "structured_text")
	private String structuredText;

	@Column(name = "created_at")
	private Instant createdAt;

	protected ResumeVersion() {
	}

	ResumeVersion(
			UUID id,
			UUID resumeId,
			String storageKey,
			String fileName,
			String mimeType,
			long sizeBytes,
			String sha256,
			Integer pageCount,
			String rawText,
			String structuredText) {
		this.id = id;
		this.resumeId = resumeId;
		this.storageKey = storageKey;
		this.fileName = fileName;
		this.mimeType = mimeType;
		this.sizeBytes = sizeBytes;
		this.sha256 = sha256;
		this.pageCount = pageCount;
		this.rawText = rawText;
		this.structuredText = structuredText;
		this.createdAt = Instant.now();
	}

	UUID id() {
		return id;
	}

	UUID resumeId() {
		return resumeId;
	}

	String storageKey() {
		return storageKey;
	}

	String fileName() {
		return fileName;
	}

	String mimeType() {
		return mimeType;
	}

	long sizeBytes() {
		return sizeBytes;
	}

	String sha256() {
		return sha256;
	}

	Integer pageCount() {
		return pageCount;
	}

	String rawText() {
		return rawText;
	}

	String structuredText() {
		return structuredText;
	}

	Instant createdAt() {
		return createdAt;
	}

	boolean isScrubbed() {
		return SCRUBBED_PLACEHOLDER.equals(storageKey);
	}

	void scrub() {
		this.storageKey = SCRUBBED_PLACEHOLDER;
		this.fileName = SCRUBBED_PLACEHOLDER;
		this.rawText = null;
		this.structuredText = null;
	}
}
