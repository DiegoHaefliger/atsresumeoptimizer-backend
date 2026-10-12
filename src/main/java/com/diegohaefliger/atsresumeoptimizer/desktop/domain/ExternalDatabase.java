package com.diegohaefliger.atsresumeoptimizer.desktop.domain;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record ExternalDatabase(String host, int port, String database, String username, String password, SslMode sslMode) {

	public String jdbcUrl() {
		String parameters = new TreeMap<>(sslMode.jdbcParameters()).entrySet().stream()
				.map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
				.collect(Collectors.joining("&"));
		return "jdbc:postgresql://" + host + ":" + port + "/" + URLEncoder.encode(database, StandardCharsets.UTF_8) + "?" + parameters;
	}

	public String address() {
		return host + ":" + port + "/" + database;
	}
}
