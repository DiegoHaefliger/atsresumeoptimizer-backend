package com.diegohaefliger.atsresumeoptimizer.diagnostics.application;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class FolderOpener {

	boolean available() {
		return !GraphicsEnvironment.isHeadless();
	}

	void open(Path folder) throws IOException {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
			Desktop.getDesktop().open(folder.toFile());
			return;
		}
		new ProcessBuilder(fallbackCommand(folder)).start();
	}

	private static List<String> fallbackCommand(Path folder) {
		String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (os.contains("win")) {
			return List.of("explorer", folder.toString());
		}
		if (os.contains("mac")) {
			return List.of("open", folder.toString());
		}
		return List.of("xdg-open", folder.toString());
	}
}
