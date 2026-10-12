package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ExternalDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyIn;
import org.postgresql.copy.CopyOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
class DatabaseCopier {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCopier.class);

	private final DataSource source;
	private final DatabaseConnections connections;
	private final String changeLog;

	DatabaseCopier(DataSource source, DatabaseConnections connections, @Value("${spring.liquibase.change-log}") String changeLog) {
		this.source = source;
		this.connections = connections;
		this.changeLog = changeLog;
	}

	void copyTo(ExternalDatabase target) throws SQLException {
		createSchema(target);
		try (Connection from = source.getConnection(); Connection to = connections.open(target)) {
			from.setAutoCommit(false);
			from.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			from.setReadOnly(true);
			to.setAutoCommit(false);
			try {
				List<String> tables = PostgresCatalog.parentsFirst(from, PostgresCatalog.applicationTables(from));
				truncate(to, tables);
				for (String table : tables) {
					copyTable(from, to, table);
				}
				for (Map.Entry<String, Long> sequence : PostgresCatalog.sequences(from).entrySet()) {
					setSequence(to, sequence.getKey(), sequence.getValue());
				}
				to.commit();
				LOGGER.info("{} tabelas copiadas para {}", tables.size(), target.address());
			} catch (SQLException | RuntimeException failure) {
				to.rollback();
				throw failure;
			} finally {
				from.rollback();
			}
		}
	}

	private void createSchema(ExternalDatabase target) {
		SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setDataSource(new DriverManagerDataSource(target.jdbcUrl(), target.username(), target.password()));
		liquibase.setChangeLog(changeLog);
		liquibase.setResourceLoader(new DefaultResourceLoader());
		try {
			liquibase.afterPropertiesSet();
		} catch (Exception exception) {
			throw new IllegalStateException("Não deu pra criar as tabelas em " + target.address() + ": " + exception.getMessage(), exception);
		}
	}

	private static void truncate(Connection to, List<String> tables) throws SQLException {
		try (Statement statement = to.createStatement()) {
			statement.execute("TRUNCATE " + tables.stream().map(PostgresCatalog::quote).collect(Collectors.joining(", ")));
		}
	}

	private static void copyTable(Connection from, Connection to, String table) throws SQLException {
		String columns = PostgresCatalog.columns(from, table).stream().map(PostgresCatalog::quote).collect(Collectors.joining(", "));
		String target = PostgresCatalog.quote(table) + " (" + columns + ")";
		CopyOut out = from.unwrap(PGConnection.class).getCopyAPI().copyOut("COPY " + target + " TO STDOUT");
		CopyIn in = to.unwrap(PGConnection.class).getCopyAPI().copyIn("COPY " + target + " FROM STDIN");
		byte[] chunk;
		while ((chunk = out.readFromCopy()) != null) {
			in.writeToCopy(chunk, 0, chunk.length);
		}
		long rows = in.endCopy();
		LOGGER.debug("{}: {} linhas", table, rows);
	}

	private static void setSequence(Connection to, String sequence, long value) throws SQLException {
		try (PreparedStatement statement = to.prepareStatement("SELECT setval(?, ?, true)")) {
			statement.setString(1, PostgresCatalog.quote(sequence));
			statement.setLong(2, value);
			statement.execute();
		}
	}
}
