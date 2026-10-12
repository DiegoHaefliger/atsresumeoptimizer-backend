package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ExternalDatabase;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

record ExternalDatabaseConnectionDetails(ExternalDatabase database) implements JdbcConnectionDetails {

	@Override
	public String getJdbcUrl() {
		return database.jdbcUrl();
	}

	@Override
	public String getUsername() {
		return database.username();
	}

	@Override
	public String getPassword() {
		return database.password();
	}
}
