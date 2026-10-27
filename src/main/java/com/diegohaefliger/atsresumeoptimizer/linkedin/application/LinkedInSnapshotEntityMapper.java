package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshotSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface LinkedInSnapshotEntityMapper {

	@Mapping(target = "profile", source = "content")
	LinkedInSnapshot toDomain(LinkedInSnapshotEntity entity);

	@Mapping(target = "fullName", source = "content.fullName")
	@Mapping(target = "headline", source = "content.headline")
	LinkedInSnapshotSummary toSummary(LinkedInSnapshotHead head);
}
