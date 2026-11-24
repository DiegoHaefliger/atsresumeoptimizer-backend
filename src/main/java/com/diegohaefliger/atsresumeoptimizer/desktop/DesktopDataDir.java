package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.nio.file.Path;
import java.util.Optional;

final class DesktopDataDir {

	private static final String DATA_DIR_ENV = "ATS_DATA_DIR";
	private static final String DEFAULT_FOLDER = ".ats-resume-optimizer";

	private DesktopDataDir() {
	}

	static Path resolve() {
		return Optional.ofNullable(System.getenv(DATA_DIR_ENV))
				.map(Path::of)
				.orElseGet(() -> Path.of(System.getProperty("user.home"), DEFAULT_FOLDER));
	}
}
