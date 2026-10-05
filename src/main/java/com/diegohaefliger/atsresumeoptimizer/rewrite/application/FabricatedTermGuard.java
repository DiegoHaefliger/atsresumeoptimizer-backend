package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.TechVocabulary;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Item de lista precisa existir no original; em texto corrido, termo novo derruba a frase ou volta o bullet original. */
final class FabricatedTermGuard {

	private static final Logger LOGGER = LoggerFactory.getLogger(FabricatedTermGuard.class);
	private static final Pattern SENTENCE_BREAK = Pattern.compile("(?<=[.!?;])\\s+");
	private static final String LIST_SEPARATOR = ",";

	private final TechVocabulary vocabulary;
	private final String originalText;
	private final Set<String> originalTerms;
	private final List<String> originalBullets;
	private final BulletPairing bulletPairing = new BulletPairing();
	private final Set<String> discarded = new HashSet<>();

	FabricatedTermGuard(TechVocabulary vocabulary, String originalText, List<String> originalBullets,
			List<RequirementEvidence> evidences) {
		this.vocabulary = vocabulary;
		this.originalText = NormalizedText.of(originalText);
		Set<String> terms = new HashSet<>(vocabulary.termsIn(originalText));
		evidences.forEach(evidence -> terms.addAll(vocabulary.termsIn(evidence.requirement())));
		this.originalTerms = Set.copyOf(terms);
		this.originalBullets = List.copyOf(originalBullets);
	}

	StructuredResume apply(StructuredResume content) {
		String headline = fabricated(content.headline()) ? null : content.headline();
		List<ResumeSection> sections = content.sections().stream().map(this::cleanSection).toList();
		if (!discarded.isEmpty()) {
			LOGGER.warn("Reescrita: descartado conteúdo sem lastro no currículo original: {}", discarded);
		}
		return new StructuredResume(content.name(), headline, sections, content.removedSkills());
	}

	private ResumeSection cleanSection(ResumeSection section) {
		String paragraph = withoutFabricatedSentences(section.paragraph());
		List<KeyValueLine> keyValues = section.semanticType() == ResumeSectionSemanticType.SKILLS
				? section.keyValues().stream().map(this::groundedSkillLine).filter(line -> !line.value().isBlank()).toList()
				: section.keyValues().stream().filter(line -> !fabricated(line.label() + " " + line.value())).toList();
		List<List<TextSpan>> richLines = section.richLines().stream().filter(line -> !fabricated(TextSpan.plainText(line))).toList();
		List<ResumeEntry> entries = section.entries().stream().map(this::cleanEntry).toList();
		return section.withContent(paragraph, keyValues, richLines).withEntries(entries);
	}

	private ResumeEntry cleanEntry(ResumeEntry entry) {
		List<List<TextSpan>> bullets = new ArrayList<>();
		for (List<TextSpan> bullet : entry.bullets()) {
			String text = TextSpan.plainText(bullet);
			if (!fabricated(text)) {
				bullets.add(bullet);
				continue;
			}
			bulletPairing.closestOriginal(text, originalBullets)
					.ifPresent(original -> bullets.add(List.of(new TextSpan(original, false))));
		}
		return new ResumeEntry(entry.heading(), entry.period(), entry.subheading(),
				withoutFabricatedSentences(entry.context()), bullets, groundedList(entry.technologies()), entry.results());
	}

	private KeyValueLine groundedSkillLine(KeyValueLine line) {
		return new KeyValueLine(line.label(), groundedList(line.value()));
	}

	private String groundedList(String items) {
		if (items == null || items.isBlank()) {
			return items;
		}
		return Arrays.stream(items.split(LIST_SEPARATOR))
				.map(String::strip)
				.filter(item -> !item.isEmpty())
				.filter(this::grounded)
				.collect(Collectors.joining(LIST_SEPARATOR + " "));
	}

	private boolean grounded(String item) {
		if (NormalizedText.containsWord(originalText, NormalizedText.of(item))) {
			return true;
		}
		Set<String> terms = vocabulary.termsIn(item);
		boolean knownAlias = !terms.isEmpty() && originalTerms.containsAll(terms);
		if (!knownAlias) {
			discarded.add(item);
		}
		return knownAlias;
	}

	private String withoutFabricatedSentences(String text) {
		if (text == null || text.isBlank()) {
			return text;
		}
		return Arrays.stream(SENTENCE_BREAK.split(text.strip()))
				.filter(sentence -> !fabricated(sentence))
				.collect(Collectors.joining(" "));
	}

	private boolean fabricated(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		Set<String> newTerms = new HashSet<>(vocabulary.termsIn(text));
		newTerms.removeAll(originalTerms);
		discarded.addAll(newTerms);
		return !newTerms.isEmpty();
	}
}
