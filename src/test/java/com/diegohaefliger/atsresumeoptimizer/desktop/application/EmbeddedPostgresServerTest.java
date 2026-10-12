package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmbeddedPostgresServerTest {

	@TempDir
	Path dataDir;

	@Test
	void keepsDataAcrossRestarts() throws Exception {
		DesktopProperties properties = properties();
		try (EmbeddedPostgresServer server = new EmbeddedPostgresServer(properties)) {
			execute(server, "CREATE TABLE note (text varchar(20))");
			execute(server, "INSERT INTO note VALUES ('persistiu')");
		}

		try (EmbeddedPostgresServer server = new EmbeddedPostgresServer(properties)) {
			assertThat(query(server, "SELECT text FROM note")).isEqualTo("persistiu");
		}
	}

	@Test
	void runsDurableAndOnlyOnLocalhost() throws Exception {
		try (EmbeddedPostgresServer server = new EmbeddedPostgresServer(properties())) {
			assertThat(query(server, "SHOW fsync")).isEqualTo("on");
			assertThat(query(server, "SHOW synchronous_commit")).isEqualTo("on");
			assertThat(query(server, "SHOW listen_addresses")).isEqualTo("localhost");
		}
	}

	@Test
	void refusesConnectionsWithoutThePassword() throws Exception {
		try (EmbeddedPostgresServer server = new EmbeddedPostgresServer(properties())) {
			assertThatThrownBy(() -> DriverManager.getConnection(server.getJdbcUrl(), server.getUsername(), "errada").close())
					.isInstanceOf(SQLException.class)
					.hasMessageContaining("password authentication failed");
		}
	}

	@Test
	void storesThePasswordReadableOnlyByTheOwner() throws Exception {
		try (EmbeddedPostgresServer server = new EmbeddedPostgresServer(properties())) {
			Path passwordFile = dataDir.resolve("postgres-password");

			assertThat(Files.readString(passwordFile)).isEqualTo(server.getPassword());
			assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(passwordFile))).isEqualTo("rw-------");
		}
	}

	@Test
	void explainsWhenTheDatabaseExistsButThePasswordIsGone() throws Exception {
		DesktopProperties properties = properties();
		new EmbeddedPostgresServer(properties).close();
		Files.delete(dataDir.resolve("postgres-password"));

		assertThatThrownBy(() -> new EmbeddedPostgresServer(properties))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Restaure o arquivo do backup");
	}

	private DesktopProperties properties() throws IOException {
		return new DesktopProperties(dataDir, freePort(), Duration.ofSeconds(60));
	}

	static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	private static void execute(EmbeddedPostgresServer server, String sql) throws SQLException {
		try (Connection connection = connect(server); Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

	private static String query(EmbeddedPostgresServer server, String sql) throws SQLException {
		try (Connection connection = connect(server);
				Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(sql)) {
			result.next();
			return result.getString(1);
		}
	}

	private static Connection connect(EmbeddedPostgresServer server) throws SQLException {
		return DriverManager.getConnection(server.getJdbcUrl(), server.getUsername(), server.getPassword());
	}
}
