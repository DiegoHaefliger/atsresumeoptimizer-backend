package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

/** Advisory lock do Postgres numa conexão própria: vale também entre computadores que usam o mesmo banco externo. */
final class DatabaseInstanceLock implements AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInstanceLock.class);
	private static final int LOCK_NAMESPACE = 0x41545330;
	private static final int LOCK_ID = 1;
	private static final String CONNECT_TIMEOUT_SECONDS = "10";
	private static final String UNKNOWN_HOLDER = "outra instância do app";
	private static final DateTimeFormatter SINCE = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());
	// Nativas por necessidade: advisory lock e pg_locks não existem em JPQL.
	private static final String TRY_LOCK = "select pg_try_advisory_lock(?, ?)";
	private static final String HOLDER = """
			select a.application_name, a.backend_start from pg_locks l join pg_stat_activity a on a.pid = l.pid
			where l.locktype = 'advisory' and l.classid = cast(? as oid) and l.objid = cast(? as oid) and l.granted
			limit 1""";

	private final Connection connection;

	private DatabaseInstanceLock(Connection connection) {
		this.connection = connection;
	}

	static DatabaseInstanceLock acquire(JdbcConnectionDetails database, String instanceName) {
		Connection connection = open(database, instanceName);
		try {
			if (tryLock(connection)) {
				return new DatabaseInstanceLock(connection);
			}
			String holder = holder(connection);
			connection.close();
			throw new DatabaseInUseException(holder);
		} catch (SQLException exception) {
			closeQuietly(connection);
			throw new IllegalStateException("Não deu pra reservar o banco de dados para esta instância.", exception);
		}
	}

	@Override
	public void close() {
		closeQuietly(connection);
	}

	private static Connection open(JdbcConnectionDetails database, String instanceName) {
		Properties properties = new Properties();
		properties.setProperty("user", database.getUsername());
		properties.setProperty("password", database.getPassword());
		properties.setProperty("connectTimeout", CONNECT_TIMEOUT_SECONDS);
		properties.setProperty("ApplicationName", instanceName);
		try {
			return DriverManager.getConnection(database.getJdbcUrl(), properties);
		} catch (SQLException exception) {
			throw new IllegalStateException("Não deu pra conectar no banco de dados.", exception);
		}
	}

	private static boolean tryLock(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(TRY_LOCK)) {
			statement.setInt(1, LOCK_NAMESPACE);
			statement.setInt(2, LOCK_ID);
			try (ResultSet result = statement.executeQuery()) {
				return result.next() && result.getBoolean(1);
			}
		}
	}

	private static String holder(Connection connection) {
		try (PreparedStatement statement = connection.prepareStatement(HOLDER)) {
			statement.setInt(1, LOCK_NAMESPACE);
			statement.setInt(2, LOCK_ID);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next() || result.getString(1) == null || result.getString(1).isBlank()) {
					return UNKNOWN_HOLDER;
				}
				Timestamp since = result.getTimestamp(2);
				return since == null ? result.getString(1)
						: result.getString(1) + ", aberto desde " + SINCE.format(since.toInstant());
			}
		} catch (SQLException exception) {
			LOGGER.warn("Não deu pra descobrir qual instância está usando o banco", exception);
			return UNKNOWN_HOLDER;
		}
	}

	private static void closeQuietly(Connection connection) {
		try {
			connection.close();
		} catch (SQLException exception) {
			LOGGER.warn("Não deu pra fechar a conexão que reserva o banco", exception);
		}
	}
}
