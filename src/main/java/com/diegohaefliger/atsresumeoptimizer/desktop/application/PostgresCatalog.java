package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// SQL nativo de propósito: catálogo do Postgres, COPY e sequências não existem em JPQL.
final class PostgresCatalog {

	static final String LIQUIBASE_CHANGELOG = "databasechangelog";
	static final String LIQUIBASE_LOCK = "databasechangeloglock";

	private PostgresCatalog() {
	}

	static List<String> publicTables(Connection connection) throws SQLException {
		return strings(connection, """
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name""");
	}

	static List<String> applicationTables(Connection connection) throws SQLException {
		List<String> tables = new ArrayList<>(publicTables(connection));
		tables.removeAll(List.of(LIQUIBASE_CHANGELOG, LIQUIBASE_LOCK));
		return tables;
	}

	static List<String> columns(Connection connection, String table) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				SELECT column_name FROM information_schema.columns
				WHERE table_schema = 'public' AND table_name = ? ORDER BY ordinal_position""")) {
			statement.setString(1, table);
			return strings(statement);
		}
	}

	static List<String> parentsFirst(Connection connection, List<String> tables) throws SQLException {
		Map<String, Set<String>> parents = new LinkedHashMap<>();
		tables.forEach(table -> parents.put(table, new LinkedHashSet<>()));
		try (PreparedStatement statement = connection.prepareStatement("""
				SELECT child.relname, parent.relname FROM pg_constraint fk
				JOIN pg_class child ON child.oid = fk.conrelid
				JOIN pg_class parent ON parent.oid = fk.confrelid
				JOIN pg_namespace ns ON ns.oid = child.relnamespace
				WHERE fk.contype = 'f' AND ns.nspname = 'public'""");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				String child = result.getString(1);
				String parent = result.getString(2);
				if (parents.containsKey(child) && !child.equals(parent)) {
					parents.get(child).add(parent);
				}
			}
		}
		List<String> ordered = new ArrayList<>();
		while (ordered.size() < tables.size()) {
			String next = parents.entrySet().stream()
					.filter(entry -> !ordered.contains(entry.getKey()) && ordered.containsAll(entry.getValue()))
					.map(Map.Entry::getKey)
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Chaves estrangeiras em ciclo; não dá pra ordenar a cópia."));
			ordered.add(next);
		}
		return ordered;
	}

	static Map<String, Long> sequences(Connection connection) throws SQLException {
		Map<String, Long> values = new LinkedHashMap<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT sequencename, last_value FROM pg_sequences WHERE schemaname = 'public' AND last_value IS NOT NULL");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				values.put(result.getString(1), result.getLong(2));
			}
		}
		return values;
	}

	static String quote(String identifier) {
		return "\"" + identifier.replace("\"", "\"\"") + "\"";
	}

	private static List<String> strings(Connection connection, String sql) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			return strings(statement);
		}
	}

	private static List<String> strings(PreparedStatement statement) throws SQLException {
		List<String> values = new ArrayList<>();
		try (ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				values.add(result.getString(1));
			}
		}
		return values;
	}
}
