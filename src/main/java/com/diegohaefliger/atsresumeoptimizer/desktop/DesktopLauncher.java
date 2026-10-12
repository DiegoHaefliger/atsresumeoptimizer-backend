package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

public final class DesktopLauncher {

	private static final Logger LOGGER = LoggerFactory.getLogger(DesktopLauncher.class);
	private static final String PROFILE = "desktop";
	private static final int DEFAULT_PORT = 8080;
	private static final Duration RUNNING_INSTANCE_WAIT = Duration.ofSeconds(90);
	private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

	private final Class<?> application;
	private final Path dataDir;

	public DesktopLauncher(Class<?> application) {
		this.application = application;
		this.dataDir = Path.of(Optional.ofNullable(System.getenv("ATS_DATA_DIR"))
				.orElseGet(() -> Path.of(System.getProperty("user.home"), ".ats-resume-optimizer").toString()));
	}

	public static boolean requested(String[] args) {
		return Stream.concat(Arrays.stream(args).filter(arg -> arg.startsWith("--spring.profiles.active=")),
						Stream.of(System.getProperty("spring.profiles.active"), System.getenv("SPRING_PROFILES_ACTIVE")))
				.filter(value -> value != null)
				.anyMatch(value -> Arrays.asList(value.substring(value.indexOf('=') + 1).split(",")).contains(PROFILE));
	}

	public void launch(String[] args) throws IOException {
		Optional<SingleInstanceLock> lock = acquireOrHandOver();
		if (lock.isEmpty()) {
			return;
		}
		StartupWindow window = StartupWindow.show();
		int port = FreePort.preferring(Optional.ofNullable(System.getenv("ATS_PORT")).map(Integer::parseInt).orElse(DEFAULT_PORT));
		SpringApplication spring = new SpringApplication(application);
		spring.setHeadless(false);
		spring.setAdditionalProfiles(PROFILE);
		spring.addListeners((ApplicationListener<ApplicationReadyEvent>) event -> onReady(event, lock.get(), window),
				(ApplicationListener<ContextClosedEvent>) event -> withdrawPort(lock.get()));
		try {
			spring.run(Stream.concat(Arrays.stream(args),
					Stream.of("--server.port=" + port, "--app.desktop.data-dir=" + dataDir)).toArray(String[]::new));
		} catch (RuntimeException startupFailure) {
			window.close();
			StartupFailureDialog.show(startupFailure, dataDir.resolve("logs").resolve("app.log"));
			System.exit(1);
		}
	}

	private void onReady(ApplicationReadyEvent event, SingleInstanceLock lock, StartupWindow window) {
		ConfigurableApplicationContext context = event.getApplicationContext();
		int port = context.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
		URI url = URI.create("http://localhost:" + port + "/");
		try {
			lock.publishPort(port);
		} catch (IOException exception) {
			LOGGER.warn("Não deu pra gravar a porta do app; abrir de novo não vai achar esta instância.", exception);
		}
		window.close();
		DesktopTray.install(url, () -> System.exit(SpringApplication.exit(context)));
		Browser.open(url);
		LOGGER.info("App pronto em {}", url);
	}

	private Optional<SingleInstanceLock> acquireOrHandOver() throws IOException {
		Instant deadline = Instant.now().plus(RUNNING_INSTANCE_WAIT);
		while (Instant.now().isBefore(deadline)) {
			Optional<SingleInstanceLock> lock = SingleInstanceLock.tryAcquire(dataDir);
			if (lock.isPresent()) {
				return lock;
			}
			Optional<Integer> port = SingleInstanceLock.publishedPort(dataDir);
			if (port.isPresent()) {
				Browser.open(URI.create("http://localhost:" + port.get() + "/"));
				return Optional.empty();
			}
			try {
				Thread.sleep(POLL_INTERVAL);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				return Optional.empty();
			}
		}
		LOGGER.warn("Outra instância do app segurou a pasta de dados por {} sem ficar pronta", RUNNING_INSTANCE_WAIT);
		return Optional.empty();
	}

	private void withdrawPort(SingleInstanceLock lock) {
		try {
			lock.withdrawPort();
		} catch (IOException exception) {
			LOGGER.warn("Não deu pra apagar o arquivo de porta do app", exception);
		}
	}
}
