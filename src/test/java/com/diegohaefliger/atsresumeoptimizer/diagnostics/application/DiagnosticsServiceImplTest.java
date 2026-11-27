package com.diegohaefliger.atsresumeoptimizer.diagnostics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.RequestId;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticEntry;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticSection;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticsContributor;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessCheck;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessContributor;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessStatus;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.ClientError;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.DiagnosticBundle;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.DiagnosticReport;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.LogsFolderUnavailableException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;

class DiagnosticsServiceImplTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-25T13:30:00Z"), ZoneOffset.UTC);
	private static final DiagnosticsContributor SYSTEM =
			() -> new DiagnosticSection("Sistema", List.of(new DiagnosticEntry("Versão do app", "1.2.0")));

	@TempDir
	Path folder;

	private final FolderOpener opener = mock(FolderOpener.class);

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	private static final ReadinessContributor DATABASE =
			() -> List.of(ReadinessCheck.ok("Banco de dados", "conectado"));
	private static final ReadinessContributor AI = () -> List.of(ReadinessCheck.failure("Conexão com Ollama (local)",
			"não respondeu: Connection refused", "Abra o Ollama."));

	private DiagnosticsServiceImpl service(String logFile, DiagnosticsContributor... contributors) {
		return service(logFile, List.of(), contributors);
	}

	private DiagnosticsServiceImpl service(String logFile, List<ReadinessContributor> readiness,
			DiagnosticsContributor... contributors) {
		return new DiagnosticsServiceImpl(List.of(contributors), readiness, new LogFiles(logFile), new LogSanitizer(),
				new BundleWriter(), opener, CLOCK);
	}

	@Test
	void checksEverythingTheAppNeedsAndKeepsGoingWhenACheckBreaks() {
		ReadinessContributor broken = () -> {
			throw new IllegalStateException("sem bean");
		};

		DiagnosticReport report = service("", List.of(DATABASE, broken, AI), SYSTEM).report();

		assertThat(report.checks()).extracting(ReadinessCheck::status)
				.containsExactly(ReadinessStatus.OK, ReadinessStatus.FAILURE, ReadinessStatus.FAILURE);
		assertThat(report.verdict()).isEqualTo("2 falhas.");
	}

	@Test
	void putsTheChecksAtTheTopOfTheDownloadedReport() throws Exception {
		DiagnosticBundle bundle = service("", List.of(DATABASE, AI), SYSTEM).bundle();

		String text = unzip(bundle.content()).get("diagnostico.txt");
		String expected = String.join(System.lineSeparator(), "## Verificação", "Resultado: 1 falha.",
				"- [OK] Banco de dados: conectado",
				"- [FALHA] Conexão com Ollama (local): não respondeu: Connection refused",
				"  Como resolver: Abra o Ollama.");
		assertThat(text).contains(expected);
		assertThat(text.indexOf("## Verificação")).isLessThan(text.indexOf("## Sistema"));
	}

	@Test
	void oneBrokenContributorDoesNotHideTheOthers() {
		DiagnosticsContributor broken = () -> {
			throw new IllegalStateException("banco fora");
		};

		DiagnosticReport report = service("", SYSTEM, broken).report();

		assertThat(report.sections()).hasSize(2);
		assertThat(report.sections().get(1).entries().getFirst().value()).isEqualTo("banco fora");
		assertThat(report.asText()).contains("## Sistema").contains("- Versão do app: 1.2.0");
		assertThat(report.logFileAvailable()).isFalse();
	}

	@Test
	void bundlesTheReportAndTheSanitizedLogs() throws Exception {
		Path log = folder.resolve("app.log");
		Files.writeString(log, "ERROR [ABCD2345] chave sk-ant-api03-abcdefghijklmnop recusada");
		Files.writeString(folder.resolve("crash.log"), "travou");

		DiagnosticBundle bundle = service(log.toString(), SYSTEM).bundle();

		Map<String, String> files = unzip(bundle.content());
		assertThat(bundle.fileName()).startsWith("diagnostico-ats-").endsWith(".zip");
		assertThat(files).containsOnlyKeys("diagnostico.txt", "app.log", "crash.log");
		assertThat(files.get("app.log")).contains("ABCD2345").contains("sk-ant-***").doesNotContain("abcdefghijklmnop");
		assertThat(files.get("diagnostico.txt")).contains("Versão do app: 1.2.0");
	}

	@Test
	void answersTheRequestIdThatLabelsTheLoggedClientError() {
		MDC.put(RequestId.MDC_KEY, "K7Q3XM2A");

		String id = service("", SYSTEM).recordClientError(new ClientError("x is undefined", "at Foo", null, "/jobs"));

		assertThat(id).isEqualTo("K7Q3XM2A");
	}

	@Test
	void refusesToOpenTheLogsFolderWhenTheAppDoesNotLogToAFile() {
		assertThatThrownBy(() -> service("", SYSTEM).openLogsFolder()).isInstanceOf(LogsFolderUnavailableException.class);
	}

	@Test
	void pointsToTheFolderWhenTheSystemCannotOpenIt() throws Exception {
		when(opener.available()).thenReturn(true);
		doThrow(new IOException("sem gerenciador de arquivos")).when(opener).open(any());

		assertThatThrownBy(() -> service(folder.resolve("app.log").toString(), SYSTEM).openLogsFolder())
				.isInstanceOf(LogsFolderUnavailableException.class)
				.hasMessageContaining(folder.toString());
	}

	private static Map<String, String> unzip(byte[] content) throws IOException {
		Map<String, String> files = new HashMap<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				files.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
			}
		}
		return files;
	}
}
