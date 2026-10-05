package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.RemovedSkill;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Competência sumida volta, salvo remoção justificada em {@code removedSkills}, com destaque ligado e dentro do teto. */
final class SkillPreservationGuard {

	private static final Logger LOGGER = LoggerFactory.getLogger(SkillPreservationGuard.class);
	private static final Set<String> SKILL_SECTION_TITLES = Set.of(
			"habilidades", "habilidades tecnicas", "competencias", "competencias tecnicas", "skills", "tecnologias",
			"conhecimentos", "conhecimentos tecnicos");
	private static final Pattern ITEM_SEPARATOR = Pattern.compile("[,;|•]");
	private static final Pattern CONJUNCTION = Pattern.compile("\\s+(?:e|and)\\s+");
	private static final Pattern LABELED_LINE = Pattern.compile("^([^:,]{1,40}):\\s*(.*)$");
	private static final Pattern LEADING_MARKER = Pattern.compile("^[\\-*◦▪●‣]+\\s*");
	private static final Pattern TRAILING_DECORATION = Pattern.compile("[\\s:\\-–]+$");
	private static final Pattern TRAILING_LIST_SEPARATOR = Pattern.compile("[\\s,;]+$");
	private static final int MAX_ITEM_LENGTH = 60;
	private static final int MAX_REMOVALS = 3;
	private static final int REMOVAL_RATIO_DIVISOR = 5;
	private static final String RESTORED_LABEL = "Outras";
	private static final String RESTORED_SECTION_TITLE = "Competências Técnicas";
	private static final String RESTORED_SECTION_LABEL = "Competências";

	StructuredResume apply(StructuredResume content, List<Section> originalSections, boolean jobHighlighted) {
		List<SkillGroup> originalGroups = originalGroups(originalSections);
		List<String> originalSkills = originalGroups.stream().flatMap(group -> group.items().stream()).distinct().toList();
		if (originalSkills.isEmpty()) {
			return withRemovedSkills(content, content.sections(), List.of());
		}
		String outputSkills = NormalizedText.of(skillsText(content));
		List<String> missing = originalSkills.stream().filter(skill -> !present(outputSkills, skill)).toList();

		Map<String, RemovedSkill> accepted = acceptedRemovals(missing, content.removedSkills(), jobHighlighted,
				removalCap(originalSkills.size()));
		List<String> restore = missing.stream().filter(skill -> !accepted.containsKey(skill)).toList();
		if (!restore.isEmpty()) {
			LOGGER.warn("Reescrita: {} competências sumiram sem justificativa e foram restauradas", restore.size());
		}
		return withRemovedSkills(content, restored(content.sections(), missingByGroup(originalGroups, restore)),
				List.copyOf(accepted.values()));
	}

	/** "MySQL e MongoDB" no original continua presente se a saída separou em "MySQL, MongoDB". */
	private boolean present(String outputSkills, String skill) {
		return NormalizedText.containsWord(outputSkills, NormalizedText.of(skill))
				|| Arrays.stream(CONJUNCTION.split(skill))
						.allMatch(part -> NormalizedText.containsWord(outputSkills, NormalizedText.of(part)));
	}

	/** Linha sem "Rótulo:" é continuação da anterior (lista quebrou no PDF), não item novo. */
	private List<SkillGroup> originalGroups(List<Section> sections) {
		List<SkillGroup> groups = new ArrayList<>();
		for (Section section : sections) {
			if (!SKILL_SECTION_TITLES.contains(
					TRAILING_DECORATION.matcher(NormalizedText.of(section.title())).replaceFirst(""))) {
				continue;
			}
			String label = null;
			StringBuilder text = null;
			for (String rawLine : section.content().lines().map(String::strip).filter(line -> !line.isEmpty()).toList()) {
				Matcher labeled = LABELED_LINE.matcher(rawLine);
				if (labeled.matches() || text == null) {
					addGroup(groups, label, text);
					label = labeled.matches() ? labeled.group(1).strip() : null;
					text = new StringBuilder(labeled.matches() ? labeled.group(2) : rawLine);
				} else {
					text.append(' ').append(rawLine);
				}
			}
			addGroup(groups, label, text);
		}
		return groups;
	}

	private void addGroup(List<SkillGroup> groups, String label, StringBuilder text) {
		if (text == null) {
			return;
		}
		List<String> items = Arrays.stream(ITEM_SEPARATOR.split(text))
				.map(this::cleanItem)
				.filter(item -> !item.isEmpty() && item.length() <= MAX_ITEM_LENGTH)
				.toList();
		if (!items.isEmpty()) {
			groups.add(new SkillGroup(label, items));
		}
	}

	private List<SkillGroup> missingByGroup(List<SkillGroup> originalGroups, List<String> restore) {
		Map<String, List<String>> byLabel = new LinkedHashMap<>();
		Set<String> pending = new LinkedHashSet<>(restore);
		for (SkillGroup group : originalGroups) {
			for (String item : group.items()) {
				if (pending.remove(item)) {
					byLabel.computeIfAbsent(group.label() == null ? RESTORED_LABEL : group.label(), label -> new ArrayList<>())
							.add(item);
				}
			}
		}
		return byLabel.entrySet().stream().map(entry -> new SkillGroup(entry.getKey(), entry.getValue())).toList();
	}

	private String cleanItem(String raw) {
		String item = LEADING_MARKER.matcher(raw.strip()).replaceFirst("");
		int labelEnd = item.lastIndexOf(':');
		return (labelEnd >= 0 ? item.substring(labelEnd + 1) : item).strip();
	}

	private Map<String, RemovedSkill> acceptedRemovals(List<String> missing, List<RemovedSkill> reported,
			boolean jobHighlighted, int cap) {
		if (!jobHighlighted) {
			return Map.of();
		}
		Map<String, RemovedSkill> justified = justifiedRemovals(missing, reported);
		if (justified.size() > cap) {
			LOGGER.warn("Reescrita: IA removeu {} competências, acima do teto de {}; todas restauradas", justified.size(), cap);
			return Map.of();
		}
		return justified;
	}

	private Map<String, RemovedSkill> justifiedRemovals(List<String> missing, List<RemovedSkill> reported) {
		Map<String, RemovedSkill> accepted = new LinkedHashMap<>();
		for (String skill : missing) {
			String normalizedSkill = NormalizedText.of(skill);
			reported.stream()
					.filter(removed -> NormalizedText.of(removed.skill()).equals(normalizedSkill))
					.findFirst()
					.ifPresent(removed -> accepted.put(skill, new RemovedSkill(skill, removed.reason())));
		}
		return accepted;
	}

	private int removalCap(int skillCount) {
		return Math.min(MAX_REMOVALS, Math.max(1, skillCount / REMOVAL_RATIO_DIVISOR));
	}

	private List<ResumeSection> restored(List<ResumeSection> sections, List<SkillGroup> missing) {
		if (missing.isEmpty()) {
			return sections;
		}
		Optional<ResumeSection> target = sections.stream()
				.filter(section -> section.semanticType() == ResumeSectionSemanticType.SKILLS)
				.filter(section -> section.kind() != ResumeSectionKind.ENTRIES)
				.findFirst();
		if (target.isEmpty()) {
			List<ResumeSection> withNew = new ArrayList<>(sections);
			withNew.add(new ResumeSection(RESTORED_SECTION_TITLE, ResumeSectionSemanticType.SKILLS, ResumeSectionKind.KEY_VALUE,
					null, missing.stream().map(group -> new KeyValueLine(sectionLabel(group), joined(group))).toList(), null,
					null));
			return withNew;
		}
		ResumeSection skillsSection = target.get();
		return sections.stream().map(section -> section == skillsSection ? appendTo(section, missing) : section).toList();
	}

	private ResumeSection appendTo(ResumeSection section, List<SkillGroup> missing) {
		return switch (section.kind()) {
			case PARAGRAPH -> {
				String joined = missing.stream().map(SkillPreservationGuard::joined).collect(Collectors.joining(", "));
				yield copy(section, section.paragraph() == null || section.paragraph().isBlank()
						? joined
						: section.paragraph().strip() + ", " + joined, section.keyValues(), section.richLines());
			}
			case RICH_LINES -> {
				List<List<TextSpan>> lines = new ArrayList<>(section.richLines());
				missing.forEach(group -> lines.add(
						List.of(new TextSpan(group.label() + ": ", true), new TextSpan(joined(group), false))));
				yield copy(section, section.paragraph(), section.keyValues(), lines);
			}
			case KEY_VALUE, ENTRIES -> copy(section, section.paragraph(), mergedInto(section.keyValues(), missing),
					section.richLines());
		};
	}

	private List<KeyValueLine> mergedInto(List<KeyValueLine> lines, List<SkillGroup> missing) {
		List<KeyValueLine> merged = new ArrayList<>(lines);
		for (SkillGroup group : missing) {
			String label = NormalizedText.of(group.label());
			int index = indexOfLabel(merged, label);
			if (index < 0) {
				merged.add(new KeyValueLine(group.label(), joined(group)));
			} else {
				KeyValueLine line = merged.get(index);
				String value = TRAILING_LIST_SEPARATOR.matcher(line.value() == null ? "" : line.value()).replaceFirst("");
				merged.set(index, new KeyValueLine(line.label(), value.isEmpty() ? joined(group) : value + ", " + joined(group)));
			}
		}
		return merged;
	}

	private int indexOfLabel(List<KeyValueLine> lines, String normalizedLabel) {
		for (int i = 0; i < lines.size(); i++) {
			if (NormalizedText.of(lines.get(i).label()).equals(normalizedLabel)) {
				return i;
			}
		}
		return -1;
	}

	private static String joined(SkillGroup group) {
		return String.join(", ", group.items());
	}

	private static String sectionLabel(SkillGroup group) {
		return RESTORED_LABEL.equals(group.label()) ? RESTORED_SECTION_LABEL : group.label();
	}

	private ResumeSection copy(ResumeSection section, String paragraph, List<KeyValueLine> keyValues,
			List<List<TextSpan>> richLines) {
		return section.withContent(paragraph, keyValues, richLines);
	}

	private String skillsText(StructuredResume content) {
		return content.sections().stream()
				.filter(section -> section.semanticType() == ResumeSectionSemanticType.SKILLS)
				.map(section -> String.join(" ; ",
						nullToEmpty(section.paragraph()),
						section.keyValues().stream().map(line -> line.label() + ": " + line.value()).collect(Collectors.joining(" ; ")),
						section.richLines().stream()
								.map(TextSpan::plainText)
								.collect(Collectors.joining(" ; "))))
				.collect(Collectors.joining(" ; "));
	}

	private StructuredResume withRemovedSkills(StructuredResume content, List<ResumeSection> sections,
			List<RemovedSkill> removedSkills) {
		return new StructuredResume(content.name(), content.headline(), sections, removedSkills);
	}

	private static String nullToEmpty(String text) {
		return text == null ? "" : text;
	}
}
