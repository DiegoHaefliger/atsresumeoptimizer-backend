package com.diegohaefliger.atsresumeoptimizer.release.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.release.domain.AppVersion;
import com.diegohaefliger.atsresumeoptimizer.release.domain.UpdateStatus;
import org.mapstruct.Mapper;

@Mapper
interface AppVersionWebMapper {

	AppVersionResponse toResponse(UpdateStatus status);

	default String toText(AppVersion version) {
		return version == null ? null : version.toString();
	}
}
