package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Só restaura bullet em entry que manteve outro do mesmo cargo; cargo inteiro sumido fica pro guard barrar. */
final class DroppedBulletRestorer {

	private static final Logger LOGGER = LoggerFactory.getLogger(DroppedBulletRestorer.class);
	private static final double MIN_COVERAGE = 0.5;

	private final BulletPairing bulletPairing = new BulletPairing();

	StructuredResume restore(StructuredResume content, List<List<String>> originalGroups) {
		List<ResumeEntry> entries = content.sections().stream()
				.filter(section -> section.semanticType().holdsBullets())
				.flatMap(section -> section.entries().stream())
				.toList();
		Map<ResumeEntry, List<String>> restored = new IdentityHashMap<>();
		for (List<String> group : originalGroups) {
			List<String> missing = group.stream().filter(bullet -> entryCovering(bullet, entries).isEmpty()).toList();
			if (missing.isEmpty() || missing.size() == group.size()) {
				continue;
			}
			homeEntry(group, entries)
					.ifPresent(home -> restored.computeIfAbsent(home, entry -> new ArrayList<>()).addAll(missing));
		}
		if (restored.isEmpty()) {
			return content;
		}
		LOGGER.warn("Reescrita: IA descartou {} bullet(s) de experiência, restaurados com o texto original",
				restored.values().stream().mapToInt(List::size).sum());

		return content.withSections(content.sections().stream()
				.map(section -> section.withEntries(
						section.entries().stream().map(entry -> withRestored(entry, restored.get(entry))).toList()))
				.toList());
	}

	private Optional<ResumeEntry> homeEntry(List<String> group, List<ResumeEntry> entries) {
		return group.stream()
				.map(bullet -> entryCovering(bullet, entries))
				.flatMap(Optional::stream)
				.collect(Collectors.groupingBy(entry -> entry, IdentityHashMap::new, Collectors.counting()))
				.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey);
	}

	private Optional<ResumeEntry> entryCovering(String original, List<ResumeEntry> entries) {
		return entries.stream()
				.filter(entry -> bestCoverage(original, entry) >= MIN_COVERAGE)
				.max(Comparator.comparingDouble(entry -> bestCoverage(original, entry)));
	}

	private double bestCoverage(String original, ResumeEntry entry) {
		return entry.bullets().stream()
				.mapToDouble(bullet -> bulletPairing.coverage(original, TextSpan.plainText(bullet)))
				.max()
				.orElse(0);
	}

	private static ResumeEntry withRestored(ResumeEntry entry, List<String> restoredBullets) {
		if (restoredBullets == null) {
			return entry;
		}
		List<List<TextSpan>> bullets = new ArrayList<>(entry.bullets());
		restoredBullets.forEach(bullet -> bullets.add(List.of(new TextSpan(bullet, false))));
		return entry.withBullets(bullets);
	}
}
