package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

final class ObjectiveMerger {

	private static final String OBJECTIVE_TITLE_PREFIX = "objetivo";
	private static final String SUMMARY_TITLE = "Resumo Profissional";
	private static final String JOIN = " ";

	private ObjectiveMerger() {
	}

	static StructuredResume mergeIntoSummary(StructuredResume content, List<Section> originalSections) {
		List<ResumeSection> adapted = content.sections().stream().filter(ObjectiveMerger::isObjective).toList();
		List<ResumeSection> remaining = content.sections().stream().filter(section -> !isObjective(section)).toList();
		Optional<String> objective = adaptedText(adapted).or(() -> originalText(originalSections));
		if (objective.isEmpty()) {
			return adapted.isEmpty() ? content : content.withSections(remaining);
		}
		return content.withSections(withObjectiveInSummary(remaining, objective.get()));
	}

	private static boolean isObjective(ResumeSection section) {
		return isObjectiveTitle(section.title());
	}

	private static boolean isObjectiveTitle(String title) {
		return NormalizedText.of(title).startsWith(OBJECTIVE_TITLE_PREFIX);
	}

	private static Optional<String> adaptedText(List<ResumeSection> objectives) {
		return text(objectives.stream().map(ResumeSection::paragraph));
	}

	private static Optional<String> originalText(List<Section> originalSections) {
		return originalSections.stream()
				.filter(section -> isObjectiveTitle(section.title()))
				.map(section -> ResumeTextLine.of(section.content()).stream()
						.map(ResumeTextLine::text)
						.collect(Collectors.joining(JOIN)))
				.filter(StringUtils::hasText)
				.findFirst();
	}

	private static Optional<String> text(Stream<String> paragraphs) {
		String joined = paragraphs.filter(StringUtils::hasText).map(String::strip).collect(Collectors.joining(JOIN));
		return joined.isEmpty() ? Optional.empty() : Optional.of(joined);
	}

	private static List<ResumeSection> withObjectiveInSummary(List<ResumeSection> sections, String objective) {
		for (int index = 0; index < sections.size(); index++) {
			ResumeSection section = sections.get(index);
			if (section.semanticType() == ResumeSectionSemanticType.SUMMARY && section.kind() == ResumeSectionKind.PARAGRAPH
					&& StringUtils.hasText(section.paragraph())) {
				List<ResumeSection> merged = new ArrayList<>(sections);
				merged.set(index, section.withContent(append(section.paragraph(), objective), null, null));
				return merged;
			}
		}
		List<ResumeSection> merged = new ArrayList<>();
		merged.add(new ResumeSection(SUMMARY_TITLE, ResumeSectionSemanticType.SUMMARY, ResumeSectionKind.PARAGRAPH,
				objective, null, null, null));
		merged.addAll(sections);
		return merged;
	}

	private static String append(String summary, String objective) {
		if (NormalizedText.of(summary).contains(NormalizedText.of(objective))) {
			return summary;
		}
		return summary.strip() + JOIN + objective;
	}
}
