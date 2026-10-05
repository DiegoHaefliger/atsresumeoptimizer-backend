package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import java.awt.geom.Point2D;
import java.io.IOException;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;

class PdfLineGridCounter extends PDFGraphicsStreamEngine {

	private static final float LINE_TOLERANCE_POINTS = 1f;
	private static final float MIN_LINE_LENGTH_POINTS = 10f;

	private float currentX;
	private float currentY;
	private float startX;
	private float startY;
	private int horizontalLines;
	private int verticalLines;
	private int rectangles;

	PdfLineGridCounter(PDPage page) {
		super(page);
	}

	int horizontalLines() {
		return horizontalLines;
	}

	int verticalLines() {
		return verticalLines;
	}

	int rectangles() {
		return rectangles;
	}

	@Override
	public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
		rectangles++;
	}

	@Override
	public void moveTo(float x, float y) {
		currentX = x;
		currentY = y;
		startX = x;
		startY = y;
	}

	@Override
	public void lineTo(float x, float y) {
		classifyLine(currentX, currentY, x, y);
		currentX = x;
		currentY = y;
	}

	private void classifyLine(float x0, float y0, float x1, float y1) {
		boolean sameY = Math.abs(y1 - y0) < LINE_TOLERANCE_POINTS;
		boolean sameX = Math.abs(x1 - x0) < LINE_TOLERANCE_POINTS;
		if (sameY && Math.abs(x1 - x0) > MIN_LINE_LENGTH_POINTS) {
			horizontalLines++;
		} else if (sameX && Math.abs(y1 - y0) > MIN_LINE_LENGTH_POINTS) {
			verticalLines++;
		}
	}

	@Override
	public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
		currentX = x3;
		currentY = y3;
	}

	@Override
	public Point2D getCurrentPoint() {
		return new Point2D.Float(currentX, currentY);
	}

	@Override
	public void closePath() {
		currentX = startX;
		currentY = startY;
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

	@Override
	public void clip(int windingRule) {
	}

	@Override
	public void drawImage(PDImage pdImage) throws IOException {
	}
}
