package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.SpellingVariant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** A IA aponta a equivalência; o código só aceita quando o trecho citado existe de verdade no currículo. */
@Component
class RequirementEvidenceFinder {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequirementEvidenceFinder.class);
	private static final int MAX_EVIDENCE_LENGTH = 80;

	private final AiPort aiPort;

	RequirementEvidenceFinder(AiPort aiPort) {
		this.aiPort = aiPort;
	}

	Optional<AiResult<List<RequirementEvidence>>> find(String originalText, List<String> missingRequirements) {
		if (missingRequirements.isEmpty()) {
			return Optional.empty();
		}
		AiResult<List<RequirementEvidence>> result;
		try {
			result = aiPort.findRequirementEvidence(originalText, missingRequirements);
		} catch (AiCallException exception) {
			LOGGER.warn("Reescrita: busca de evidência das exigências da vaga falhou, segue sem ela", exception);
			return Optional.empty();
		}
		Map<String, String> requirementsByNormalized = new LinkedHashMap<>();
		missingRequirements.forEach(requirement -> requirementsByNormalized.put(NormalizedText.of(requirement), requirement));
		String normalizedOriginal = NormalizedText.of(originalText);

		Map<String, RequirementEvidence> verified = new LinkedHashMap<>();
		for (RequirementEvidence candidate : result.value()) {
			String requirement = requirementsByNormalized.get(NormalizedText.of(candidate.requirement()));
			String evidence = candidate.evidence() == null ? "" : candidate.evidence().strip();
			boolean literal = !evidence.isEmpty() && evidence.length() <= MAX_EVIDENCE_LENGTH
					&& normalizedOriginal.contains(NormalizedText.of(evidence));
			if (requirement != null && literal && !SpellingVariant.same(requirement, evidence)) {
				verified.putIfAbsent(requirement, new RequirementEvidence(requirement, evidence));
			} else {
				LOGGER.info("Reescrita: evidência descartada por não constar no currículo: {}", candidate);
			}
		}
		return Optional.of(new AiResult<>(List.copyOf(verified.values()), result.usage()));
	}
}
