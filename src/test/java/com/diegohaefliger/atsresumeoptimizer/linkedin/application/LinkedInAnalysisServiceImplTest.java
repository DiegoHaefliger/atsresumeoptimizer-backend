package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInAnalysisBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInAnalysisDraft;
import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInItemReviewDraft;
import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInSectionReviewDraft;
import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInSuggestionDraft;
import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfileService;
import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfile;
import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfileNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysis;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInEntry;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SnapshotSource;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SuggestionPriority;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkedInAnalysisServiceImplTest {

	@Mock
	private LinkedInAnalysisRepository repository;

	@Mock
	private LinkedInSnapshotService snapshotService;

	@Mock
	private CareerProfileService careerProfileService;

	@Mock
	private AiPort aiPort;

	private final CareerProfile career = new CareerProfile("Engenharia de software", "Dev Pleno", "Pleno", "Tech Lead",
			"Sênior", null, List.of(), List.of(), null, List.of(), List.of("Java"), null, null, null, null);

	private final LinkedInProfile profile = new LinkedInProfile("https://www.linkedin.com/in/ana/", "Ana", "Dev Java", null,
			"Sobre mim", null, null, null, List.of(new LinkedInEntry("Dev", "Acme", "2020", null, "- fiz X")), null,
			null, null, null, List.of("Java"), null, null, null, null);

	private final UUID snapshotId = UUID.randomUUID();
	private LinkedInAnalysisServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new LinkedInAnalysisServiceImpl(repository, new LinkedInAnalysisEntityMapperImpl(),
				new LinkedInAnalysisDraftMapperImpl(), snapshotService, careerProfileService, aiPort,
				Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC));
	}

	private LinkedInSnapshot snapshot(byte[] photo) {
		return new LinkedInSnapshot(snapshotId, Instant.now(), SnapshotSource.EXTENSION, profile, photo,
				photo == null ? null : "image/png", null, null);
	}

	private static LinkedInAnalysisDraft draft() {
		return new LinkedInAnalysisDraft(150, "Perfil razoável.", "Falta posicionamento.", List.of("Reescrever o título"),
				List.of("Java"), List.of("Liderança"), List.of(
						new LinkedInSectionReviewDraft("headline", 55, "Genérico.",
								List.of(new LinkedInSuggestionDraft("high", "Incluir o cargo-alvo")), "Tech Lead Java | AWS", null),
						new LinkedInSectionReviewDraft("HEADLINE", 10, "Duplicada.", null, null, null),
						new LinkedInSectionReviewDraft("INEXISTENTE", 90, "Ignorada.", null, null, null),
						new LinkedInSectionReviewDraft("ABOUT", -5, "Curto.",
								List.of(new LinkedInSuggestionDraft("???", "Abrir com resultado")), null, null),
						new LinkedInSectionReviewDraft("EXPERIENCE", 70, "Bom.", null, null, List.of(
								new LinkedInItemReviewDraft("Porto - Desenvolvedor Sênior", 120, "Sem métricas.",
										List.of(new LinkedInSuggestionDraft("high", "Abrir com o resultado")), "- Reduzi o tempo de resposta"),
								new LinkedInItemReviewDraft("Zup - Desenvolvedor", null, "Claro.", null, null)))));
	}

	private static AiResult<LinkedInAnalysisDraft> result() {
		return new AiResult<>(draft(), new AiUsage("gpt-4o-mini", 1000, 500, new BigDecimal("0.0123"), false));
	}

	@Test
	void analyzesTheLatestSnapshotAndStoresACleanReport() {
		when(snapshotService.latest()).thenReturn(Optional.of(snapshot(new byte[] {1, 2})));
		when(careerProfileService.current()).thenReturn(Optional.of(career));
		when(aiPort.analyzeLinkedIn(any())).thenReturn(result());
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		LinkedInAnalysis analysis = service.analyze(null);

		ArgumentCaptor<LinkedInAnalysisBrief> brief = ArgumentCaptor.forClass(LinkedInAnalysisBrief.class);
		verify(aiPort).analyzeLinkedIn(brief.capture());
		assertThat(brief.getValue().careerProfile()).contains("Próximo cargo desejado (cargo-alvo): Tech Lead");
		assertThat(brief.getValue().linkedInProfile()).contains("Título (headline): Dev Java").contains("Foto de perfil: enviada")
				.contains("Banner: não enviado");
		assertThat(brief.getValue().images()).hasSize(1);
		assertThat(analysis.overallScore()).isEqualTo(100);
		assertThat(analysis.snapshotId()).isEqualTo(snapshotId);
		assertThat(analysis.imagesAnalyzed()).isTrue();
		assertThat(analysis.aiModel()).isEqualTo("gpt-4o-mini");
		assertThat(analysis.costUsd()).isEqualByComparingTo("0.0123");
		assertThat(analysis.report().sections()).hasSize(3);
		assertThat(analysis.report().sections().get(0).section()).isEqualTo(ProfileSection.HEADLINE);
		assertThat(analysis.report().sections().get(0).score()).isEqualTo(55);
		assertThat(analysis.report().sections().get(0).suggestions().get(0).priority()).isEqualTo(SuggestionPriority.HIGH);
		assertThat(analysis.report().sections().get(1).score()).isZero();
		assertThat(analysis.report().sections().get(1).suggestions().get(0).priority()).isEqualTo(SuggestionPriority.MEDIUM);
		var experience = analysis.report().sections().get(2);
		assertThat(experience.items()).extracting("title").containsExactly("Porto - Desenvolvedor Sênior", "Zup - Desenvolvedor");
		assertThat(experience.items().get(0).score()).isEqualTo(100);
		assertThat(experience.items().get(0).suggestions().get(0).priority()).isEqualTo(SuggestionPriority.HIGH);
		assertThat(experience.items().get(1).score()).isZero();
	}

	@Test
	void retriesWithoutImagesWhenTheProviderRejectsThem() {
		when(snapshotService.get(snapshotId)).thenReturn(snapshot(new byte[] {1}));
		when(careerProfileService.current()).thenReturn(Optional.of(career));
		when(aiPort.analyzeLinkedIn(any())).thenThrow(new AiCallException("sem visão", null)).thenReturn(result());
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		LinkedInAnalysis analysis = service.analyze(snapshotId);

		verify(aiPort, times(2)).analyzeLinkedIn(any());
		assertThat(analysis.imagesAnalyzed()).isFalse();
	}

	@Test
	void reportsTheProviderFailureWhenThereWereNoImagesToDrop() {
		when(snapshotService.get(snapshotId)).thenReturn(snapshot(null));
		when(careerProfileService.current()).thenReturn(Optional.of(career));
		when(aiPort.analyzeLinkedIn(any())).thenThrow(new AiCallException("Nenhum provedor de IA configurado.", null));

		assertThatThrownBy(() -> service.analyze(snapshotId))
				.isInstanceOf(LinkedInAnalysisUnavailableException.class)
				.hasMessage("Nenhum provedor de IA configurado.");
		verify(repository, never()).save(any());
	}

	@Test
	void refusesAnEmptyAnswerFromTheModel() {
		when(snapshotService.get(snapshotId)).thenReturn(snapshot(null));
		when(careerProfileService.current()).thenReturn(Optional.of(career));
		when(aiPort.analyzeLinkedIn(any())).thenReturn(
				new AiResult<>(new LinkedInAnalysisDraft(50, "Resumo", null, null, null, null, null), AiUsage.NONE));

		assertThatThrownBy(() -> service.analyze(snapshotId)).isInstanceOf(LinkedInAnalysisUnavailableException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void requiresTheCareerProfile() {
		when(snapshotService.get(snapshotId)).thenReturn(snapshot(null));
		when(careerProfileService.current()).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.analyze(snapshotId)).isInstanceOf(CareerProfileNotFoundException.class);
	}
}
