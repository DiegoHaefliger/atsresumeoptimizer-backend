package com.diegohaefliger.atsresumeoptimizer.desktop.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalDatabaseTest {

	@Test
	void requiresEncryptionByDefault() {
		assertThat(database(SslMode.REQUIRE).jdbcUrl()).isEqualTo("jdbc:postgresql://vps.exemplo.com:5432/ats?sslmode=require");
	}

	@Test
	void validatesTheCertificateWithTheJavaTrustStore() {
		assertThat(database(SslMode.VERIFY_FULL).jdbcUrl()).isEqualTo(
				"jdbc:postgresql://vps.exemplo.com:5432/ats?sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory&sslmode=verify-full");
	}

	@Test
	void keepsUnusualDatabaseNamesInsideThePath() {
		ExternalDatabase database = new ExternalDatabase("localhost", 5432, "meu banco?x=1", "ats", "segredo", SslMode.DISABLE);

		assertThat(database.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/meu+banco%3Fx%3D1?sslmode=disable");
	}

	private static ExternalDatabase database(SslMode sslMode) {
		return new ExternalDatabase("vps.exemplo.com", 5432, "ats", "ats", "segredo", sslMode);
	}
}
