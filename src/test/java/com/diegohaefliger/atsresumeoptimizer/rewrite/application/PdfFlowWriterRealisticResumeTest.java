package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

/**
 * Currículo real com várias experiências repetindo o mesmo stack de tecnologias (padrão comum:
 * "Tecnologias: Java, Spring Boot, Docker, ..." em cada bloco) reproduzia dois falsos positivos
 * próprios do app, mesmo sem nenhuma coluna ou tabela de verdade:
 *
 * <p>1. {@code PdfMultiColumnDetector} (lê com {@code sortByPosition(true)}): quando uma palavra é
 * desenhada só avançando o cursor X sem nenhum glyph preenchendo o espaço até a próxima, o PDFBox
 * fragmenta a linha numa posição por palavra ao reordenar por posição. Como o mesmo texto se repete
 * a cada bloco de experiência, essas posições reaparecem sempre no mesmo x e formam clusters
 * estáveis espalhados pela largura da página — exatamente o padrão de duas colunas.
 *
 * <p>2. {@code PdfTableDetector}: os ícones vetoriais de contato (telefone/e-mail/LinkedIn) usavam
 * o operador {@code re} do PDF (via {@code addRect}) pra desenhar o quadrado — o mesmo sinal gráfico
 * que o detector de tabela conta. Três ícones bastam pra bater o mínimo de retângulos.
 *
 * <p>Testes na classe {@code parsing.application} não alcançam essas duas (são package-private
 * noutro pacote), então este teste reimplementa a mesma leitura/contagem que os detectores fazem.
 */
class PdfFlowWriterRealisticResumeTest {

	private static final float COLUMN_X_TOLERANCE_POINTS = 10f;
	private static final float MIN_COLUMN_GAP_POINTS = 100f;
	private static final int MIN_LINES_PER_COLUMN = 3;
	private static final int MIN_RECTANGLES_FOR_TABLE = 3;

	@Test
	void rendersRepeatedTechStackAcrossManyEntriesWithoutLookingLikeColumnsOrATable() throws Exception {
		StructuredResume content = sixEntriesWithRepeatingTechStack();

		List<ContactField> contact = ContactField.collect(
				"ana@email.com | (55) 99999-0000 | linkedin.com/in/ana-silva", "github.com/anasilva", null, "Panambi, RS");

		byte[] modernBytes = new ThemedPdfRenderer().render(ResumeTheme.of(ResumeTemplate.MODERN_BLUE), content, contact);
		byte[] classicBytes = new ThemedPdfRenderer().render(ResumeTheme.of(ResumeTemplate.CLASSIC), content, contact);

		assertThat(looksLikeMultiColumn(modernBytes)).isFalse();
		assertThat(looksLikeMultiColumn(classicBytes)).isFalse();
		assertThat(countRectangles(modernBytes)).isLessThan(MIN_RECTANGLES_FOR_TABLE);
	}

	private StructuredResume sixEntriesWithRepeatingTechStack() {
		List<ResumeEntry> entries = new ArrayList<>();
		String[] periods = {
				"Jan 2020 - Dez 2022", "Mar 2018 - Jan 2020", "Fev 2016 - Fev 2018", "Jan 2014 - Jan 2016",
				"Jan 2012 - Dez 2013", "Jan 2010 - Dez 2011",
		};
		for (int i = 0; i < periods.length; i++) {
			entries.add(new ResumeEntry(
					"Desenvolvedora Backend " + i, periods[i], "Empresa " + i, null,
					List.of(List.of(new TextSpan("Desenvolvimento e evolução de módulos back-end críticos.", false))),
					"Java, Spring Boot, Docker, Kubernetes, AWS, Swagger/OpenAPI, Dynatrace, Grafana, Kibana"));
		}
		ResumeSection section = new ResumeSection(
				"EXPERIENCIA", ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, null, null, entries);
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

	private int countRectangles(byte[] pdf) throws IOException {
		int[] count = { 0 };
		try (PDDocument document = Loader.loadPDF(pdf)) {
			for (PDPage page : document.getPages()) {
				PDFGraphicsStreamEngine counter = new PDFGraphicsStreamEngine(page) {
					@Override
					public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
						count[0]++;
					}

					@Override
					public void drawImage(PDImage pdImage) {
					}

					@Override
					public void clip(int windingRule) {
					}

					@Override
					public void moveTo(float x, float y) {
					}

					@Override
					public void lineTo(float x, float y) {
					}

					@Override
					public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
					}

					@Override
					public Point2D getCurrentPoint() {
						return new Point2D.Float(0, 0);
					}

					@Override
					public void closePath() {
					}

					@Override
					public void endPath() {
					}

					@Override
					public void strokePath() {
					}

					@Override
					public void fillPath(int windingRule) {
					}

					@Override
					public void fillAndStrokePath(int windingRule) {
					}

					@Override
					public void shadingFill(COSName shadingName) {
					}
				};
				counter.processPage(page);
			}
		}
		return count[0];
	}
}
