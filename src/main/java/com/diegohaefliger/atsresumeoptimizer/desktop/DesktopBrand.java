package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

final class DesktopBrand {

	static final String NAME = "ATS Resume Optimizer";

	private static final String ICON = "/desktop/icon.png";
	private static final String BUILD_INFO = "/META-INF/build-info.properties";
	private static final String VERSION_KEY = "build.version";

	private DesktopBrand() {
	}

	static Image icon() {
		return Toolkit.getDefaultToolkit().getImage(Objects.requireNonNull(DesktopBrand.class.getResource(ICON)));
	}

	static Optional<String> version() {
		try (InputStream stream = DesktopBrand.class.getResourceAsStream(BUILD_INFO)) {
			if (stream == null) {
				return Optional.empty();
			}
			Properties build = new Properties();
			build.load(stream);
			return Optional.ofNullable(build.getProperty(VERSION_KEY)).filter(value -> !value.isBlank());
		} catch (IOException e) {
			return Optional.empty();
		}
	}
}
