package com.diegohaefliger.atsresumeoptimizer.parsing.domain;

import java.util.List;

public record ParsingResult(
		NormalizedDocument document,
		ParsingSignals signals,
		List<Section> sections,
		ContactInfo contact) {
}
