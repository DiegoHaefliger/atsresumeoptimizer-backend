package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.awt.Image;
import java.awt.Toolkit;
import java.util.Objects;

final class DesktopBrand {

	static final String NAME = "ATS Resume Optimizer";

	private static final String ICON = "/desktop/icon.png";

	private DesktopBrand() {
	}

	static Image icon() {
		return Toolkit.getDefaultToolkit().getImage(Objects.requireNonNull(DesktopBrand.class.getResource(ICON)));
	}
}
