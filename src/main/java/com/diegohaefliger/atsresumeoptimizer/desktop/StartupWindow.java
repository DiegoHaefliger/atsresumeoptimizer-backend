package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

final class StartupWindow {

	private static final int WIDTH = 460;
	private static final int HEIGHT = 190;
	private static final int PADDING = 24;
	private static final int LOGO_SIZE = 72;
	private static final int ACCENT_HEIGHT = 4;
	private static final Color BACKGROUND = new Color(0x11131f);
	private static final Color BORDER = new Color(0x2a2e45);
	private static final Color TEXT = new Color(0xe8eaf2);
	private static final Color MUTED = new Color(0x959db2);
	private static final Color BRAND_START = new Color(0x4f46e5);
	private static final Color BRAND_END = new Color(0x7c3aed);

	private JWindow window;
	private ProgressStrip strip;

	private StartupWindow() {
	}

	static StartupWindow show() {
		StartupWindow startup = new StartupWindow();
		if (!GraphicsEnvironment.isHeadless()) {
			SwingUtilities.invokeLater(startup::open);
		}
		return startup;
	}

	void close() {
		SwingUtilities.invokeLater(() -> {
			if (strip != null) {
				strip.stop();
				strip = null;
			}
			if (window != null) {
				window.dispose();
				window = null;
			}
		});
	}

	private void open() {
		strip = new ProgressStrip();
		window = new JWindow();
		window.setContentPane(content(strip));
		window.setSize(WIDTH, HEIGHT);
		window.setIconImage(DesktopBrand.icon());
		window.setLocationRelativeTo(null);
		window.setAlwaysOnTop(true);
		window.setVisible(true);
		strip.start();
	}

	static JPanel content(JComponent progress) {
		JLabel logo = new JLabel(new ImageIcon(DesktopBrand.icon().getScaledInstance(LOGO_SIZE, LOGO_SIZE, Image.SCALE_SMOOTH)));
		logo.setAlignmentY(0f);

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.add(label(DesktopBrand.NAME, Font.BOLD, 20, TEXT));
		DesktopBrand.version().ifPresent(version -> text.add(label("Versão " + version, Font.PLAIN, 12, MUTED)));
		text.add(Box.createVerticalStrut(6));
		text.add(label("Abrindo o aplicativo...", Font.PLAIN, 14, TEXT));
		text.add(Box.createVerticalStrut(2));
		text.add(label("Na primeira vez pode levar um minuto.", Font.PLAIN, 12, MUTED));

		JPanel top = new JPanel(new BorderLayout(PADDING / 2 + 4, 0));
		top.setOpaque(false);
		top.add(logo, BorderLayout.WEST);
		top.add(text, BorderLayout.CENTER);

		JPanel body = new JPanel(new BorderLayout(0, PADDING / 2));
		body.setOpaque(false);
		body.setBorder(BorderFactory.createEmptyBorder(PADDING + ACCENT_HEIGHT, PADDING, PADDING, PADDING));
		body.add(top, BorderLayout.CENTER);
		body.add(progress, BorderLayout.SOUTH);

		JPanel panel = new JPanel(new BorderLayout()) {
			@Override
			protected void paintComponent(Graphics graphics) {
				Graphics2D g = (Graphics2D) graphics.create();
				g.setColor(BACKGROUND);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setPaint(new GradientPaint(0, 0, BRAND_START, getWidth(), 0, BRAND_END));
				g.fillRect(0, 0, getWidth(), ACCENT_HEIGHT);
				g.dispose();
			}

			@Override
			protected void paintBorder(Graphics graphics) {
				graphics.setColor(BORDER);
				graphics.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
			}
		};
		panel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		panel.add(body, BorderLayout.CENTER);
		return panel;
	}

	private static JLabel label(String text, int style, int size, Color color) {
		JLabel label = new JLabel(text);
		label.setFont(new Font(Font.SANS_SERIF, style, size));
		label.setForeground(color);
		label.setAlignmentX(0f);
		return label;
	}

	static final class ProgressStrip extends JComponent {

		private static final int HEIGHT_PX = 6;
		private static final int TICK_MS = 16;
		private static final double SEGMENT = 0.32;
		private static final double STEP = 0.012;

		private final Timer timer = new Timer(TICK_MS, event -> advance());
		private double position;

		ProgressStrip() {
			setPreferredSize(new Dimension(0, HEIGHT_PX));
			setOpaque(false);
		}

		void start() {
			timer.start();
		}

		void stop() {
			timer.stop();
		}

		private void advance() {
			position += STEP;
			if (position > 1 + SEGMENT) {
				position = 0;
			}
			repaint();
		}

		void freezeAt(double value) {
			position = value;
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int width = getWidth();
			int height = getHeight();
			g.setColor(BORDER);
			g.fill(new RoundRectangle2D.Double(0, 0, width, height, height, height));
			g.setClip(new RoundRectangle2D.Double(0, 0, width, height, height, height));
			int segment = (int) (width * SEGMENT);
			int x = (int) ((position - SEGMENT) * width);
			g.setPaint(new GradientPaint(x, 0, BRAND_START, x + segment, 0, BRAND_END));
			g.fillRect(x, 0, segment, height);
			g.dispose();
		}
	}
}
