package com.diegohaefliger.atsresumeoptimizer.resume.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "source_format")
	private SourceFormat sourceFormat;

	@Column(name = "no_text_layer")
	private boolean noTextLayer;

	@Column(name = "multi_column")
	private boolean multiColumn;

	@Column(name = "has_table")
	private boolean hasTable;

	@Column(name = "contact_in_header_or_footer")
	private boolean contactInHeaderOrFooter;

	@Column(name = "has_text_box")
	private boolean hasTextBox;

	@Column(name = "has_icon_glyphs_without_unicode")
	private boolean hasIconGlyphsWithoutUnicode;

	@Column(name = "has_hidden_text")
	private boolean hasHiddenText;

	@Column(name = "has_inconsistent_fonts")
	private boolean hasInconsistentFonts;

	@Column(name = "created_at")
	private Instant createdAt;

	protected ResumeVersion() {
	}

	ResumeVersion(
			UUID id,
			UUID resumeId,
			String fileName,
			String mimeType,
			long sizeBytes,
			String sha256,
			NormalizedDocument document,
			ParsingSignals signals) {
		this.id = id;
		this.resumeId = resumeId;
		this.fileName = fileName;
		this.mimeType = mimeType;
		this.sizeBytes = sizeBytes;
		this.sha256 = sha256;
		this.pageCount = document.pageCount();
		this.rawText = document.rawText();
		this.structuredText = document.structuredText();
		this.sourceFormat = document.sourceFormat();
		this.noTextLayer = signals.noTextLayer();
		this.multiColumn = signals.multiColumn();
		this.hasTable = signals.hasTable();
		this.contactInHeaderOrFooter = signals.contactInHeaderOrFooter();
		this.hasTextBox = signals.hasTextBox();
		this.hasIconGlyphsWithoutUnicode = signals.hasIconGlyphsWithoutUnicode();
		this.hasHiddenText = signals.hasHiddenText();
		this.hasInconsistentFonts = signals.hasInconsistentFonts();
		this.createdAt = Instant.now();
	}

	UUID id() {
		return id;
	}

	UUID resumeId() {
		return resumeId;
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

	NormalizedDocument document() {
		return new NormalizedDocument(sourceFormat, rawText, structuredText, pageCount);
	}

	ParsingSignals signals() {
		return new ParsingSignals(noTextLayer, multiColumn, hasTable, contactInHeaderOrFooter, hasTextBox,
				hasIconGlyphsWithoutUnicode, hasHiddenText, hasInconsistentFonts);
	}

	Instant createdAt() {
		return createdAt;
	}

	boolean isScrubbed() {
		return SCRUBBED_PLACEHOLDER.equals(fileName);
	}

	void scrub() {
		this.fileName = SCRUBBED_PLACEHOLDER;
		this.rawText = null;
		this.structuredText = null;
	}
}
