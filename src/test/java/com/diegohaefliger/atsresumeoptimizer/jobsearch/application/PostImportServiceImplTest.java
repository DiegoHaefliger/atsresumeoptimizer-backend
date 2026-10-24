package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.PostAlreadyImportedException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.PostTextUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceMatchService;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;

@ExtendWith(MockitoExtension.class)
class PostImportServiceImplTest {

	private static final String LINK = "https://www.linkedin.com/posts/fulano_vaga-activity-7251234567890123456-AbCd";
	private static final String TEXT = "Estamos contratando Dev Java Sênior!\nModelo 100% remoto, contrato PJ. "
			+ "Stack: Spring Boot, Kafka e PostgreSQL. Envie o currículo por mensagem.";

	@Mock
	private LinkedInPostReader reader;

	@Mock
	private JobPreferenceMatchService matchService;

	@Mock
	private SuggestionRepository repository;

	private PostImportServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new PostImportServiceImpl(reader, new SuggestionOfferFactory(), matchService, repository,
				new SuggestionEntityMapperImpl(), Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC));
	}

	private ArgumentCaptor<SuggestionEntity> saved() {
		ArgumentCaptor<SuggestionEntity> captor = ArgumentCaptor.forClass(SuggestionEntity.class);
		when(repository.save(captor.capture())).thenAnswer(call -> call.getArgument(0));
		return captor;
	}

	@Test
	void importsThePastedTextAsANewSuggestionWithItsScore() {
		when(matchService.match(any(JobOffer.class))).thenReturn(Optional.of(new PreferenceMatch(80, java.util.List.of())));
		ArgumentCaptor<SuggestionEntity> captor = saved();

		JobSuggestion suggestion = service.importPost(LINK, TEXT);

		assertThat(suggestion.source()).isEqualTo("LINKEDIN_POST");
		assertThat(suggestion.externalId()).isEqualTo("7251234567890123456");
		assertThat(suggestion.title()).isEqualTo("Estamos contratando Dev Java Sênior!");
		assertThat(suggestion.status()).isEqualTo(SuggestionStatus.NEW);
		assertThat(suggestion.score()).isEqualTo(80);
		assertThat(captor.getValue().getDescription()).isEqualTo(TEXT);
		verify(reader, never()).read(any());
	}

	@Test
	void keepsTheSuggestionNewEvenWhenAdherenceIsLow() {
		when(matchService.match(any(JobOffer.class))).thenReturn(Optional.of(new PreferenceMatch(10, java.util.List.of())));
		saved();

		assertThat(service.importPost(LINK, TEXT).status()).isEqualTo(SuggestionStatus.NEW);
	}

	@Test
	void readsTheLinkWhenNoTextIsPasted() {
		when(reader.read(URI.create(LINK))).thenReturn(Optional.of(TEXT));
		when(matchService.match(any(JobOffer.class))).thenReturn(Optional.empty());
		saved();

		JobSuggestion suggestion = service.importPost(LINK, "  ");

		assertThat(suggestion.description()).isEqualTo(TEXT);
		assertThat(suggestion.score()).isNull();
	}

	@Test
	void askForThePastedTextWhenTheLinkCannotBeRead() {
		when(reader.read(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.importPost(LINK, null)).isInstanceOf(PostTextUnavailableException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void rejectsTextTooShortToBeAJob() {
		assertThatThrownBy(() -> service.importPost(LINK, "Vaga Java")).isInstanceOf(PostTextUnavailableException.class);
	}

	@Test
	void rejectsALinkAlreadyImported() {
		when(repository.existsBySourceAndExternalId("LINKEDIN_POST", "7251234567890123456")).thenReturn(true);

		assertThatThrownBy(() -> service.importPost(LINK, TEXT)).isInstanceOf(PostAlreadyImportedException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void rejectsTheSameTextUnderAnotherLink() {
		when(repository.existsByDedupKey(anyString())).thenReturn(true);

		assertThatThrownBy(() -> service.importPost(LINK, TEXT)).isInstanceOf(PostAlreadyImportedException.class);
	}
}
