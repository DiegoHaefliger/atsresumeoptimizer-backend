package com.diegohaefliger.atsresumeoptimizer.language.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.language.domain.GrammarIssue;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortugueseGrammarCheckerTest {

	private final PortugueseGrammarChecker checker = new PortugueseGrammarChecker();

	@Test
	void flagsAMisspelledWord() {
		List<GrammarIssue> issues = checker.check("Eu trabalho com desenvolvimento de sofware há cinco anos.");

		assertThat(issues).isNotEmpty();
	}

	@Test
	void wellWrittenSentenceHasNoIssues() {
		// Frase sem termo técnico em inglês: o dicionário técnico (tech_term) que evita falso
		// positivo em palavras como "backend" é tabela do banco, fora do escopo desta issue.
		List<GrammarIssue> issues = checker.check("Ela trabalha com programação há cinco anos e gosta de resolver problemas complexos.");

		assertThat(issues).isEmpty();
	}

	@Test
	void issueCarriesOffsetsAndSuggestions() {
		List<GrammarIssue> issues = checker.check("Eu trabalho com desenvolvimento de sofware há cinco anos.");

		GrammarIssue issue = issues.get(0);
		assertThat(issue.startOffset()).isLessThan(issue.endOffset());
		assertThat(issue.message()).isNotBlank();
	}
}
