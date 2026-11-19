package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInProfileDraft;
import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInProfileEntryDraft;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInEntry;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import java.util.regex.Pattern;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
interface LinkedInProfileDraftMapper {

	Pattern TRAILING_DURATION = Pattern.compile("\\s*\\([^()]*\\d[^()]*\\)\\s*$");

	@Mapping(target = "connections", ignore = true)
	@Mapping(target = "followers", ignore = true)
	@Mapping(target = "recommendations", ignore = true)
	@Mapping(target = "featured", ignore = true)
	@Mapping(target = "recentActivity", ignore = true)
	LinkedInProfile toProfile(LinkedInProfileDraft draft, String profileUrl, String customUrl);

	@Mapping(target = "period", source = "period", qualifiedByName = "withoutDuration")
	LinkedInEntry toEntry(LinkedInProfileEntryDraft draft);

	@Named("withoutDuration")
	default String withoutDuration(String period) {
		return period == null ? null : TRAILING_DURATION.matcher(period).replaceFirst("");
	}
}
