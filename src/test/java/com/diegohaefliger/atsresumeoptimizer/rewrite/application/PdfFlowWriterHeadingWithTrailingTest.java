package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

/**
 * {@code PdfMultiColumnDetector} (outro pacote, package-private) lê o PDF com
 * {@code sortByPosition(true)}, agrupa a margem esquerda de cada linha por proximidade
 * (tolerância 10pt) e marca "múltiplas colunas" quando duas margens estáveis (≥3 linhas cada)
 * ficam a ≥100pt de distância. Um currículo real com 3+ experiências datadas cai exatamente
 * nesse formato se a data for desenhada colada na borda direita da página. Este teste
 * reimplementa a mesma checagem (não dá pra importar a classe do detector de outro pacote) pra
 * travar que o cargo e a data desenhados por {@link PdfFlowWriter#writeHeadingWithTrailing}
 * nunca voltem a formar essa segunda margem.
 */
class PdfFlowWriterHeadingWithTrailingTest {

	private static final float COLUMN_X_TOLERANCE_POINTS = 10f;
	private static final float MIN_COLUMN_GAP_POINTS = 100f;
	private static final int MIN_LINES_PER_COLUMN = 3;

	@Test
	void keepsHeadingAndTrailingPeriodCloseEnoughToNeverLookLikeASecondColumn() throws Exception {
		StructuredResume content = threeDatedEntries();

		List<ContactField> contact = ContactField.collect("ana@email.com | (55) 99999-0000", null, null, "Panambi, RS");

		for (ResumeTemplate template : ResumeTemplate.values()) {
			byte[] bytes = new ThemedPdfRenderer().render(ResumeTheme.of(template), content, contact);
			assertThat(looksLikeMultiColumn(bytes)).as(template.name()).isFalse();
		}
	}

	private StructuredResume threeDatedEntries() {
		ResumeEntry entryA = new ResumeEntry(
				"Desenvolvedora Backend", "Jan 2020 - Dez 2022", "Empresa X", null, List.of(), "Java");
		ResumeEntry entryB = new ResumeEntry(
				"Desenvolvedora Pleno", "Mar 2018 - Jan 2020", "Empresa Y", null, List.of(), "Java");
		ResumeEntry entryC = new ResumeEntry(
				"Estagiária", "Fev 2016 - Fev 2018", "Empresa Z", null, List.of(), "Python");
		ResumeSection section = new ResumeSection(
				"EXPERIENCIA", ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, null, null,
				List.of(entryA, entryB, entryC));
		return new StructuredResume("Ana Silva", "Backend Java", List.of(section));
	}

	private boolean looksLikeMultiColumn(byte[] pdf) throws IOException {
		List<Float> lineStartXs = new ArrayList<>();
		try (PDDocument document = Loader.loadPDF(pdf)) {
			PDFTextStripper stripper = new PDFTextStripper() {
				@Override
				protected void writeString(String text, List<TextPosition> textPositions) {
					if (!text.isBlank() && !textPositions.isEmpty()) {
						lineStartXs.add(textPositions.get(0).getXDirAdj());
					}
				}
			};
			stripper.setSortByPosition(true);
			stripper.getText(document);
		}

		List<Float> sortedXs = new ArrayList<>(lineStartXs);
		sortedXs.sort(Float::compareTo);
		List<float[]> clusters = new ArrayList<>();
		float clusterStart = Float.NaN;
		int clusterSize = 0;
		for (float x : sortedXs) {
			if (Float.isNaN(clusterStart) || x - clusterStart > COLUMN_X_TOLERANCE_POINTS) {
				if (clusterSize > 0) {
					clusters.add(new float[] { clusterStart, clusterSize });
				}
				clusterStart = x;
				clusterSize = 0;
			}
			clusterSize++;
		}
		if (clusterSize > 0) {
			clusters.add(new float[] { clusterStart, clusterSize });
		}

		List<Float> stableClusterStarts =
				clusters.stream().filter(cluster -> cluster[1] >= MIN_LINES_PER_COLUMN).map(cluster -> cluster[0]).toList();
		if (stableClusterStarts.size() < 2) {
			return false;
		}
		float gap = stableClusterStarts.get(stableClusterStarts.size() - 1) - stableClusterStarts.get(0);
		return gap >= MIN_COLUMN_GAP_POINTS;
	}
}
