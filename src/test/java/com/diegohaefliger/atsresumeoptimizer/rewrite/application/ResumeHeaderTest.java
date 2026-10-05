package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResumeHeaderTest {

	private static final String RAW_TEXT = """
			Maria Exemplo
			Desenvolvedora Backend

			maria@exemplo.com | (51) 99999-0000
			RESUMO PROFISSIONAL
			Desenvolvedora backend com 4 anos de experiência.""";

	@Test
	void keepsTheLinesBeforeTheFirstSectionTitle() {
		ResumeHeader header = ResumeHeader.of(parsing(RAW_TEXT, List.of(new Section("RESUMO PROFISSIONAL", true, "..."))));

		assertThat(header.lines())
				.containsExactly("Maria Exemplo", "Desenvolvedora Backend", "maria@exemplo.com | (51) 99999-0000");
	}

	@Test
	void keepsTheAiNameWhenItAppearsInTheOriginalText() {
		ResumeHeader header = ResumeHeader.of(parsing(RAW_TEXT, List.of(new Section("RESUMO PROFISSIONAL", true, "..."))));

		assertThat(header.resolveName("MARIA EXEMPLO", RAW_TEXT)).isEqualTo("MARIA EXEMPLO");
	}

	@Test
	void fallsBackToTheFirstHeaderLineWhenTheAiNameIsNotInTheOriginal() {
		ResumeHeader header = ResumeHeader.of(parsing(RAW_TEXT, List.of(new Section("RESUMO PROFISSIONAL", true, "..."))));

		assertThat(header.resolveName("nome completo da pessoa", RAW_TEXT)).isEqualTo("Maria Exemplo");
		assertThat(header.resolveName(null, RAW_TEXT)).isEqualTo("Maria Exemplo");
	}

	@Test
	void leavesTheNameEmptyWhenTheFirstHeaderLineIsNotAName() {
		String rawText = "maria@exemplo.com | (51) 99999-0000\nRESUMO PROFISSIONAL\nTexto.";
		ResumeHeader header = ResumeHeader.of(parsing(rawText, List.of(new Section("RESUMO PROFISSIONAL", true, "Texto."))));

		assertThat(header.resolveName("nome completo da pessoa", rawText)).isEmpty();
	}

	private ParsingResult parsing(String rawText, List<Section> sections) {
		return new ParsingResult(new NormalizedDocument(SourceFormat.DOCX, rawText, rawText, 1),
				new ParsingSignals(false, false, false, false, false, false, false, false), sections,
				new ContactInfo(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
	}
}
