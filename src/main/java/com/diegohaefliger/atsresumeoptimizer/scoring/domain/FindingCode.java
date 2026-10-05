package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

/** {@code blocker} vira alerta bloqueante independente da severidade calculada. */
public enum FindingCode {

	NO_TEXT_LAYER(Dimension.PARSEABILITY, Severity.CRITICAL, true),
	LAYOUT_MULTI_COLUMN(Dimension.PARSEABILITY, Severity.HIGH, false),
	LAYOUT_TABLE(Dimension.PARSEABILITY, Severity.MEDIUM, false),
	CONTACT_IN_HEADER(Dimension.PARSEABILITY, Severity.MEDIUM, false),
	LAYOUT_TEXTBOX(Dimension.PARSEABILITY, Severity.HIGH, false),
	ICON_WITHOUT_UNICODE(Dimension.PARSEABILITY, Severity.LOW, false),
	HIDDEN_TEXT(Dimension.PARSEABILITY, Severity.CRITICAL, true),
	FONT_INCONSISTENT(Dimension.PARSEABILITY, Severity.LOW, false),
	DOCUMENT_LENGTH_MISMATCH(Dimension.PARSEABILITY, Severity.LOW, false),
	SECTION_TITLE_NOT_RECOGNIZED(Dimension.STRUCTURE_SECTIONS, Severity.MEDIUM, false),
	SENSITIVE_DATA_PRESENT(Dimension.CONTACT_DATA, Severity.HIGH, false),
	KEYWORD_REQUIRED_MISSING(Dimension.KEYWORD_MATCH, Severity.CRITICAL, true),
	KEYWORD_STUFFING(Dimension.KEYWORD_MATCH, Severity.MEDIUM, false),
	WEAK_ACTION_VERB(Dimension.CONTENT_QUALITY, Severity.LOW, false),
	TIMELINE_GAP(Dimension.CONTENT_QUALITY, Severity.MEDIUM, false),
	BULLET_LOW_IMPACT(Dimension.CONTENT_QUALITY, Severity.LOW, false),
	REQUIREMENTS_MIN_EXPERIENCE_NOT_MET(Dimension.REQUIREMENTS_SENIORITY, Severity.MEDIUM, false),
	REQUIREMENTS_LANGUAGE_NOT_EVIDENCED(Dimension.REQUIREMENTS_SENIORITY, Severity.LOW, false),
	SPELLING_GRAMMAR_ERROR(Dimension.LANGUAGE, Severity.LOW, false);

	private final Dimension dimension;
	private final Severity defaultSeverity;
	private final boolean blocker;

	FindingCode(Dimension dimension, Severity defaultSeverity, boolean blocker) {
		this.dimension = dimension;
		this.defaultSeverity = defaultSeverity;
		this.blocker = blocker;
	}

	public Dimension dimension() {
		return dimension;
	}

	public Severity defaultSeverity() {
		return defaultSeverity;
	}

	public boolean blocker() {
		return blocker;
	}
}
