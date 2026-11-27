package com.diegohaefliger.atsresumeoptimizer.diagnostics.application;

import com.diegohaefliger.atsresumeoptimizer.RequestId;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticEntry;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticSection;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticsContributor;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessCheck;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessContributor;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.ClientError;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.DiagnosticBundle;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.DiagnosticReport;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.LogsFolderUnavailableException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class DiagnosticsServiceImpl implements DiagnosticsService {

	private static final Logger LOG = LoggerFactory.getLogger(DiagnosticsServiceImpl.class);
	private static final Logger FRONT_LOG = LoggerFactory.getLogger("front");
	private static final int LOG_TAIL_LINES = 3000;
	private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
	private static final String NO_ID = "sem-codigo";
	private static final String UNCHECKED_HINT = "Envie o pacote de diagnóstico pelo relato de problema.";

	private final List<DiagnosticsContributor> contributors;
	private final List<ReadinessContributor> readinessContributors;
	private final LogFiles logFiles;
	private final LogSanitizer sanitizer;
	private final BundleWriter bundleWriter;
	private final FolderOpener folderOpener;
	private final Clock clock;

	DiagnosticsServiceImpl(List<DiagnosticsContributor> contributors,
			List<ReadinessContributor> readinessContributors, LogFiles logFiles, LogSanitizer sanitizer,
			BundleWriter bundleWriter, FolderOpener folderOpener, Clock clock) {
		this.contributors = List.copyOf(contributors);
		this.readinessContributors = List.copyOf(readinessContributors);
		this.logFiles = logFiles;
		this.sanitizer = sanitizer;
		this.bundleWriter = bundleWriter;
		this.folderOpener = folderOpener;
		this.clock = clock;
	}

	@Override
	public DiagnosticReport report() {
		List<ReadinessCheck> checks = readinessContributors.stream().flatMap(contributor -> check(contributor).stream()).toList();
		List<DiagnosticSection> sections = contributors.stream().map(DiagnosticsServiceImpl::collect).toList();
		boolean openable = logFiles.folder().isPresent() && folderOpener.available();
		return new DiagnosticReport(clock.instant(), checks, sections, logFiles.appLog().isPresent(), openable);
	}

	@Override
	public DiagnosticBundle bundle() {
		Map<String, String> files = new LinkedHashMap<>();
		files.put("diagnostico.txt", sanitizer.clean(report().asText()));
		logFiles.appLog().ifPresent(log -> files.put("app.log", sanitizedTail(log)));
		logFiles.crashLog().ifPresent(log -> files.put("crash.log", sanitizedTail(log)));
		String stamp = FILE_STAMP.format(clock.instant().atZone(ZoneId.systemDefault()));
		return new DiagnosticBundle("diagnostico-ats-" + stamp + ".zip", bundleWriter.zip(files));
	}

	@Override
	public String recordClientError(ClientError error) {
		FRONT_LOG.error("Erro na tela {}: {}{}{}", error.page(), error.message(), block("Pilha", error.stack()),
				block("Componentes", error.componentStack()));
		return RequestId.current().orElse(NO_ID);
	}

	@Override
	public void openLogsFolder() {
		Path folder = logFiles.folder().filter(Files::isDirectory)
				.orElseThrow(() -> new LogsFolderUnavailableException("Esta instalação não grava o log em arquivo."));
		if (!folderOpener.available()) {
			throw new LogsFolderUnavailableException("Não dá pra abrir pastas neste ambiente. O log fica em " + folder);
		}
		try {
			folderOpener.open(folder);
		} catch (IOException | UnsupportedOperationException exception) {
			LOG.warn("Não deu pra abrir a pasta de logs {}", folder, exception);
			throw new LogsFolderUnavailableException("Não deu pra abrir a pasta. Abra manualmente: " + folder);
		}
	}

	private String sanitizedTail(Path log) {
		try {
			return sanitizer.clean(LogFiles.tail(log, LOG_TAIL_LINES));
		} catch (IOException exception) {
			LOG.warn("Não deu pra ler {}", log, exception);
			return "Não deu pra ler " + log + ": " + exception.getMessage();
		}
	}

	private static List<ReadinessCheck> check(ReadinessContributor contributor) {
		try {
			return contributor.checks();
		} catch (RuntimeException exception) {
			LOG.warn("Verificação de {} falhou", contributor.getClass().getSimpleName(), exception);
			return List.of(ReadinessCheck.failure(contributor.getClass().getSimpleName(),
					"não deu pra verificar: " + exception.getMessage(), UNCHECKED_HINT));
		}
	}

	private static DiagnosticSection collect(DiagnosticsContributor contributor) {
		try {
			return contributor.section();
		} catch (RuntimeException exception) {
			LOG.warn("Diagnóstico de {} falhou", contributor.getClass().getSimpleName(), exception);
			return new DiagnosticSection(contributor.getClass().getSimpleName(),
					List.of(new DiagnosticEntry("Erro ao coletar", String.valueOf(exception.getMessage()))));
		}
	}

	private static String block(String title, String content) {
		return content == null || content.isBlank() ? "" : System.lineSeparator() + title + ":" + System.lineSeparator() + content;
	}
}
