package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceSaved;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteria;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteriaProvider;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
import com.diegohaefliger.atsresumeoptimizer.preference.SearchSite;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuggestionExclusionSweeperTest {

	@Mock
	private SuggestionRepository repository;

	@Mock
	private JobSearchCriteriaProvider criteriaProvider;

	private static JobSearchCriteria criteria(List<String> excluded) {
		return new JobSearchCriteria(List.of("java"), excluded, List.of(), List.of(), List.of(), List.of(),
				PostedWithin.PAST_WEEK, 10, 70, true, "0 0 */12 * * *", List.of(SearchSite.values()));
	}

	private static SuggestionEntity suggestion(String title, String description) {
		SuggestionEntity entity = new SuggestionEntity(UUID.randomUUID());
		entity.setTitle(title);
		entity.setCompany("Acme");
		entity.setDescription(description);
		entity.setStatus(SuggestionStatus.NEW);
		return entity;
	}

	@Test
	void movesNewSuggestionsThatCiteAnExcludedWordOutOfTheSuggestedList() {
		SuggestionEntity dotNet = suggestion("Sênior .NET Backend Developer", "C# e Azure");
		SuggestionEntity node = suggestion("Senior Backend Engineer", "<p>TypeScript, Node.js</p>");
		SuggestionEntity java = suggestion("Dev Java", "Spring Boot e Kafka");
		when(criteriaProvider.current()).thenReturn(criteria(List.of(".NET", "Node")));
		when(repository.findByStatus(eq(SuggestionStatus.NEW), any())).thenReturn(List.of(dotNet, node, java));

		new SuggestionExclusionSweeper(repository, criteriaProvider).onPreferenceSaved(new JobPreferenceSaved());

		assertThat(dotNet.getStatus()).isEqualTo(SuggestionStatus.FILTERED);
		assertThat(node.getStatus()).isEqualTo(SuggestionStatus.FILTERED);
		assertThat(java.getStatus()).isEqualTo(SuggestionStatus.NEW);
		verify(repository, never()).save(java);
	}

	@Test
	void leavesSuggestionsAloneWhenNothingIsExcluded() {
		when(criteriaProvider.current()).thenReturn(criteria(List.of()));

		new SuggestionExclusionSweeper(repository, criteriaProvider).onPreferenceSaved(new JobPreferenceSaved());

		verify(repository, never()).findByStatus(any(), any());
	}
}
