package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfile;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileLookup;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
class ScoringProfileProviderImpl implements ScoringProfileProvider {

	private final ScoringProfileRepository repository;
	private final ObjectMapper objectMapper;

	ScoringProfileProviderImpl(ScoringProfileRepository repository, ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	@Override
	public ScoringProfileLookup activeProfile(AnalysisMode mode) {
		ScoringProfileEntity entity = repository.findFirstByModeAndActiveTrueOrderByVersionDesc(mode)
				.orElseThrow(() -> new IllegalStateException("Nenhum scoring_profile ativo para o modo " + mode));

		ScoringProfile profile = new ScoringProfile(entity.name(), entity.version(), mode, parseWeights(entity.weights()));
		return new ScoringProfileLookup(entity.id(), profile);
	}

	@SuppressWarnings("unchecked")
	private Map<Dimension, Double> parseWeights(String weightsJson) {
		Map<String, Object> raw = objectMapper.readValue(weightsJson, Map.class);
		Map<Dimension, Double> weights = new LinkedHashMap<>();
		raw.forEach((key, value) -> weights.put(Dimension.valueOf(key), ((Number) value).doubleValue()));
		return weights;
	}
}
