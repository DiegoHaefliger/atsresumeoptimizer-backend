package com.diegohaefliger.atsresumeoptimizer.language.application;

import com.diegohaefliger.atsresumeoptimizer.language.domain.GrammarCheckException;
import com.diegohaefliger.atsresumeoptimizer.language.domain.GrammarIssue;
import java.io.IOException;
import java.util.List;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.rules.RuleMatch;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Instanciar {@code BrazilianPortuguese} direto falha: o LanguageTool já a registra por reflexão em {@code Languages}. O carregamento custa ~2s, então fica fora da inicialização. */
@Component
public class PortugueseGrammarChecker {

	private static final String PT_BR_SHORT_CODE = "pt-BR";

	private volatile JLanguageTool languageTool;

	@EventListener(ApplicationReadyEvent.class)
	void warmUp() {
		Thread.ofPlatform().daemon().name("languagetool-warmup").start(this::languageTool);
	}

	public List<GrammarIssue> check(String text) {
		try {
			return languageTool().check(text).stream().map(this::toGrammarIssue).toList();
		} catch (IOException | RuntimeException exception) {
			throw new GrammarCheckException(exception);
		}
	}

	private JLanguageTool languageTool() {
		JLanguageTool tool = languageTool;
		if (tool == null) {
			synchronized (this) {
				tool = languageTool;
				if (tool == null) {
					tool = new JLanguageTool(Languages.getLanguageForShortCode(PT_BR_SHORT_CODE));
					languageTool = tool;
				}
			}
		}
		return tool;
	}

	private GrammarIssue toGrammarIssue(RuleMatch match) {
		return new GrammarIssue(match.getMessage(), match.getFromPos(), match.getToPos(), List.copyOf(match.getSuggestedReplacements()));
	}
}
