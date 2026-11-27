package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StartupFailureDialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(StartupFailureDialog.class);
	private static final String[] DATABASE_OPTIONS = {"Tentar de novo", "Usar o banco local", "Sair"};
	private static final StartupChoice[] DATABASE_CHOICES = {StartupChoice.RETRY, StartupChoice.USE_EMBEDDED, StartupChoice.EXIT};

	private static final String[] IN_USE_EXTERNAL_OPTIONS = {"Tentar de novo", "Usar o banco local", "Sair"};
	private static final StartupChoice[] IN_USE_EXTERNAL_CHOICES = {StartupChoice.RETRY, StartupChoice.USE_EMBEDDED, StartupChoice.EXIT};
	private static final String[] IN_USE_OPTIONS = {"Tentar de novo", "Sair"};
	private static final StartupChoice[] IN_USE_CHOICES = {StartupChoice.RETRY, StartupChoice.EXIT};
	private static final String[] OTHER_VERSION_OPTIONS = {"Tentar de novo", "Abrir a versão que está aberta", "Sair"};
	private static final StartupChoice[] OTHER_VERSION_CHOICES = {StartupChoice.RETRY, StartupChoice.OPEN_RUNNING, StartupChoice.EXIT};
	private static final String EARLIER_VERSION = "uma versão anterior";
	private static final String DEVELOPMENT_VERSION = "de desenvolvimento";

	private StartupFailureDialog() {
	}

	static StartupChoice askDatabaseInUse(String reason, boolean externalDatabase) {
		LOGGER.warn("O app não iniciou: {}", reason);
		if (GraphicsEnvironment.isHeadless()) {
			return StartupChoice.EXIT;
		}
		String message = reason + "\n\nSó uma instância do " + DesktopBrand.NAME + " pode usar o mesmo banco de dados por vez."
				+ "\nFeche o app no outro computador (ícone na bandeja > Sair) e tente de novo"
				+ (externalDatabase ? ",\nou use o banco local deste computador." : ".");
		String[] options = externalDatabase ? IN_USE_EXTERNAL_OPTIONS : IN_USE_OPTIONS;
		StartupChoice[] choices = externalDatabase ? IN_USE_EXTERNAL_CHOICES : IN_USE_CHOICES;
		int chosen = JOptionPane.showOptionDialog(null, message, DesktopBrand.NAME, JOptionPane.DEFAULT_OPTION,
				JOptionPane.WARNING_MESSAGE, null, options, options[0]);
		return chosen < 0 ? StartupChoice.EXIT : choices[chosen];
	}

	static StartupChoice askOtherVersionRunning(Optional<String> runningVersion, String thisVersion) {
		String running = runningVersion.map(StartupFailureDialog::describe).orElse(EARLIER_VERSION);
		LOGGER.warn("Já existe {} aberta neste computador; esta é a {}", running, describe(thisVersion));
		if (GraphicsEnvironment.isHeadless()) {
			return StartupChoice.EXIT;
		}
		String message = "Já está aberta " + running + " do " + DesktopBrand.NAME + " neste computador."
				+ "\n\nPra usar a versão " + versionName(thisVersion)
				+ ", feche a outra pelo ícone na bandeja (Sair) e clique em Tentar de novo.";
		int chosen = JOptionPane.showOptionDialog(null, message, DesktopBrand.NAME, JOptionPane.DEFAULT_OPTION,
				JOptionPane.WARNING_MESSAGE, null, OTHER_VERSION_OPTIONS, OTHER_VERSION_OPTIONS[0]);
		return chosen < 0 ? StartupChoice.EXIT : OTHER_VERSION_CHOICES[chosen];
	}

	private static String describe(String version) {
		return "a versão " + versionName(version);
	}

	private static String versionName(String version) {
		return version.isBlank() ? DEVELOPMENT_VERSION : version;
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
