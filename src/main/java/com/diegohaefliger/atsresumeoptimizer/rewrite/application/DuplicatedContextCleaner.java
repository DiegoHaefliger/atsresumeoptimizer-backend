package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.List;

/** Modelo pequeno copia o primeiro bullet no contexto; o cargo sai com a mesma frase duas vezes. */
final class DuplicatedContextCleaner {

	private static final double SAME_TEXT_RATIO = 0.8;

	private DuplicatedContextCleaner() {
	}

	static StructuredResume apply(StructuredResume content) {
		return content.withSections(content.sections().stream()
				.map(section -> section.withEntries(section.entries().stream().map(DuplicatedContextCleaner::cleaned).toList()))
				.toList());
	}

	private static ResumeEntry cleaned(ResumeEntry entry) {
		String context = NormalizedText.of(entry.context());
		if (context.isBlank() || entry.bullets().stream().noneMatch(bullet -> overlaps(context, bullet))) {
			return entry;
		}
		return new ResumeEntry(entry.heading(), entry.period(), entry.subheading(), null, entry.bullets(),
				entry.technologies(), entry.results());
	}

	private static boolean overlaps(String context, List<TextSpan> bullet) {
		String text = NormalizedText.of(TextSpan.plainText(bullet));
		if (text.isBlank() || !(text.contains(context) || context.contains(text))) {
			return false;
		}
		return (double) Math.min(text.length(), context.length()) / Math.max(text.length(), context.length()) >= SAME_TEXT_RATIO;
	}
}
