package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static com.diegohaefliger.atsresumeoptimizer.parsing.application.OdtTestFixture.odt;
import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class OdtExtractorTest {

	private final OdtExtractor extractor = new OdtExtractor();

	@Test
	void extractsParagraphsAndTableCellsIntoBothRawAndStructuredText() throws Exception {
		byte[] bytes = odt().withParagraphs("Ana Silva", "EXPERIENCIA PROFISSIONAL").withTable("Java", "Spring Boot").build();

		var document = extractor.extract(new ByteArrayInputStream(bytes));

		assertThat(document.sourceFormat()).isEqualTo(SourceFormat.ODT);
		assertThat(document.rawText()).contains("Ana Silva").contains("EXPERIENCIA PROFISSIONAL").contains("Java");
		assertThat(document.structuredText()).contains("Java | Spring Boot");
	}
}
