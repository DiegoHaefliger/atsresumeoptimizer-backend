package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.desktop")
record DesktopProperties(Path dataDir, int postgresPort, Duration postgresStartupTimeout) {
}
