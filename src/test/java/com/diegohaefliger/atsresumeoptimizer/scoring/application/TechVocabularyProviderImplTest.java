package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.TechVocabulary;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TechVocabularyProviderImplTest {

	@Test
	void groupsAliasesAndJobEquivalentsUnderTheCanonicalTerm() {
		TechTerm kubernetes = mock(TechTerm.class);
		when(kubernetes.canonical()).thenReturn("Kubernetes");
		when(kubernetes.aliases()).thenReturn(List.of("k8s"));
		TechTermRepository repository = mock(TechTermRepository.class);
		when(repository.findAll()).thenReturn(List.of(kubernetes));
		JobStructured job = new JobStructured("Backend", null, null, null, List.of(), List.of("Terraform", "Kubernetes"),
				Map.of("Terraform", List.of("IaC"), "Kubernetes", List.of("Kubernetes Cluster")));

		TechVocabulary vocabulary = new TechVocabularyProviderImpl(repository).forJob(job);

		assertThat(vocabulary.termsIn("Deploy em K8s com IaC.")).containsExactlyInAnyOrder("kubernetes", "terraform");
		assertThat(vocabulary.termsIn("Criação de pipelines.")).isEmpty();
	}
}
