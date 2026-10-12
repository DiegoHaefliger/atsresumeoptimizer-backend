package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StartupFailureDialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(StartupFailureDialog.class);
	private static final String[] DATABASE_OPTIONS = {"Tentar de novo", "Usar o banco local", "Sair"};
	private static final StartupChoice[] DATABASE_CHOICES = {StartupChoice.RETRY, StartupChoice.USE_EMBEDDED, StartupChoice.EXIT};

	private StartupFailureDialog() {
	}

	static StartupChoice ask(Throwable failure, Path logFile, boolean externalDatabaseDown) {
		LOGGER.error("O app não conseguiu iniciar", failure);
		if (GraphicsEnvironment.isHeadless()) {
			return StartupChoice.EXIT;
		}
		Throwable rootCause = failure;
		while (rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}
		if (externalDatabaseDown) {
			String message = "Não deu pra conectar no banco de dados externo.\n\n" + rootCause.getMessage()
					+ "\n\nVerifique a internet e o servidor, ou volte para o banco local deste computador"
					+ "\n(os dados que estão só no banco externo não aparecem nele).";
			int chosen = JOptionPane.showOptionDialog(null, message, DesktopBrand.NAME, JOptionPane.DEFAULT_OPTION,
					JOptionPane.WARNING_MESSAGE, null, DATABASE_OPTIONS, DATABASE_OPTIONS[0]);
			return chosen < 0 ? StartupChoice.EXIT : DATABASE_CHOICES[chosen];
		}
		String message = "Não deu pra iniciar o " + DesktopBrand.NAME + ".\n\n" + rootCause.getMessage()
				+ "\n\nOs detalhes estão em:\n" + logFile;
		JOptionPane.showMessageDialog(null, message, DesktopBrand.NAME, JOptionPane.ERROR_MESSAGE);
		return StartupChoice.EXIT;
	}
}
