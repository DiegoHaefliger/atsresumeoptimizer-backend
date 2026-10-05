package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SectionSegmenter {

	private static final int MAX_TITLE_LENGTH = 40;

	public List<Section> segment(String text) {
		List<Section> sections = new ArrayList<>();
		String currentTitle = null;
		boolean currentRecognized = false;
		StringBuilder currentContent = new StringBuilder();

		for (String rawLine : text.split("\\R")) {
			String line = rawLine.strip();
			if (line.isEmpty()) {
				continue;
			}
			boolean recognized = SectionTitleDictionary.matches(line);
			boolean repeatsCurrentTitle = line.equalsIgnoreCase(currentTitle) && currentContent.isEmpty();
			if (!repeatsCurrentTitle && (recognized || looksLikeUnrecognizedTitle(line))) {
				if (currentTitle != null) {
					sections.add(new Section(currentTitle, currentRecognized, currentContent.toString().strip()));
				}
				currentTitle = line;
				currentRecognized = recognized;
				currentContent = new StringBuilder();
			} else if (currentTitle != null) {
				currentContent.append(line).append('\n');
			}
		}
		if (currentTitle != null) {
			sections.add(new Section(currentTitle, currentRecognized, currentContent.toString().strip()));
		}
		return sections;
	}

	/** Exige 2+ palavras: sigla ("CI/CD", "AWS") que sobra sozinha numa lista quebrada parece título. */
	private boolean looksLikeUnrecognizedTitle(String line) {
		boolean shortEnough = line.length() <= MAX_TITLE_LENGTH;
		boolean hasLetters = line.chars().anyMatch(Character::isLetter);
		boolean allUpperCase = line.equals(line.toUpperCase(Locale.ROOT)) && !line.equals(line.toLowerCase(Locale.ROOT));
		boolean multiWord = line.strip().contains(" ");
		return shortEnough && hasLetters && allUpperCase && multiWord;
	}
}
