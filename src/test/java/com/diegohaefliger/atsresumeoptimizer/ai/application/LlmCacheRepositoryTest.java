package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LlmCacheRepositoryTest {

	private static final EmbeddedPostgres POSTGRES = start();

	@Autowired
	private LlmCacheRepository repository;

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

	@Test
	void keepsTheFirstResponseWhenTheSameKeyIsSavedTwice() {
		int first = repository.saveIfAbsent("abc", "job-structuring", 4, "{\"title\": \"Dev Java\"}");
		int second = repository.saveIfAbsent("abc", "job-structuring", 4, "{\"title\": \"Outro\"}");

		assertThat(first).isEqualTo(1);
		assertThat(second).isZero();
		assertThat(repository.findById("abc")).get().extracting(LlmCacheEntry::response).asString().contains("Dev Java");
	}
}
