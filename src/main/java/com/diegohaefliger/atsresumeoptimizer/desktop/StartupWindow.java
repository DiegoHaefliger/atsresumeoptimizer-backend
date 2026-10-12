package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

final class StartupWindow {

	private static final int WIDTH = 380;
	private static final int HEIGHT = 110;
	private static final int PADDING = 20;

	private JWindow window;

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
			if (window != null) {
				window.dispose();
				window = null;
			}
		});
	}

	private void open() {
		JProgressBar progress = new JProgressBar();
		progress.setIndeterminate(true);
		JPanel panel = new JPanel(new BorderLayout(0, PADDING / 2));
		panel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
		panel.add(new JLabel(DesktopBrand.NAME + " está abrindo. Na primeira vez pode levar um minuto."), BorderLayout.CENTER);
		panel.add(progress, BorderLayout.SOUTH);
		window = new JWindow();
		window.setContentPane(panel);
		window.setSize(WIDTH, HEIGHT);
		window.setIconImage(DesktopBrand.icon());
		window.setLocationRelativeTo(null);
		window.setAlwaysOnTop(true);
		window.setVisible(true);
	}
}
