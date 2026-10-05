package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.ArrayList;
import java.util.List;

/** O {@code SectionSegmenter} descarta o cabeçalho; sem ele a IA não vê o nome e devolve o placeholder do prompt. */
final class ResumeHeader {

	private static final int MAX_NAME_LENGTH = 60;

	private final List<String> lines;

	private ResumeHeader(List<String> lines) {
		this.lines = List.copyOf(lines);
	}

	static ResumeHeader of(ParsingResult parsingResult) {
		List<Section> sections = parsingResult.sections();
		String firstTitle = sections.isEmpty() ? null : sections.getFirst().title().strip();
		List<String> header = new ArrayList<>();
		for (String rawLine : parsingResult.document().rawText().split("\\R")) {
			String line = rawLine.strip();
			if (line.equals(firstTitle)) {
				break;
			}
			if (!line.isEmpty()) {
				header.add(line);
			}
		}
		return new ResumeHeader(header);
	}

	List<String> lines() {
		return lines;
	}

	String resolveName(String aiName, String originalText) {
		if (aiName != null && !aiName.isBlank() && NormalizedText.of(originalText).contains(NormalizedText.of(aiName))) {
			return aiName;
		}
		return lines.stream().findFirst().filter(this::looksLikeName).orElse("");
	}

	private boolean looksLikeName(String line) {
		return line.length() <= MAX_NAME_LENGTH && !line.contains("@") && line.chars().noneMatch(Character::isDigit);
	}
}
