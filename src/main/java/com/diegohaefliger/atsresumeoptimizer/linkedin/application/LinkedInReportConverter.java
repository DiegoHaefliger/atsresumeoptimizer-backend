package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisReport;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.json.JsonMapper;

@Converter
class LinkedInReportConverter implements AttributeConverter<LinkedInAnalysisReport, String> {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Override
	public String convertToDatabaseColumn(LinkedInAnalysisReport report) {
		return report == null ? null : JSON.writeValueAsString(report);
	}

	@Override
	public LinkedInAnalysisReport convertToEntityAttribute(String json) {
		return json == null ? null : JSON.readValue(json, LinkedInAnalysisReport.class);
	}
}
