package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

/** Só o campo correspondente ao {@code kind} vem preenchido. */
public record ResumeSection(
		String title,
		ResumeSectionSemanticType semanticType,
		ResumeSectionKind kind,
		String paragraph,
		List<KeyValueLine> keyValues,
		List<List<TextSpan>> richLines,
		List<ResumeEntry> entries) {

	public ResumeSection {
		semanticType = semanticType == null ? ResumeSectionSemanticType.OTHER : semanticType;
		keyValues = keyValues == null ? List.of() : List.copyOf(keyValues);
		richLines = richLines == null ? List.of() : List.copyOf(richLines);
		entries = entries == null ? List.of() : List.copyOf(entries);
	}

	public ResumeSection withTitle(String newTitle) {
		return new ResumeSection(newTitle, semanticType, kind, paragraph, keyValues, richLines, entries);
	}

	public ResumeSection withContent(String newParagraph, List<KeyValueLine> newKeyValues, List<List<TextSpan>> newRichLines) {
		return new ResumeSection(title, semanticType, kind, newParagraph, newKeyValues, newRichLines, entries);
	}

	public ResumeSection withKeyValues(List<KeyValueLine> newKeyValues) {
		return new ResumeSection(title, semanticType, kind, paragraph, newKeyValues, richLines, entries);
	}

	public ResumeSection withEntries(List<ResumeEntry> newEntries) {
		return new ResumeSection(title, semanticType, kind, paragraph, keyValues, richLines, newEntries);
	}
}
