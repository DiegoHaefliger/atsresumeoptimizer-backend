package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record ResumeEntry(
		String heading,
		String period,
		String subheading,
		String context,
		List<List<TextSpan>> bullets,
		String technologies,
		List<List<TextSpan>> results) {

	public ResumeEntry {
		bullets = bullets == null ? List.of() : List.copyOf(bullets);
		results = results == null ? List.of() : List.copyOf(results);
	}

	public ResumeEntry(
			String heading, String period, String subheading, String context, List<List<TextSpan>> bullets,
			String technologies) {
		this(heading, period, subheading, context, bullets, technologies, List.of());
	}

	public ResumeEntry withBullets(List<List<TextSpan>> newBullets) {
		return new ResumeEntry(heading, period, subheading, context, newBullets, technologies, results);
	}

	public ResumeEntry withBulletsAndTechnologies(List<List<TextSpan>> newBullets, String newTechnologies) {
		return new ResumeEntry(heading, period, subheading, context, newBullets, newTechnologies, results);
	}
}
