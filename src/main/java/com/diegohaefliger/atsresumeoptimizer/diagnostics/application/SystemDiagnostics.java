package com.diegohaefliger.atsresumeoptimizer.diagnostics.application;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticEntry;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticSection;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticsContributor;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SystemDiagnostics implements DiagnosticsContributor {

	private static final String UNKNOWN_VERSION = "desenvolvimento";
	private static final long BYTES_PER_MB = 1024 * 1024;

	private final ObjectProvider<BuildProperties> buildProperties;
	private final Environment environment;
	private final LogFiles logFiles;

	SystemDiagnostics(ObjectProvider<BuildProperties> buildProperties, Environment environment, LogFiles logFiles) {
		this.buildProperties = buildProperties;
		this.environment = environment;
		this.logFiles = logFiles;
	}

	@Override
	public DiagnosticSection section() {
		return new DiagnosticSection("Sistema", List.of(
				new DiagnosticEntry("Versão do app", Optional.ofNullable(buildProperties.getIfAvailable())
						.map(BuildProperties::getVersion).orElse(UNKNOWN_VERSION)),
				new DiagnosticEntry("Sistema operacional", "%s %s (%s)".formatted(System.getProperty("os.name"),
						System.getProperty("os.version"), System.getProperty("os.arch"))),
				new DiagnosticEntry("Java", "%s (%s)".formatted(System.getProperty("java.version"), System.getProperty("java.vendor"))),
				new DiagnosticEntry("Perfil", environment.getActiveProfiles().length == 0 ? "padrão"
						: String.join(", ", environment.getActiveProfiles())),
				new DiagnosticEntry("Memória máxima", Runtime.getRuntime().maxMemory() / BYTES_PER_MB + " MB"),
				new DiagnosticEntry("Em execução há", uptime()),
				new DiagnosticEntry("Fuso e idioma", ZoneId.systemDefault() + " · " + Locale.getDefault().toLanguageTag()),
				DiagnosticEntry.personal("Arquivo de log", logFiles.appLog().map(Object::toString).orElse("não grava em arquivo"))));
	}

	private static String uptime() {
		Duration uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
		return uptime.toHours() > 0 ? "%dh %02dmin".formatted(uptime.toHours(), uptime.toMinutesPart())
				: "%dmin".formatted(uptime.toMinutes());
	}
}
