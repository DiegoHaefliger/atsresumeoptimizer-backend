package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionQuery;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionSort;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SuggestionSpecificationTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 27);
	private static final EmbeddedPostgres POSTGRES = start();

	@Autowired
	private SuggestionRepository repository;

	private static EmbeddedPostgres start() {
		try {
			return EmbeddedPostgres.builder().start();
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
		registry.add("spring.datasource.username", () -> "postgres");
		registry.add("spring.datasource.password", () -> "postgres");
	}

	@AfterAll
	static void stop() throws IOException {
		POSTGRES.close();
	}

	@BeforeEach
	void setUp() {
		repository.deleteAll();
		save("java", "GUPY", "Dev Java", "Acme", "São Paulo", 90, TODAY.minusDays(2), SuggestionStatus.NEW);
		save("kotlin", "LINKEDIN", "Desenvolvedor Kotlin", "Globex", "Curitiba", 60, TODAY.minusDays(20),
				SuggestionStatus.NEW);
		save("unscored", "GUPY", "Analista", "Initech", "Remoto", null, null, SuggestionStatus.NEW);
		save("discarded", "VAGAS", "Dev Java Sênior", "Umbrella", "Recife", 95, TODAY, SuggestionStatus.DISCARDED);
	}

	private void save(String externalId, String source, String title, String company, String location, Integer score,
			LocalDate postedOn, SuggestionStatus status) {
		SuggestionEntity entity = new SuggestionEntity(UUID.randomUUID());
		entity.setExternalId(externalId);
		entity.setSource(source);
		entity.setTitle(title);
		entity.setCompany(company);
		entity.setLocation(location);
		entity.setScore(score);
		entity.setPostedOn(postedOn);
		entity.setStatus(status);
		entity.setDedupKey(externalId);
		entity.setDescription("Vaga para " + title);
		entity.setFoundAt(Instant.parse("2026-11-20T10:00:00Z"));
		repository.save(entity);
	}

	private List<String> find(String text, String source, Integer minScore, Integer postedWithinDays, SuggestionSort sort) {
		SuggestionQuery query = new SuggestionQuery(text, source, minScore, postedWithinDays, sort, 0, 30);
		return repository
				.findAll(SuggestionSpecification.matching(SuggestionStatus.NEW, query, TODAY),
						PageRequest.of(0, 30, SuggestionOrdering.of(sort)))
				.map(SuggestionEntity::getExternalId)
				.getContent();
	}

	@Test
	void listsOnlyTheRequestedStatusWithUnscoredJobsLast() {
		assertThat(find(null, null, null, null, SuggestionSort.SCORE_DESC)).containsExactly("java", "kotlin", "unscored");
		assertThat(find(null, null, null, null, SuggestionSort.SCORE_ASC)).containsExactly("kotlin", "java", "unscored");
	}

	@Test
	void sortsByPublicationDateWithUndatedJobsLast() {
		assertThat(find(null, null, null, null, SuggestionSort.POSTED_DESC)).containsExactly("java", "kotlin", "unscored");
		assertThat(find(null, null, null, null, SuggestionSort.POSTED_ASC)).containsExactly("kotlin", "java", "unscored");
	}

	@Test
	void searchesEveryWordIgnoringAccentsAndCase() {
		assertThat(find("sao PAULO", null, null, null, SuggestionSort.SCORE_DESC)).containsExactly("java");
		assertThat(find("desenvolvedor curitiba", null, null, null, SuggestionSort.SCORE_DESC)).containsExactly("kotlin");
		assertThat(find("100%", null, null, null, SuggestionSort.SCORE_DESC)).isEmpty();
	}

	@Test
	void filtersBySourceMinimumScoreAndPublicationDate() {
		assertThat(find(null, "GUPY", null, null, SuggestionSort.SCORE_DESC)).containsExactly("java", "unscored");
		assertThat(find(null, null, 70, null, SuggestionSort.SCORE_DESC)).containsExactly("java");
		assertThat(find(null, null, null, 7, SuggestionSort.SCORE_DESC)).containsExactly("java");
	}

	@Test
	void listsTheDistinctSourcesOfAStatus() {
		assertThat(repository.findDistinctSourcesByStatus(SuggestionStatus.NEW)).containsExactly("GUPY", "LINKEDIN");
	}
}
