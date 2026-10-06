package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.domain.InvalidPromptTemplateException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;

class PromptTemplateServiceImplTest {

	private final PromptTemplateRepository repository = Mockito.mock(PromptTemplateRepository.class);
	private final PromptTemplateServiceImpl service =
			new PromptTemplateServiceImpl(repository, Mappers.getMapper(PromptTemplateMapper.class));

	private void existing(String content) {
		when(repository.findFirstByKeyOrderByVersionDesc("resume-structuring"))
				.thenReturn(Optional.of(new PromptTemplate(UUID.randomUUID(), "resume-structuring", 3, content, "gpt-4o")));
		when(repository.save(any(PromptTemplate.class))).thenAnswer(call -> call.getArgument(0));
	}

	@Test
	void editingCreatesTheNextVersionInsteadOfOverwriting() {
		existing("Texto {{resumeText}}");

		PromptTemplateView saved =
				service.publish("resume-structuring", new PromptTemplateUpdate("Novo {{resumeText}}"));

		assertThat(saved.version()).isEqualTo(4);
		assertThat(saved.content()).isEqualTo("Novo {{resumeText}}");
	}

	@Test
	void savingTheSameContentDoesNotCreateAVersion() {
		existing("Texto {{resumeText}}");

		PromptTemplateView saved =
				service.publish("resume-structuring", new PromptTemplateUpdate("Texto {{resumeText}}"));

		assertThat(saved.version()).isEqualTo(3);
		verify(repository, never()).save(any());
	}

	@Test
	void rejectsRemovingAPlaceholderTheSystemFills() {
		existing("Texto {{resumeText}} {{jobFocus}}");

		assertThatThrownBy(() -> service.publish("resume-structuring", new PromptTemplateUpdate("Sem marcador")))
				.isInstanceOf(InvalidPromptTemplateException.class)
				.hasMessageContaining("{{resumeText}}")
				.hasMessageContaining("{{jobFocus}}");
	}

	@Test
	void registersANewInstructionAsVersionOne() {
		when(repository.findFirstByKeyOrderByVersionDesc("nova-instrucao")).thenReturn(Optional.empty());
		when(repository.save(any(PromptTemplate.class))).thenAnswer(call -> call.getArgument(0));

		PromptTemplateView saved = service.publish("nova-instrucao", new PromptTemplateUpdate("Texto"));

		assertThat(saved.version()).isEqualTo(1);
	}

	@Test
	void rejectsAMalformedKey() {
		assertThatThrownBy(() -> service.publish("Chave Inválida", new PromptTemplateUpdate("Texto")))
				.isInstanceOf(InvalidPromptTemplateException.class);
	}
}
