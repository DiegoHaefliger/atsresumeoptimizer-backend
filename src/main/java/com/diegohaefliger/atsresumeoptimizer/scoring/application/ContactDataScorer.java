package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.application.SensitiveDataDetector;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionScorer;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Finding;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class ContactDataScorer implements DimensionScorer {

	private static final int FIELDS_TRACKED = 2;

	private final SensitiveDataDetector sensitiveDataDetector;

	ContactDataScorer(SensitiveDataDetector sensitiveDataDetector) {
		this.sensitiveDataDetector = sensitiveDataDetector;
	}

	@Override
	public Dimension dimension() {
		return Dimension.CONTACT_DATA;
	}

	@Override
	public DimensionResult score(ScoringContext context) {
		ContactInfo contact = context.parsing().contact();
		int found = (contact.email().isPresent() ? 1 : 0) + (contact.phone().isPresent() ? 1 : 0);
		int score = (int) Math.round(100.0 * found / FIELDS_TRACKED);

		List<Finding> findings = new ArrayList<>();
		if (sensitiveDataDetector.containsSensitiveData(context.parsing().document().rawText())) {
			findings.add(Finding.of(FindingCode.SENSITIVE_DATA_PRESENT,
					"O currículo parece ter CPF, RG ou data de nascimento — esse dado não ajuda no ATS e é sensível.",
					"Remova CPF, RG e data de nascimento do currículo; eles não fazem parte de uma candidatura padrão."));
		}
		return DimensionResult.of(Dimension.CONTACT_DATA, score, findings);
	}
}
