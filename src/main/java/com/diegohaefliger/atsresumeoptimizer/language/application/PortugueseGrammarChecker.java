package com.diegohaefliger.atsresumeoptimizer.language.application;

import com.diegohaefliger.atsresumeoptimizer.language.domain.GrammarCheckException;
import com.diegohaefliger.atsresumeoptimizer.language.domain.GrammarIssue;
import java.io.IOException;
import java.util.List;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.rules.RuleMatch;
import org.springframework.stereotype.Component;

/** Instanciar {@code BrazilianPortuguese} direto falha: o LanguageTool já a registra por reflexão em {@code Languages}. */
@Component
public class PortugueseGrammarChecker {

	private static final String PT_BR_SHORT_CODE = "pt-BR";

	private final JLanguageTool languageTool = new JLanguageTool(Languages.getLanguageForShortCode(PT_BR_SHORT_CODE));

	public List<GrammarIssue> check(String text) {
		try {
			return languageTool.check(text).stream().map(this::toGrammarIssue).toList();
		} catch (IOException | RuntimeException exception) {
			throw new GrammarCheckException(exception);
		}
	}

	private GrammarIssue toGrammarIssue(RuleMatch match) {
		return new GrammarIssue(match.getMessage(), match.getFromPos(), match.getToPos(), List.copyOf(match.getSuggestedReplacements()));
	}
}
