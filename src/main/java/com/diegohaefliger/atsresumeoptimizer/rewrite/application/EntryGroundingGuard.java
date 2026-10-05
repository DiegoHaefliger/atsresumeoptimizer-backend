package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.TechVocabulary;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Termo técnico precisa constar no trecho original do próprio cargo, não só em algum lugar do currículo. */
final class EntryGroundingGuard {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntryGroundingGuard.class);
	private static final Pattern SENTENCE_BREAK = Pattern.compile("(?<=[.!?;])\\s+");
	private static final String LIST_SEPARATOR = ",";
	private static final String SUBHEADING_SEPARATOR = "|";
	private static final int NOT_FOUND = -1;

	private final TechVocabulary vocabulary;
	private final String experienceText;
	private final List<String> originalBullets;
	private final List<RequirementEvidence> evidences;
	private final BulletPairing bulletPairing = new BulletPairing();
	private final Set<String> discarded = new HashSet<>();

	EntryGroundingGuard(TechVocabulary vocabulary, String experienceText, List<String> originalBullets,
			List<RequirementEvidence> evidences) {
		this.vocabulary = vocabulary;
		this.experienceText = NormalizedText.of(experienceText);
		this.originalBullets = List.copyOf(originalBullets);
		this.evidences = List.copyOf(evidences);
	}

	StructuredResume apply(StructuredResume content) {
		Map<ResumeEntry, String> blocks = entryBlocks(content);
		List<ResumeSection> sections = content.sections().stream()
				.map(section -> section.withEntries(section.entries().stream()
						.map(entry -> blocks.containsKey(entry) ? grounded(entry, blocks.get(entry)) : entry)
						.toList()))
				.toList();
		if (!discarded.isEmpty()) {
			LOGGER.warn("Reescrita: termos levados pra um cargo onde o original não cita: {}", discarded);
		}
		return content.withSections(sections);
	}

	private Map<ResumeEntry, String> entryBlocks(StructuredResume content) {
		List<ResumeEntry> entries = content.sections().stream()
				.filter(section -> section.semanticType().holdsBullets())
				.flatMap(section -> section.entries().stream())
				.toList();
		Map<ResumeEntry, Integer> anchors = new IdentityHashMap<>();
		int cursor = 0;
		for (ResumeEntry entry : entries) {
			int position = firstFound(cursor, entry.heading(), company(entry.subheading()));
			if (position != NOT_FOUND) {
				anchors.put(entry, position);
				cursor = position + 1;
			}
		}
		Map<ResumeEntry, String> blocks = new IdentityHashMap<>();
		anchors.forEach((entry, start) -> {
			int end = anchors.values().stream().filter(other -> other > start).min(Integer::compare)
					.orElse(experienceText.length());
			blocks.put(entry, experienceText.substring(start, end));
		});
		return blocks;
	}

	private int firstFound(int from, String... candidates) {
		for (String candidate : candidates) {
			String normalized = NormalizedText.of(candidate);
			if (!normalized.isBlank()) {
				int position = experienceText.indexOf(normalized, from);
				if (position != NOT_FOUND) {
					return position;
				}
			}
		}
		return NOT_FOUND;
	}

	private ResumeEntry grounded(ResumeEntry entry, String block) {
		Set<String> blockTerms = new HashSet<>(vocabulary.termsIn(block));
		evidences.stream()
				.filter(evidence -> block.contains(NormalizedText.of(evidence.evidence())))
				.forEach(evidence -> blockTerms.addAll(vocabulary.termsIn(evidence.requirement())));
		List<List<TextSpan>> bullets = new ArrayList<>();
		for (List<TextSpan> bullet : entry.bullets()) {
			String text = TextSpan.plainText(bullet);
			if (!foreign(text, blockTerms)) {
				bullets.add(bullet);
				continue;
			}
			bulletPairing.closestOriginal(text, originalBullets)
					.ifPresent(original -> bullets.add(List.of(new TextSpan(original, false))));
		}
		return new ResumeEntry(entry.heading(), entry.period(), entry.subheading(),
				groundedSentences(entry.context(), blockTerms), bullets,
				groundedTechnologies(entry.technologies(), block, blockTerms), entry.results());
	}

	private boolean foreign(String text, Set<String> blockTerms) {
		if (text == null || text.isBlank()) {
			return false;
		}
		Set<String> terms = new HashSet<>(vocabulary.termsIn(text));
		terms.removeAll(blockTerms);
		discarded.addAll(terms);
		return !terms.isEmpty();
	}

	private String groundedSentences(String text, Set<String> blockTerms) {
		if (text == null || text.isBlank()) {
			return text;
		}
		return Arrays.stream(SENTENCE_BREAK.split(text.strip()))
				.filter(sentence -> !foreign(sentence, blockTerms))
				.collect(Collectors.joining(" "));
	}

	private String groundedTechnologies(String items, String block, Set<String> blockTerms) {
		if (items == null || items.isBlank()) {
			return items;
		}
		return Arrays.stream(items.split(LIST_SEPARATOR))
				.map(String::strip)
				.filter(item -> !item.isEmpty())
				.filter(item -> NormalizedText.containsWord(block, NormalizedText.of(item)) || !foreign(item, blockTerms))
				.collect(Collectors.joining(LIST_SEPARATOR + " "));
	}

	private static String company(String subheading) {
		if (subheading == null) {
			return null;
		}
		int separator = subheading.indexOf(SUBHEADING_SEPARATOR);
		return separator == NOT_FOUND ? subheading : subheading.substring(0, separator);
	}
}
