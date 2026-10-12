package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DesktopTray {

	private static final Logger LOGGER = LoggerFactory.getLogger(DesktopTray.class);

	private DesktopTray() {
	}

	static void install(URI url, Runnable exit) {
		if (!SystemTray.isSupported()) {
			LOGGER.info("Sem bandeja do sistema; o app segue rodando e abre em {}", url);
			return;
		}
		PopupMenu menu = new PopupMenu();
		MenuItem open = new MenuItem("Abrir " + DesktopBrand.NAME);
		open.addActionListener(event -> Browser.open(url));
		MenuItem quit = new MenuItem("Sair");
		quit.addActionListener(event -> new Thread(exit, "desktop-exit").start());
		menu.add(open);
		menu.addSeparator();
		menu.add(quit);
		TrayIcon icon = new TrayIcon(DesktopBrand.icon(), DesktopBrand.NAME, menu);
		icon.setImageAutoSize(true);
		icon.addActionListener(event -> Browser.open(url));
		try {
			SystemTray.getSystemTray().add(icon);
		} catch (AWTException exception) {
			LOGGER.warn("Não deu pra pôr o ícone na bandeja", exception);
		}
	}
}
