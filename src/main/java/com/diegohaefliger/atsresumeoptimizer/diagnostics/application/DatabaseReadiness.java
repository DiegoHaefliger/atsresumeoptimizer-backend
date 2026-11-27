package com.diegohaefliger.atsresumeoptimizer.diagnostics.application;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessCheck;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessContributor;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
class DatabaseReadiness implements ReadinessContributor {

	private static final String NAME = "Banco de dados";
	private static final int VALIDATION_TIMEOUT_SECONDS = 5;
	private static final String HINT = "Confira se o banco está ligado e acessível. No desktop, veja Configurações > Banco de dados.";

	private final DataSource dataSource;

	DatabaseReadiness(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public List<ReadinessCheck> checks() {
		long start = System.nanoTime();
		try (Connection connection = dataSource.getConnection()) {
			if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
				return List.of(ReadinessCheck.failure(NAME, "a conexão abriu mas não respondeu", HINT));
			}
			DatabaseMetaData metadata = connection.getMetaData();
			long millis = Duration.ofNanos(System.nanoTime() - start).toMillis();
			return List.of(ReadinessCheck.ok(NAME, "conectado · %s %s (%d ms)"
					.formatted(metadata.getDatabaseProductName(), metadata.getDatabaseProductVersion(), millis)));
		} catch (SQLException exception) {
			return List.of(ReadinessCheck.failure(NAME, "sem conexão: " + exception.getMessage(), HINT));
		}
	}
}
