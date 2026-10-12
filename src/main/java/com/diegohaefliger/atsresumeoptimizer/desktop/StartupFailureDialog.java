package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StartupFailureDialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(StartupFailureDialog.class);

	private StartupFailureDialog() {
	}

	static void show(Throwable failure, Path logFile) {
		LOGGER.error("O app não conseguiu iniciar", failure);
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		Throwable rootCause = failure;
		while (rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}
		String message = "Não deu pra iniciar o " + DesktopBrand.NAME + ".\n\n" + rootCause.getMessage()
				+ "\n\nOs detalhes estão em:\n" + logFile;
		JOptionPane.showMessageDialog(null, message, DesktopBrand.NAME, JOptionPane.ERROR_MESSAGE);
	}
}
