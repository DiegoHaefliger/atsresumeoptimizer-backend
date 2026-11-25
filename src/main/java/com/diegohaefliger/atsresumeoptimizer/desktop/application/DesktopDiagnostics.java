package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseMode;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.DatabaseStatus;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticEntry;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticSection;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticsContributor;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
@Order(10)
class DesktopDiagnostics implements DiagnosticsContributor {

	private final DesktopProperties properties;
	private final DatabaseStatus database;

	DesktopDiagnostics(DesktopProperties properties, DatabaseStatus database) {
		this.properties = properties;
		this.database = database;
	}

	@Override
	public DiagnosticSection section() {
		return new DiagnosticSection("Instalação", List.of(
				DiagnosticEntry.personal("Pasta de dados", properties.dataDir().toAbsolutePath().toString()),
				DiagnosticEntry.personal("Banco de dados", databaseDescription())));
	}

	private String databaseDescription() {
		if (database.mode() == DatabaseMode.EMBEDDED) {
			return "embutido (porta " + properties.postgresPort() + ")";
		}
		return database.external().map(external -> "externo em " + external.address() + " (SSL " + external.sslMode() + ")")
				.orElse("externo");
	}
}
