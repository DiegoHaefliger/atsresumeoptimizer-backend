package com.diegohaefliger.atsresumeoptimizer.desktop;

import com.diegohaefliger.atsresumeoptimizer.AtsResumeOptimizerBackendApplication;
import java.io.IOException;
import java.nio.file.Path;

public final class DesktopApplication {

	private DesktopApplication() {
	}

	public static void main(String[] args) throws IOException {
		Path dataDir = DesktopDataDir.resolve();
		Thread.setDefaultUncaughtExceptionHandler(new CrashLog(dataDir));
		new DesktopLauncher(AtsResumeOptimizerBackendApplication.class, dataDir).launch(args);
	}
}
