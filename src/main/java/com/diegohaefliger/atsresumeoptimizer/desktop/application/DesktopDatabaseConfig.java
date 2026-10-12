package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseMode;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseStatus;
import java.io.IOException;
import java.util.Optional;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("desktop")
class DesktopDatabaseConfig {

	@Bean
	ExternalDatabaseStore externalDatabaseStore(DesktopProperties properties) {
		return new ExternalDatabaseStore(properties.dataDir());
	}

	@Bean
	DatabaseStatus runningDatabase(ExternalDatabaseStore store) {
		return store.load()
				.map(external -> new DatabaseStatus(DatabaseMode.EXTERNAL, Optional.of(external)))
				.orElseGet(() -> new DatabaseStatus(DatabaseMode.EMBEDDED, Optional.empty()));
	}

	@Bean
	JdbcConnectionDetails desktopDatabase(DatabaseStatus running, DesktopProperties properties) throws IOException {
		if (running.external().isPresent()) {
			return new ExternalDatabaseConnectionDetails(running.external().get());
		}
		return new EmbeddedPostgresServer(properties);
	}
}
