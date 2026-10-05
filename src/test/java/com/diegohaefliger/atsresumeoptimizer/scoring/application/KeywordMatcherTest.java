package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.KeywordMatch;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeywordMatcherTest {

	@Mock
	private TechTermRepository techTermRepository;

	private KeywordMatcher matcher;

	@BeforeEach
	void setUp() {
		TechTerm kubernetes = mock(TechTerm.class);
		when(kubernetes.canonical()).thenReturn("Kubernetes");
		when(kubernetes.aliases()).thenReturn(List.of("k8s"));
		when(techTermRepository.findAll()).thenReturn(List.of(kubernetes));
		matcher = new KeywordMatcher(techTermRepository);
	}

	@Test
	void findsAKeywordWrittenWithAnotherSpellingInTheResume() {
		JobStructured job = job(List.of("microserviços"), Map.of());

		KeywordMatch match = matcher.match(resume("Arquitetura: Microsserviços, APIs REST"), job).getFirst();

		assertThat(match.foundExact()).isTrue();
	}

	@Test
	void doesNotMatchAShortEquivalentInsideAnotherWord() {
		JobStructured job = job(List.of("Terraform"), Map.of("Terraform", List.of("IaC")));

		KeywordMatch match = matcher.match(resume("Criação de pipelines de build com Docker."), job).getFirst();

		assertThat(match.found()).isFalse();
	}

	@Test
	void doesNotMatchAKeywordThatIsOnlyThePrefixOfAnotherTechnology() {
		KeywordMatch match = matcher.match(resume("Front-end em JavaScript e TypeScript."), job(List.of("Java"), Map.of()))
				.getFirst();

		assertThat(match.found()).isFalse();
		assertThat(match.occurrences()).isZero();
	}

	@Test
	void stillMatchesWholeWordsAliasesAndEquivalents() {
		JobStructured job = job(List.of("Kubernetes", "Terraform", "C++"), Map.of("Terraform", List.of("IaC")));

		List<KeywordMatch> matches = matcher.match(resume("Deploy em K8s, IaC com Pulumi e módulos em C++."), job);

		assertThat(matches).allMatch(KeywordMatch::found);
	}

	private JobStructured job(List<String> keywords, Map<String, List<String>> equivalents) {
		return new JobStructured("Backend", "pleno", null, null, List.of(), keywords, equivalents);
	}

	private ParsingResult resume(String rawText) {
		return new ParsingResult(new NormalizedDocument(SourceFormat.PDF, rawText, "", 1),
				new ParsingSignals(false, false, false, false, false, false, false, false), List.of(),
				new ContactInfo(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
	}
}
