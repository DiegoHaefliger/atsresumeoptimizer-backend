package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.json.JsonMapper;

@Converter
class LinkedInProfileConverter implements AttributeConverter<LinkedInProfile, String> {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Override
	public String convertToDatabaseColumn(LinkedInProfile profile) {
		return profile == null ? null : JSON.writeValueAsString(profile);
	}

	@Override
	public LinkedInProfile convertToEntityAttribute(String json) {
		return json == null ? null : JSON.readValue(json, LinkedInProfile.class);
	}
}
