package com.diegohaefliger.atsresumeoptimizer.desktop.domain;

public record DatabaseSettings(DatabaseMode mode, String host, Integer port, String database, String username, SslMode sslMode) {

	public static DatabaseSettings embedded() {
		return new DatabaseSettings(DatabaseMode.EMBEDDED, null, null, null, null, null);
	}

	public static DatabaseSettings of(DatabaseMode mode, ExternalDatabase database) {
		return new DatabaseSettings(mode, database.host(), database.port(), database.database(), database.username(),
				database.sslMode());
	}
}
