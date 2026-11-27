package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

class DatabaseInstanceLockTest {

	private static EmbeddedPostgres postgres;
	private static JdbcConnectionDetails database;

	@BeforeAll
	static void start() throws IOException {
		postgres = EmbeddedPostgres.builder().start();
		String url = postgres.getJdbcUrl("postgres", "postgres");
		database = new JdbcConnectionDetails() {
			@Override
			public String getJdbcUrl() {
				return url;
			}

			@Override
			public String getUsername() {
				return "postgres";
			}

			@Override
			public String getPassword() {
				return "postgres";
			}
		};
	}

	@AfterAll
	static void stop() throws IOException {
		postgres.close();
	}

	@Test
	void refusesASecondInstanceAndSaysWhichOneHoldsTheDatabase() {
		try (DatabaseInstanceLock first = DatabaseInstanceLock.acquire(database, "ATS Resume Optimizer 1.1.14 em PC-SALA")) {
			assertThatThrownBy(() -> DatabaseInstanceLock.acquire(database, "ATS Resume Optimizer 1.1.15 em NOTEBOOK"))
					.isInstanceOf(DatabaseInUseException.class)
					.hasMessageContaining("ATS Resume Optimizer 1.1.14 em PC-SALA");
		}
	}

	@Test
	void freesTheDatabaseWhenTheInstanceCloses() {
		DatabaseInstanceLock.acquire(database, "primeira").close();

		try (DatabaseInstanceLock second = DatabaseInstanceLock.acquire(database, "segunda")) {
			assertThat(second).isNotNull();
		}
	}
}
