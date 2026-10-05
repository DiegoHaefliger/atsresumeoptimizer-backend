package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionScorer;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Finding;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class StructureSectionsScorer implements DimensionScorer {

	@Override
	public Dimension dimension() {
		return Dimension.STRUCTURE_SECTIONS;
	}

	@Override
	public DimensionResult score(ScoringContext context) {
		List<Section> sections = context.parsing().sections();
		if (sections.isEmpty()) {
			return DimensionResult.of(Dimension.STRUCTURE_SECTIONS, 0, List.of());
		}

		List<Finding> findings = sections.stream()
				.filter(section -> !section.recognized())
				.map(section -> Finding.of(FindingCode.SECTION_TITLE_NOT_RECOGNIZED,
						"Título de seção \"%s\" não é um título padrão reconhecido.".formatted(section.title()),
						"Use um título padrão, como \"Experiência Profissional\" ou \"Formação Acadêmica\"."))
				.toList();

		long recognizedCount = sections.stream().filter(Section::recognized).count();
		int score = (int) Math.round(100.0 * recognizedCount / sections.size());
		return DimensionResult.of(Dimension.STRUCTURE_SECTIONS, score, findings);
	}
}
