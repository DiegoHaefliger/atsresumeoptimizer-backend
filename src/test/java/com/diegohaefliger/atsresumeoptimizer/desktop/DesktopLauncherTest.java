package com.diegohaefliger.atsresumeoptimizer.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DesktopLauncherTest {

	@Test
	void runsAsDesktopWhenTheProfileComesInTheArguments() {
		assertThat(DesktopLauncher.requested(new String[] {"--spring.profiles.active=desktop"})).isTrue();
		assertThat(DesktopLauncher.requested(new String[] {"--spring.profiles.active=local,desktop"})).isTrue();
	}

	@Test
	void runsAsServerOtherwise() {
		assertThat(DesktopLauncher.requested(new String[] {})).isFalse();
		assertThat(DesktopLauncher.requested(new String[] {"--spring.profiles.active=desktopish"})).isFalse();
	}
}
