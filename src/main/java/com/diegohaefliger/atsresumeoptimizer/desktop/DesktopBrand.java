package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

final class DesktopBrand {

	static final String NAME = "ATS Resume Optimizer";

	private static final int ICON_SIZE = 64;
	private static final int CORNER = 16;
	private static final float FONT_RATIO = 0.36f;
	private static final String LETTERS = "ATS";
	private static final Color BACKGROUND = new Color(0x2563EB);

	private DesktopBrand() {
	}

	static Image icon() {
		BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setColor(BACKGROUND);
		graphics.fillRoundRect(0, 0, ICON_SIZE, ICON_SIZE, CORNER, CORNER);
		graphics.setColor(Color.WHITE);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(ICON_SIZE * FONT_RATIO)));
		int x = (ICON_SIZE - graphics.getFontMetrics().stringWidth(LETTERS)) / 2;
		int y = (ICON_SIZE + graphics.getFontMetrics().getAscent() - graphics.getFontMetrics().getDescent()) / 2;
		graphics.drawString(LETTERS, x, y);
		graphics.dispose();
		return image;
	}
}
