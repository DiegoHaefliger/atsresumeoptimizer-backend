package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileLookup;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ScoringProfileProviderImplTest {

	@Mock
	private ScoringProfileRepository repository;

	private ScoringProfileProviderImpl provider;

	@Test
	void loadsTheActiveProfileFromTheDatabaseAndParsesTheWeights() {
		provider = new ScoringProfileProviderImpl(repository, new ObjectMapper());
		UUID id = UUID.randomUUID();
		ScoringProfileEntity entity = mock(ScoringProfileEntity.class);
		when(entity.id()).thenReturn(id);
		when(entity.name()).thenReturn("default");
		when(entity.version()).thenReturn(1);
		when(entity.weights()).thenReturn("{\"PARSEABILITY\":0.6,\"LANGUAGE\":0.4}");
		when(repository.findFirstByModeAndActiveTrueOrderByVersionDesc(AnalysisMode.GENERAL)).thenReturn(Optional.of(entity));

		ScoringProfileLookup lookup = provider.activeProfile(AnalysisMode.GENERAL);

		assertThat(lookup.id()).isEqualTo(id);
		assertThat(lookup.profile().weightOf(Dimension.PARSEABILITY)).isEqualTo(0.6);
		assertThat(lookup.profile().weightOf(Dimension.LANGUAGE)).isEqualTo(0.4);
	}

	@Test
	void failsLoudlyWhenNoActiveProfileExistsForTheMode() {
		provider = new ScoringProfileProviderImpl(repository, new ObjectMapper());
		when(repository.findFirstByModeAndActiveTrueOrderByVersionDesc(AnalysisMode.JOB_MATCH)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> provider.activeProfile(AnalysisMode.JOB_MATCH)).isInstanceOf(IllegalStateException.class);
	}
}
