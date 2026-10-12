package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Browser {

	private static final Logger LOGGER = LoggerFactory.getLogger(Browser.class);

	private Browser() {
	}

	static void open(URI url) {
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(url);
				return;
			}
			new ProcessBuilder(fallbackCommand(url)).start();
		} catch (IOException | UnsupportedOperationException exception) {
			LOGGER.warn("Não deu pra abrir o navegador. Acesse {} manualmente.", url, exception);
		}
	}

	private static List<String> fallbackCommand(URI url) {
		String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (os.contains("win")) {
			return List.of("rundll32", "url.dll,FileProtocolHandler", url.toString());
		}
		if (os.contains("mac")) {
			return List.of("open", url.toString());
		}
		return List.of("xdg-open", url.toString());
	}
}
