package com.diegohaefliger.atsresumeoptimizer.rewrite;

public enum ResumeFormat {

	PDF("application/pdf", ".pdf"),
	DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");

	private final String mimeType;
	private final String extension;

	ResumeFormat(String mimeType, String extension) {
		this.mimeType = mimeType;
		this.extension = extension;
	}

	public String mimeType() {
		return mimeType;
	}

	public String extension() {
		return extension;
	}
}
