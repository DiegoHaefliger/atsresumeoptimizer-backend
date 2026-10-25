package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.InvalidPostLinkException;

class PostLinkTest {

	@Test
	void extractsTheActivityIdFromAPostLink() {
		PostLink link = PostLink.parse(
				"https://www.linkedin.com/posts/fulano_vaga-java-activity-7251234567890123456-AbCd?utm_source=share");

		assertThat(link.externalId()).isEqualTo("7251234567890123456");
	}

	@Test
	void extractsTheIdFromAFeedUpdateUrn() {
		PostLink link = PostLink.parse("https://br.linkedin.com/feed/update/urn:li:activity:7251234567890123456/");

		assertThat(link.externalId()).isEqualTo("7251234567890123456");
	}

	@Test
	void hashesThePathWhenThereIsNoId() {
		PostLink link = PostLink.parse("https://www.linkedin.com/posts/fulano_sem-id");

		assertThat(link.externalId()).hasSize(64);
	}

	@Test
	void rejectsOtherSites() {
		assertThatThrownBy(() -> PostLink.parse("https://evil.com/posts/x-activity-7251234567890123456"))
				.isInstanceOf(InvalidPostLinkException.class);
		assertThatThrownBy(() -> PostLink.parse("https://notlinkedin.com/posts/x"))
				.isInstanceOf(InvalidPostLinkException.class);
	}

	@Test
	void rejectsInsecureAndMalformedLinks() {
		assertThatThrownBy(() -> PostLink.parse("http://www.linkedin.com/posts/x"))
				.isInstanceOf(InvalidPostLinkException.class);
		assertThatThrownBy(() -> PostLink.parse("não é link")).isInstanceOf(InvalidPostLinkException.class);
		assertThatThrownBy(() -> PostLink.parse(null)).isInstanceOf(InvalidPostLinkException.class);
	}
}
