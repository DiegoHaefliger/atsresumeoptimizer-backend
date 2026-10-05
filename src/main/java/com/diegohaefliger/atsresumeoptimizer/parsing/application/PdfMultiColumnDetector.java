package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

class PdfMultiColumnDetector implements MultiColumnDetector {

	private static final float COLUMN_X_TOLERANCE_POINTS = 10f;
	private static final float MIN_COLUMN_GAP_POINTS = 100f;
	private static final int MIN_LINES_PER_COLUMN = 3;

	@Override
	public boolean isMultiColumn(InputStream input) {
		try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
			return hasStableColumnClusters(collectLineStartXs(document));
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.PDF, exception);
		}
	}

	private List<Float> collectLineStartXs(PDDocument document) throws IOException {
		List<Float> lineStartXs = new ArrayList<>();
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
		return lineStartXs;
	}

	private boolean hasStableColumnClusters(List<Float> lineStartXs) {
		List<Float> sortedXs = new ArrayList<>(lineStartXs);
		sortedXs.sort(Float::compareTo);

		List<float[]> clusters = clusterByProximity(sortedXs);
		List<Float> stableClusterStarts = clusters.stream()
			.filter(cluster -> cluster[1] >= MIN_LINES_PER_COLUMN)
			.map(cluster -> cluster[0])
			.toList();

		if (stableClusterStarts.size() < 2) {
			return false;
		}
		float gap = stableClusterStarts.get(stableClusterStarts.size() - 1) - stableClusterStarts.get(0);
		return gap >= MIN_COLUMN_GAP_POINTS;
	}

	private List<float[]> clusterByProximity(List<Float> sortedXs) {
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
		return clusters;
	}
}
