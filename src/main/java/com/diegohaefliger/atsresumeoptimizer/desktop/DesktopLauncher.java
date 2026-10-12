package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

public final class DesktopLauncher {

	private static final Logger LOGGER = LoggerFactory.getLogger(DesktopLauncher.class);
	private static final String PROFILE = "desktop";
	private static final int DEFAULT_PORT = 8080;
	private static final Duration RUNNING_INSTANCE_WAIT = Duration.ofSeconds(90);

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
		Optional<SingleInstanceLock> lock = SingleInstanceLock.tryAcquire(dataDir);
		if (lock.isEmpty()) {
			openRunningInstance();
			return;
		}
		StartupWindow window = StartupWindow.show();
		int port = FreePort.preferring(Optional.ofNullable(System.getenv("ATS_PORT")).map(Integer::parseInt).orElse(DEFAULT_PORT));
		SpringApplication spring = new SpringApplication(application);
		spring.setHeadless(false);
		spring.setAdditionalProfiles(PROFILE);
		spring.addListeners((ApplicationListener<ApplicationReadyEvent>) event -> onReady(event, lock.get(), window));
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
			LOGGER.warn("Não deu pra gravar a porta do app; abrir de novo vai subir outra tela de espera.", exception);
		}
		window.close();
		DesktopTray.install(url, () -> System.exit(SpringApplication.exit(context)));
		Browser.open(url);
		LOGGER.info("App pronto em {}", url);
	}

	private void openRunningInstance() throws IOException {
		Optional<Integer> port = SingleInstanceLock.waitForPublishedPort(dataDir, RUNNING_INSTANCE_WAIT);
		if (port.isPresent()) {
			Browser.open(URI.create("http://localhost:" + port.get() + "/"));
		} else {
			LOGGER.warn("Já existe uma instância do app, mas ela não informou a porta em {}", RUNNING_INSTANCE_WAIT);
		}
	}
}
