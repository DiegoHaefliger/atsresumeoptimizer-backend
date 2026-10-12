package com.diegohaefliger.atsresumeoptimizer.desktop;

import com.diegohaefliger.atsresumeoptimizer.AtsResumeOptimizerBackendApplication;
import java.io.IOException;

public final class DesktopApplication {

	private DesktopApplication() {
	}

	public static void main(String[] args) throws IOException {
		new DesktopLauncher(AtsResumeOptimizerBackendApplication.class).launch(args);
	}
}
