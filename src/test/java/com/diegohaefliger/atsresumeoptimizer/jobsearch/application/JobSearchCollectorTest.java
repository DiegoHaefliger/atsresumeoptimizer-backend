package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchProgress;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;
import com.diegohaefliger.atsresumeoptimizer.job.JobListing;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceMatchService;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteria;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
import com.diegohaefliger.atsresumeoptimizer.preference.SearchSite;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;

@ExtendWith(MockitoExtension.class)
class JobSearchCollectorTest {

	private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");

	@Mock
	private JobSource source;

	@Mock
	private RequestPacer pacer;

	@Mock
	private JobPreferenceMatchService matchService;

	@Mock
	private JobStructuringService jobService;

	@Mock
	private SuggestionRepository repository;

	private final SearchProgressTracker progress = new SearchProgressTracker();

	private JobSearchCollector collector;

	@BeforeEach
	void setUp() {
		var properties = new JobSearchProperties("https://linkedin.test", "agent", Duration.ZERO, Duration.ZERO,
				Duration.ofSeconds(1), "Brazil", "https://gupy.test", "https://programathor.test", "https://infojobs.test", "https://vagas.test", "https://api.remotar.test", "https://remotar.test");
		collector = new JobSearchCollector(List.of(source), pacer, new SuggestionOfferFactory(), matchService, jobService,
				repository, properties, progress, Clock.fixed(NOW, ZoneOffset.UTC));
		lenient().when(source.name()).thenReturn("LINKEDIN");
		lenient().when(source.label()).thenReturn("LinkedIn");
		lenient().when(source.pageSize()).thenReturn(10);
	}

	private static JobSearchCriteria criteria(int maxResults, int minAdherence, List<String> excluded, List<String> locations) {
		return new JobSearchCriteria(List.of("java"), excluded, List.of(), locations, List.of(), List.of(),
				PostedWithin.PAST_WEEK, maxResults, minAdherence, true, "0 0 */12 * * *", List.of(SearchSite.values()));
	}

	private static SourceJob job(String id, String title, String company) {
		return new SourceJob(id, "https://linkedin.test/jobs/view/" + id, title, company, "Curitiba, Brazil",
				LocalDate.of(2026, 10, 23));
	}

	private void foundJobs(SourceJob... jobs) {
		when(source.search(any())).thenReturn(List.of(jobs));
	}

	private void detailFor(String id) {
		when(source.detail(id)).thenReturn(Optional.of(new SourceJobDetail("Descrição da vaga", null, null)));
	}

	private void scoring(Integer score) {
		when(matchService.match(any(JobOffer.class))).thenReturn(Optional.of(new PreferenceMatch(score, List.of())));
	}

	@Test
	void savesAnAdherentJobAsANewSuggestion() {
		foundJobs(job("111", "Dev Java", "Acme"));
		detailFor("111");
		scoring(85);

		var outcome = collector.collect(criteria(10, 70, List.of(), List.of()));

		ArgumentCaptor<SuggestionEntity> saved = ArgumentCaptor.forClass(SuggestionEntity.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(SuggestionStatus.NEW);
		assertThat(saved.getValue().getScore()).isEqualTo(85);
		assertThat(saved.getValue().getExternalId()).isEqualTo("111");
		assertThat(saved.getValue().getFoundAt()).isEqualTo(NOW);
		assertThat(outcome).isEqualTo(new JobSearchCollector.Outcome(1, 1, 0, 0, null));
	}

	@Test
	void reportsProgressAsEachJobIsRead() {
		foundJobs(job("111", "Dev Java", "Acme"), job("222", "Dev Kotlin", "Beta"));
		detailFor("111");
		detailFor("222");
		when(matchService.match(any(JobOffer.class)))
				.thenReturn(Optional.of(new PreferenceMatch(85, List.of())))
				.thenReturn(Optional.of(new PreferenceMatch(10, List.of())));

		collector.collect(criteria(10, 70, List.of(), List.of()));

		assertThat(progress.snapshot()).isEqualTo(new SearchProgress(2, 1, 1));
	}

	@Test
	void remembersAJobBelowTheMinimumWithoutSuggestingIt() {
		foundJobs(job("111", "Dev Java", "Acme"));
		detailFor("111");
		scoring(69);

		var outcome = collector.collect(criteria(10, 70, List.of(), List.of()));

		ArgumentCaptor<SuggestionEntity> saved = ArgumentCaptor.forClass(SuggestionEntity.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(SuggestionStatus.FILTERED);
		assertThat(outcome.belowAdherence()).isEqualTo(1);
		assertThat(outcome.added()).isZero();
	}

	@Test
	void treatsAJobWithoutAScoreAsBelowTheMinimum() {
		foundJobs(job("111", "Dev Java", "Acme"));
		detailFor("111");
		scoring(null);

		var outcome = collector.collect(criteria(10, 0, List.of(), List.of()));

		assertThat(outcome.belowAdherence()).isEqualTo(1);
		assertThat(outcome.added()).isZero();
	}

	@Test
	void skipsAJobAlreadySeenWithoutFetchingItsDetail() {
		when(repository.findExternalIdsBySource("LINKEDIN")).thenReturn(Set.of("111"));
		foundJobs(job("111", "Dev Java", "Acme"));

		var outcome = collector.collect(criteria(10, 70, List.of(), List.of()));

		verify(source, never()).detail(anyString());
		verify(repository, never()).save(any());
		assertThat(outcome.duplicates()).isEqualTo(1);
	}

	@Test
	void skipsTheSameJobRepostedWithAnotherId() {
		foundJobs(job("111", "Dev Java", "Acme"), job("222", "Dev Java", "Acme"));
		detailFor("111");
		scoring(90);

		var outcome = collector.collect(criteria(10, 70, List.of(), List.of()));

		verify(source, never()).detail("222");
		assertThat(outcome.added()).isEqualTo(1);
		assertThat(outcome.duplicates()).isEqualTo(1);
	}

	@Test
	void skipsAJobTheUserAlreadyRegisteredByHand() {
		var registered = new JobOffer(UUID.randomUUID(), 1L, "Dev Java", "Acme",
				"https://www.linkedin.com/jobs/view/dev-java-at-acme-4481055232?trk=x", null, null, null, null, null, List.of(), null,
				null, "texto");
		when(jobService.listJobs()).thenReturn(List.of(new JobListing(registered, NOW)));
		when(source.externalIdOf(registered.sourceUrl())).thenReturn(Optional.of("4481055232"));
		foundJobs(job("4481055232", "Dev Java", "Acme"));

		var outcome = collector.collect(criteria(10, 70, List.of(), List.of()));

		verify(source, never()).detail(anyString());
		assertThat(outcome.duplicates()).isEqualTo(1);
	}

	@Test
	void ignoresAJobWithAnExcludedWordInTheTitle() {
		foundJobs(job("111", "Estágio em Java", "Acme"));

		var outcome = collector.collect(criteria(10, 70, List.of("estagio"), List.of()));

		verify(source, never()).detail(anyString());
		assertThat(outcome.read()).isEqualTo(1);
		assertThat(outcome.added()).isZero();
		assertThat(outcome.duplicates()).isZero();
	}

	@Test
	void stopsAndReportsTheErrorButKeepsWhatWasAlreadySaved() {
		foundJobs(job("111", "Dev Java", "Acme"), job("222", "Dev Kotlin", "Globex"));
		detailFor("111");
		scoring(90);
		when(source.detail("222")).thenThrow(new JobSourceException("O LinkedIn limitou as requisições."));

		var outcome = collector.collect(criteria(10, 70, List.of(), List.of()));

		verify(repository).save(any(SuggestionEntity.class));
		assertThat(outcome.added()).isEqualTo(1);
		assertThat(outcome.error()).contains("limitou");
	}

	@Test
	void fetchesNoMoreDetailsThanTheConfiguredLimit() {
		foundJobs(job("111", "Dev Java", "Acme"), job("222", "Dev Kotlin", "Globex"));
		detailFor("111");
		scoring(90);

		collector.collect(criteria(1, 70, List.of(), List.of()));

		verify(source, never()).detail("222");
	}

	@Test
	void searchesTheWholeCountryWhenNoLocationWasChosen() {
		foundJobs();

		collector.collect(criteria(10, 70, List.of(), List.of()));

		ArgumentCaptor<SourceQuery> query = ArgumentCaptor.forClass(SourceQuery.class);
		verify(source).search(query.capture());
		assertThat(query.getValue().location()).isEqualTo("Brazil");
		assertThat(query.getValue().keyword()).isEqualTo("java");
	}

	@Test
	void ignoresTheLocationsWhenOnlyRemoteJobsAreWanted() {
		foundJobs();
		var remoteOnly = new JobSearchCriteria(List.of("java"), List.of(), List.of(), List.of("Apenas remoto"),
				List.of(WorkModel.REMOTE), List.of(), PostedWithin.PAST_WEEK, 10, 70, true, "0 0 */12 * * *", List.of(SearchSite.values()));

		collector.collect(remoteOnly);

		ArgumentCaptor<SourceQuery> query = ArgumentCaptor.forClass(SourceQuery.class);
		verify(source).search(query.capture());
		assertThat(query.getValue().location()).isEqualTo("Brazil");
	}

	@Test
	void searchesEachChosenLocationSeparately() {
		foundJobs();

		collector.collect(criteria(10, 70, List.of(), List.of("Curitiba", "Londrina")));

		ArgumentCaptor<SourceQuery> query = ArgumentCaptor.forClass(SourceQuery.class);
		verify(source, org.mockito.Mockito.times(2)).search(query.capture());
		assertThat(query.getAllValues()).extracting(SourceQuery::location).containsExactly("Curitiba", "Londrina");
	}

	private JobSearchCollector collectorWith(JobSource... sources) {
		var properties = new JobSearchProperties("https://linkedin.test", "agent", Duration.ZERO, Duration.ZERO,
				Duration.ofSeconds(1), "Brazil", "https://gupy.test", "https://programathor.test", "https://infojobs.test", "https://vagas.test", "https://api.remotar.test", "https://remotar.test");
		return new JobSearchCollector(List.of(sources), pacer, new SuggestionOfferFactory(), matchService, jobService,
				repository, properties, progress, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private JobSource otherSource(String name, SourceJob... jobs) {
		JobSource other = org.mockito.Mockito.mock(JobSource.class);
		when(other.name()).thenReturn(name);
		lenient().when(other.label()).thenReturn(name);
		when(other.pageSize()).thenReturn(Integer.MAX_VALUE);
		when(other.search(any())).thenReturn(List.of(jobs));
		return other;
	}

	@Test
	void collectsFromEverySourceAndKeepsTheSourceNameOnEachSuggestion() {
		foundJobs(job("111", "Dev Java", "Acme"));
		detailFor("111");
		var gupyJob = new SourceJob("999", "https://gupy.test/job/999", "Dev Kotlin", "Beta", "Remoto",
				LocalDate.of(2026, 10, 23), new SourceJobDetail("Descrição", null, null));
		scoring(80);

		var outcome = collectorWith(source, otherSource("GUPY", gupyJob)).collect(criteria(10, 70, List.of(), List.of()));

		ArgumentCaptor<SuggestionEntity> saved = ArgumentCaptor.forClass(SuggestionEntity.class);
		verify(repository, org.mockito.Mockito.times(2)).save(saved.capture());
		assertThat(saved.getAllValues()).extracting(SuggestionEntity::getSource).containsExactlyInAnyOrder("LINKEDIN", "GUPY");
		assertThat(outcome.added()).isEqualTo(2);
	}

	@Test
	void onlySearchesTheSitesSelectedInThePreferences() {
		var gupyJob = new SourceJob("999", "https://gupy.test/job/999", "Dev Kotlin", "Beta", "Remoto",
				LocalDate.of(2026, 10, 23), new SourceJobDetail("Descrição", null, null));
		JobSource gupy = otherSource("GUPY", gupyJob);
		scoring(80);
		var onlyGupy = new JobSearchCriteria(List.of("java"), List.of(), List.of(), List.of(), List.of(), List.of(),
				PostedWithin.PAST_WEEK, 10, 70, true, "0 0 */12 * * *", List.of(SearchSite.GUPY));

		var outcome = collectorWith(source, gupy).collect(onlyGupy);

		verify(source, never()).search(any());
		assertThat(outcome.added()).isEqualTo(1);
	}

	@Test
	void retriesASearchThatFailedOnceBeforeGivingUp() {
		detailFor("111");
		scoring(80);
		when(source.search(any())).thenThrow(new JobSourceException("tempo esgotado"))
				.thenReturn(List.of(job("111", "Dev Java", "Acme")));

		var outcome = collector.collect(criteria(10, 70, List.of(), List.of()));

		assertThat(outcome.added()).isEqualTo(1);
		assertThat(outcome.error()).isNull();
	}

	@Test
	void aQueryThatKeepsFailingDoesNotStopTheNextKeywordOfTheSameSite() {
		detailFor("111");
		scoring(80);
		when(source.search(any())).thenThrow(new JobSourceException("tempo esgotado"))
				.thenThrow(new JobSourceException("tempo esgotado"))
				.thenReturn(List.of(job("111", "Dev Java", "Acme")));
		var twoKeywords = new JobSearchCriteria(List.of("java", "kotlin"), List.of(), List.of(), List.of(), List.of(),
				List.of(), PostedWithin.PAST_WEEK, 10, 70, true, "0 0 */12 * * *", List.of(SearchSite.LINKEDIN));

		var outcome = collector.collect(twoKeywords);

		assertThat(outcome.added()).isEqualTo(1);
		assertThat(outcome.error()).isEqualTo("tempo esgotado");
	}

	@Test
	void aJobWhoseDetailFailsIsSkippedAndTheNextOnesAreStillRead() {
		foundJobs(job("111", "Dev Java", "Acme"), job("222", "Dev Kotlin", "Globex"));
		when(source.detail("111")).thenThrow(new JobSourceException("tempo esgotado"));
		detailFor("222");
		scoring(90);

		var outcome = collector.collect(criteria(10, 70, List.of(), List.of()));

		assertThat(outcome.added()).isEqualTo(1);
		assertThat(outcome.error()).isEqualTo("tempo esgotado");
	}

	@Test
	void truncatesTextsThatWouldNotFitTheColumns() {
		foundJobs(job("111", "T".repeat(600), "E".repeat(300)));
		detailFor("111");
		scoring(80);

		collector.collect(criteria(10, 70, List.of(), List.of()));

		ArgumentCaptor<SuggestionEntity> saved = ArgumentCaptor.forClass(SuggestionEntity.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getTitle()).hasSize(500);
		assertThat(saved.getValue().getCompany()).hasSize(255);
	}

	@Test
	void doesNotFetchTheDetailWhenTheSourceAlreadySentIt() {
		var gupyJob = new SourceJob("999", "https://gupy.test/job/999", "Dev Kotlin", "Beta", "Remoto",
				LocalDate.of(2026, 10, 23), new SourceJobDetail("Descrição", null, null));
		JobSource gupy = otherSource("GUPY", gupyJob);
		scoring(80);

		collectorWith(gupy).collect(criteria(10, 70, List.of(), List.of()));

		verify(gupy, never()).detail(anyString());
	}

	@Test
	void aFailingSourceDoesNotStopTheOthers() {
		foundJobs(job("111", "Dev Java", "Acme"));
		detailFor("111");
		scoring(80);
		JobSource broken = org.mockito.Mockito.mock(JobSource.class);
		when(broken.name()).thenReturn("GUPY");
		when(broken.pageSize()).thenReturn(Integer.MAX_VALUE);
		when(broken.search(any())).thenThrow(new JobSourceException("A Gupy respondeu com erro 500."));

		var outcome = collectorWith(broken, source).collect(criteria(10, 70, List.of(), List.of()));

		assertThat(outcome.added()).isEqualTo(1);
		assertThat(outcome.error()).isEqualTo("A Gupy respondeu com erro 500.");
	}

	@Test
	void anUnexpectedFailureInASourceBecomesAFriendlyErrorAndTheOthersContinue() {
		foundJobs(job("111", "Dev Java", "Acme"));
		detailFor("111");
		scoring(80);
		JobSource broken = org.mockito.Mockito.mock(JobSource.class);
		when(broken.name()).thenReturn("GUPY");
		when(broken.label()).thenReturn("Gupy");
		when(broken.pageSize()).thenReturn(Integer.MAX_VALUE);
		when(broken.search(any())).thenThrow(new IllegalStateException("html mudou"));

		var outcome = collectorWith(broken, source).collect(criteria(10, 70, List.of(), List.of()));

		assertThat(outcome.added()).isEqualTo(1);
		assertThat(outcome.error()).isEqualTo("Não deu pra ler as vagas da Gupy. A busca nas outras fontes continuou.")
				.doesNotContain("html mudou");
	}

	@Test
	void anUnexpectedFailureOnOneJobDoesNotStopTheNextJobsOfTheSameSite() {
		foundJobs(job("111", "Dev Java", "Acme"), job("222", "Dev Kotlin", "Globex"));
		when(source.detail("111")).thenThrow(new IllegalStateException("html mudou"));
		detailFor("222");
		scoring(80);

		var outcome = collector.collect(criteria(10, 70, List.of(), List.of()));

		assertThat(outcome.added()).isEqualTo(1);
		assertThat(outcome.error()).isEqualTo("Não deu pra ler as vagas da LinkedIn. A busca nas outras fontes continuou.");
	}

	@Test
	void anUnexpectedFailureInAQueryDoesNotStopTheNextKeywordOfTheSameSite() {
		var twoKeywords = new JobSearchCriteria(List.of("java", "kotlin"), List.of(), List.of(), List.of(), List.of(),
				List.of(), PostedWithin.PAST_WEEK, 10, 70, true, "0 0 */12 * * *", List.of(SearchSite.values()));
		when(source.search(any())).thenThrow(new IllegalStateException("html mudou"))
				.thenReturn(List.of(job("111", "Dev Kotlin", "Acme")));
		detailFor("111");
		scoring(80);

		var outcome = collector.collect(twoKeywords);

		assertThat(outcome.added()).isEqualTo(1);
	}

	@Test
	void anErrorThrownByASourceDoesNotLoseTheOthers() {
		foundJobs(job("111", "Dev Java", "Acme"));
		detailFor("111");
		scoring(80);
		JobSource broken = org.mockito.Mockito.mock(JobSource.class);
		when(broken.name()).thenReturn("GUPY");
		when(broken.label()).thenReturn("Gupy");
		when(broken.pageSize()).thenReturn(Integer.MAX_VALUE);
		when(broken.search(any())).thenThrow(new StackOverflowError());

		var outcome = collectorWith(broken, source).collect(criteria(10, 70, List.of(), List.of()));

		assertThat(outcome.added()).isEqualTo(1);
		assertThat(outcome.error()).contains("Gupy");
	}

	@Test
	void skipsJobsPublishedBeforeThePostedWithinWindow() {
		var old = new SourceJob("999", "https://gupy.test/job/999", "Dev Kotlin", "Beta", "Remoto",
				LocalDate.of(2026, 10, 1), new SourceJobDetail("Descrição", null, null));

		var outcome = collectorWith(otherSource("GUPY", old)).collect(criteria(10, 70, List.of(), List.of()));

		assertThat(outcome.added()).isZero();
		verify(repository, never()).save(any());
	}

	@Test
	void usesTheDatePublishedInTheDetailWhenTheListHasNone() {
		var undated = new SourceJob("999", "https://p.test/jobs/999", "Dev Kotlin", "Beta", "Remoto", null);
		JobSource programathor = otherSource("PROGRAMATHOR", undated);
		when(programathor.detail("999")).thenReturn(Optional.of(new SourceJobDetail("Descrição", null, null, null, null,
				LocalDate.of(2026, 8, 19))));

		var outcome = collectorWith(programathor).collect(criteria(10, 70, List.of(), List.of()));

		assertThat(outcome.added()).isZero();
		verify(repository, never()).save(any());
	}
}
