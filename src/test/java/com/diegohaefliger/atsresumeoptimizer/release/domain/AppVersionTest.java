package com.diegohaefliger.atsresumeoptimizer.release.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppVersionTest {

	@Test
	void readsTagsWithOrWithoutThePrefix() {
		assertThat(AppVersion.parse("v1.2.3")).contains(new AppVersion(1, 2, 3, null));
		assertThat(AppVersion.parse("1.2.3-SNAPSHOT")).contains(new AppVersion(1, 2, 3, "SNAPSHOT"));
		assertThat(AppVersion.parse("versao-nova")).isEmpty();
		assertThat(AppVersion.parse(null)).isEmpty();
	}

	@Test
	void comparesNumbersAsNumbersNotText() {
		assertThat(version("1.10.0").isNewerThan(version("1.9.9"))).isTrue();
		assertThat(version("2.0.0").isNewerThan(version("1.99.99"))).isTrue();
		assertThat(version("1.0.0").isNewerThan(version("1.0.0"))).isFalse();
	}

	@Test
	void treatsTheStableReleaseAsNewerThanItsPreRelease() {
		assertThat(version("1.0.0").isNewerThan(version("1.0.0-SNAPSHOT"))).isTrue();
		assertThat(version("1.0.0-rc1").isNewerThan(version("1.0.0"))).isFalse();
	}

	@Test
	void printsWithoutThePrefix() {
		assertThat(version("v1.2.3")).hasToString("1.2.3");
		assertThat(version("1.2.3-rc1")).hasToString("1.2.3-rc1");
	}

	private static AppVersion version(String text) {
		return AppVersion.parse(text).orElseThrow();
	}
}
