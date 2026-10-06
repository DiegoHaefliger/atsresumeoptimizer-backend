package com.diegohaefliger.atsresumeoptimizer.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JobStructuredTest {

	private static final List<String> TEN = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");

	@Test
	void withoutAUserChoiceTheFirstFiveKeywordsAreThePriorityOnes() {
		JobStructured job = new JobStructured("Dev", null, null, null, List.of(), TEN, Map.of());

		assertThat(job.priorityKeywords()).containsExactly("a", "b", "c", "d", "e");
	}

	@Test
	void keepsTheUserChoiceAsIs() {
		JobStructured job = new JobStructured("Dev", null, null, null, List.of(), TEN, Map.of(), null, List.of("j", "a"));

		assertThat(job.priorityKeywords()).containsExactly("j", "a");
	}
}
