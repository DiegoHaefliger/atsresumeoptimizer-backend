package com.diegohaefliger.atsresumeoptimizer.desktop.domain;

import java.util.Map;

public enum SslMode {

	REQUIRE(Map.of("sslmode", "require")),
	// O padrão do pgjdbc procura ~/.postgresql/root.crt; a fábrica do Java usa a cadeia de certificados da própria JVM.
	VERIFY_FULL(Map.of("sslmode", "verify-full", "sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory")),
	DISABLE(Map.of("sslmode", "disable"));

	private final Map<String, String> jdbcParameters;

	SslMode(Map<String, String> jdbcParameters) {
		this.jdbcParameters = jdbcParameters;
	}

	public Map<String, String> jdbcParameters() {
		return jdbcParameters;
	}
}
