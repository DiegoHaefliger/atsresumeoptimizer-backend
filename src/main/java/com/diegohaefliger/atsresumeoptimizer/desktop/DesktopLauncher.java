package com.diegohaefliger.atsresumeoptimizer.desktop;

import com.diegohaefliger.atsresumeoptimizer.desktop.application.DesktopRestart;
import com.diegohaefliger.atsresumeoptimizer.desktop.application.ExternalDatabaseStore;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.sql.SQLException;
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

final class DesktopLauncher {

	private static final Logger LOGGER = LoggerFactory.getLogger(DesktopLauncher.class);
	private static final String PROFILE = "desktop";
	private static final int DEFAULT_PORT = 8080;
	private static final Duration RUNNING_INSTANCE_WAIT = Duration.ofSeconds(90);
	private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
	private static final Duration RESTART_DELAY = Duration.ofSeconds(1);

	private final Class<?> application;
	private final Path dataDir;
	private final ExternalDatabaseStore databases;

	private String[] args;
	private SingleInstanceLock lock;
	private StartupWindow window;
	private int port;
	private volatile ConfigurableApplicationContext context;
	private boolean started;

	DesktopLauncher(Class<?> application) {
		this.application = application;
		this.dataDir = Path.of(Optional.ofNullable(System.getenv("ATS_DATA_DIR"))
				.orElseGet(() -> Path.of(System.getProperty("user.home"), ".ats-resume-optimizer").toString()));
		this.databases = new ExternalDatabaseStore(dataDir);
	}

	void launch(String[] args) throws IOException {
		Optional<SingleInstanceLock> acquired = acquireOrHandOver();
		if (acquired.isEmpty()) {
			return;
		}
		this.args = args;
		this.lock = acquired.get();
		this.window = StartupWindow.show();
		this.port = Optional.ofNullable(System.getenv("ATS_PORT")).map(Integer::parseInt).orElse(DEFAULT_PORT);
		start();
	}

	private void start() {
		while (true) {
			try {
				port = FreePort.preferring(port);
				context = spring().run(Stream.concat(Arrays.stream(args),
						Stream.of("--server.port=" + port, "--app.desktop.data-dir=" + dataDir)).toArray(String[]::new));
				return;
			} catch (RuntimeException | IOException failure) {
				window.close();
				boolean externalDatabaseDown = databases.load().isPresent() && isDatabaseFailure(failure);
				switch (StartupFailureDialog.ask(failure, dataDir.resolve("logs").resolve("app.log"), externalDatabaseDown)) {
					case RETRY -> LOGGER.info("Tentando iniciar de novo");
					case USE_EMBEDDED -> databases.disable();
					case EXIT -> System.exit(1);
				}
			}
		}
	}

	private SpringApplication spring() {
		SpringApplication spring = new SpringApplication(application);
		spring.setHeadless(false);
		spring.setAdditionalProfiles(PROFILE);
		spring.addInitializers(created -> created.getBeanFactory().registerSingleton("desktopRestart", (DesktopRestart) this::restart));
		spring.addListeners((ApplicationListener<ApplicationReadyEvent>) this::onReady,
				(ApplicationListener<ContextClosedEvent>) event -> withdrawPort());
		return spring;
	}

	private void restart() {
		Thread.ofPlatform().name("desktop-restart").daemon(false).start(() -> {
			try {
				Thread.sleep(RESTART_DELAY);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				return;
			}
			LOGGER.info("Reiniciando o app");
			context.close();
			start();
		});
	}

	private void onReady(ApplicationReadyEvent event) {
		URI url = URI.create("http://localhost:" + port + "/");
		try {
			lock.publishPort(port);
		} catch (IOException exception) {
			LOGGER.warn("Não deu pra gravar a porta do app; abrir de novo não vai achar esta instância.", exception);
		}
		window.close();
		if (!started) {
			started = true;
			DesktopTray.install(url, () -> System.exit(SpringApplication.exit(context)));
			Browser.open(url);
		}
		LOGGER.info("App pronto em {}", url);
	}

	private static boolean isDatabaseFailure(Throwable failure) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException || cause instanceof ConnectException || cause instanceof UnknownHostException) {
				return true;
			}
		}
		return false;
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

	private void withdrawPort() {
		try {
			lock.withdrawPort();
		} catch (IOException exception) {
			LOGGER.warn("Não deu pra apagar o arquivo de porta do app", exception);
		}
	}
}
