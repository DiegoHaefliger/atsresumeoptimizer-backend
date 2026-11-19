package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LinkedInProfileUrlTest {

	@Test
	void findsTheProfileInTheContactBlockOfTheExportedPdf() {
		var found = LinkedInProfileUrl.find("Contact\nwww.linkedin.com/in/ana-souza (LinkedIn)\nTop Skills").orElseThrow();

		assertThat(found.url()).isEqualTo("https://www.linkedin.com/in/ana-souza/");
		assertThat(found.customUrl()).isEqualTo("linkedin.com/in/ana-souza");
	}

	@Test
	void treatsTheGeneratedSuffixAsNoCustomUrl() {
		var found = LinkedInProfileUrl.find("www.linkedin.com/in/ana-souza-1a2b3c4d5 (LinkedIn)").orElseThrow();

		assertThat(found.url()).isEqualTo("https://www.linkedin.com/in/ana-souza-1a2b3c4d5/");
		assertThat(found.customUrl()).isNull();
	}

	@Test
	void isEmptyWhenTheTextHasNoProfileAddress() {
		assertThat(LinkedInProfileUrl.find("Currículo de Ana, sem LinkedIn")).isEmpty();
	}
}
