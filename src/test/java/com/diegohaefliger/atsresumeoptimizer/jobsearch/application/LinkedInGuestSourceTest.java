package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class LinkedInGuestSourceTest {

	private final JobSearchProperties properties = new JobSearchProperties("https://linkedin.test", "test-agent",
			Duration.ZERO, Duration.ZERO, Duration.ofSeconds(1), "Brazil");

	private MockRestServiceServer server;
	private LinkedInGuestSource source;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		source = new LinkedInGuestSource(properties, builder);
	}

	private static String fixture(String name) throws IOException {
		return new ClassPathResource("jobsearch/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

	private static SourceQuery query() {
		return new SourceQuery("desenvolvedor java", "Curitiba", PostedWithin.PAST_WEEK, List.of(WorkModel.REMOTE),
				List.of("Pleno"), List.of(ContractType.CLT), 10);
	}

	@Test
	void sendsTheCriteriaAsLinkedInFilters() throws IOException {
		server.expect(queryParam("keywords", "desenvolvedor%20java"))
				.andExpect(queryParam("location", "Curitiba"))
				.andExpect(queryParam("f_TPR", "r604800"))
				.andExpect(queryParam("f_WT", "2"))
				.andExpect(queryParam("f_JT", "F"))
				.andExpect(queryParam("f_E", "3"))
				.andExpect(queryParam("start", "10"))
				.andRespond(withSuccess(fixture("linkedin-search.html"), MediaType.TEXT_HTML));

		assertThat(source.search(query())).hasSize(2);
		server.verify();
	}

	@Test
	void omitsTheFiltersTheUserLeftEmpty() {
		var open = new SourceQuery("java", "Brazil", PostedWithin.ANY, List.of(), List.of(), List.of(), 0);
		server.expect(requestTo("https://linkedin.test/jobs-guest/jobs/api/seeMoreJobPostings/search?keywords=java&location=Brazil&start=0"))
				.andRespond(withSuccess("", MediaType.TEXT_HTML));

		assertThat(source.search(open)).isEmpty();
		server.verify();
	}

	@Test
	void fetchesTheDetailOfAJob() throws IOException {
		server.expect(requestTo("https://linkedin.test/jobs-guest/jobs/api/jobPosting/1000000001"))
				.andRespond(withSuccess(fixture("linkedin-detail.html"), MediaType.TEXT_HTML));

		assertThat(source.detail("1000000001")).isPresent();
	}

	@Test
	void treatsAMissingJobAsNoDetail() {
		server.expect(requestTo("https://linkedin.test/jobs-guest/jobs/api/jobPosting/9"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThat(source.detail("9")).isEmpty();
	}

	@Test
	void explainsWhenLinkedInLimitsTheRequests() {
		server.expect(requestTo("https://linkedin.test/jobs-guest/jobs/api/jobPosting/1"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertThatThrownBy(() -> source.detail("1"))
				.isInstanceOf(JobSourceException.class)
				.hasMessageContaining("limitou");
	}
}
