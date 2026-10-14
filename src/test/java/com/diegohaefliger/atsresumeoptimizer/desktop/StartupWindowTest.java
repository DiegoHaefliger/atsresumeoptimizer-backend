package com.diegohaefliger.atsresumeoptimizer.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;

class StartupWindowTest {

	@Test
	void showsTheAppNameAndTheFirstRunHint() {
		List<String> texts = new ArrayList<>();
		collectLabelTexts(StartupWindow.content(new StartupWindow.ProgressStrip()), texts);

		assertThat(texts).contains(DesktopBrand.NAME).anyMatch(text -> text.contains("primeira vez"));
	}

	private static void collectLabelTexts(Container container, List<String> texts) {
		for (Component child : container.getComponents()) {
			if (child instanceof JLabel label && label.getText() != null) {
				texts.add(label.getText());
			}
			if (child instanceof Container nested) {
				collectLabelTexts(nested, texts);
			}
		}
	}
}
