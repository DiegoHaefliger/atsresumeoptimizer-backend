package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ExternalDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
class DatabaseConnections {

	private static final String CONNECT_TIMEOUT_SECONDS = "10";

	Connection open(ExternalDatabase database) throws SQLException {
		Properties properties = new Properties();
		properties.setProperty("user", database.username());
		properties.setProperty("password", database.password());
		properties.setProperty("connectTimeout", CONNECT_TIMEOUT_SECONDS);
		return DriverManager.getConnection(database.jdbcUrl(), properties);
	}
}
