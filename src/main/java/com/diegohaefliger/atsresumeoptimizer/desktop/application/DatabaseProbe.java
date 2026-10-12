package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ConnectionCheck;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DesktopDatabaseException;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ExternalDatabase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
class DatabaseProbe {

	// Tabelas que as próprias migrations preenchem; com só elas populadas, o banco é o que sobra de uma cópia interrompida.
	private static final List<String> SEEDED_TABLES = List.of("prompt_template", "scoring_profile", "tech_term");

	private final DatabaseConnections connections;

	DatabaseProbe(DatabaseConnections connections) {
		this.connections = connections;
	}

	ConnectionCheck check(ExternalDatabase database) {
		try (Connection connection = connections.open(database); Statement statement = connection.createStatement()) {
			String version = single(statement, "SHOW server_version");
			return new ConnectionCheck(version, isEmpty(statement));
		} catch (SQLException exception) {
			throw DesktopDatabaseException.unreachable(database, exception);
		}
	}

	private static boolean isEmpty(Statement statement) throws SQLException {
		List<String> tables = PostgresCatalog.publicTables(statement.getConnection());
		if (tables.isEmpty()) {
			return true;
		}
		if (!tables.contains(PostgresCatalog.LIQUIBASE_CHANGELOG)) {
			return false;
		}
		for (String table : PostgresCatalog.applicationTables(statement.getConnection())) {
			if (!SEEDED_TABLES.contains(table)
					&& "t".equals(single(statement, "SELECT EXISTS (SELECT 1 FROM " + PostgresCatalog.quote(table) + ")"))) {
				return false;
			}
		}
		return true;
	}

	private static String single(Statement statement, String sql) throws SQLException {
		try (ResultSet result = statement.executeQuery(sql)) {
			result.next();
			return result.getString(1);
		}
	}
}
