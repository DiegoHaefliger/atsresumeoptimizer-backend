package com.diegohaefliger.atsresumeoptimizer.release.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.diegohaefliger.atsresumeoptimizer.release.domain.AppVersion;
import com.diegohaefliger.atsresumeoptimizer.release.domain.LatestRelease;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GitHubReleaseClientTest {

	private static final String LATEST = "https://api.github.com/repos/DiegoHaefliger/atsresumeoptimizer/releases/latest";

	private MockRestServiceServer server;
	private GitHubReleaseClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new GitHubReleaseClient(new ReleaseProperties(true, "DiegoHaefliger/atsresumeoptimizer",
				"https://api.github.com", Duration.ofHours(6), Duration.ofMinutes(30), Duration.ofSeconds(5)), builder);
	}

	@Test
	void readsTheLatestPublishedRelease() {
		server.expect(requestTo(LATEST))
				.andExpect(header("Accept", "application/vnd.github+json"))
				.andRespond(withSuccess(release("v1.2.0", false, false), MediaType.APPLICATION_JSON));

		Optional<LatestRelease> latest = client.latest();

		assertThat(latest).contains(new LatestRelease(new AppVersion(1, 2, 0, null),
				"https://github.com/DiegoHaefliger/atsresumeoptimizer/releases/tag/v1.2.0",
				Instant.parse("2026-10-10T12:00:00Z")));
	}

	@Test
	void ignoresPreReleasesAndTagsOutsideTheVersionFormat() {
		server.expect(requestTo(LATEST)).andRespond(withSuccess(release("v1.3.0-rc1", false, true), MediaType.APPLICATION_JSON));
		assertThat(client.latest()).isEmpty();

		server.reset();
		server.expect(requestTo(LATEST)).andRespond(withSuccess(release("nightly", false, false), MediaType.APPLICATION_JSON));
		assertThat(client.latest()).isEmpty();
	}

	@Test
	void treatsAMissingReleaseAsNoUpdate() {
		server.expect(requestTo(LATEST)).andRespond(withResourceNotFound());

		assertThat(client.latest()).isEmpty();
	}

	private static String release(String tag, boolean draft, boolean prerelease) {
		return """
				{"tag_name": "%s", "html_url": "https://github.com/DiegoHaefliger/atsresumeoptimizer/releases/tag/%s",
				 "published_at": "2026-10-10T12:00:00Z", "draft": %s, "prerelease": %s, "assets": []}
				""".formatted(tag, tag, draft, prerelease);
	}
}
